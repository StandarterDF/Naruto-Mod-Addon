package com.sekwah.narutoaddon.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.sekwah.narutoaddon.client.ClientSensingData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Draws what the chakra sense is feeling out, in the world and through walls.
 *
 * <p>The ordinary sense is staged: a creature that has only just begun to appear is a thin cloud of
 * dim motes and, once it resolves, its own body is drawn in place. The Byakugan is not staged - it
 * is a way of seeing, not a roll against mastery - so it takes a separate path in here.</p>
 *
 * <p>The eye deliberately does not draw the creature's texture through the wall. A full coloured
 * mob seen through stone is the cheapest kind of ESP: it says "there is a zombie somewhere" but not
 * "it is in the next room rather than in this one", because every body is equally sharp and equally
 * lit. Instead the eye shows chakra: a flat, additive silhouette of the model, dimmed with distance,
 * with a handful of bright tenketsu nodes on it. Bodies in direct line of sight are left to the
 * ordinary level pass and only get a soft aura and the same nodes, so the eye never doubles a body
 * that is already on screen.</p>
 *
 * <p>This draws at {@code AFTER_LEVEL}, the very end of the level pass: it is the only point where
 * the ordinary sense can wipe the depth buffer for its bodies without breaking the translucent or
 * weather passes that still use it. The eye never wipes it. The event pose stack there is the
 * projection rather than the view, so the view is captured earlier from {@code AFTER_PARTICLES}
 * (which is handed the level pose stack) and reused; with that view and the modelview pinned to
 * identity vertices are placed at {@code world - camera}, exactly like the vanilla world pass
 * does.</p>
 */
public final class SensingEspRenderer {

    private SensingEspRenderer() {
    }

    /** A ceiling on how many creatures are drawn as full bodies at once, nearest first. */
    private static final int MAX_MODELS = 24;

    /** A ceiling on how many creatures are drawn as particle shells at once, nearest first. */
    private static final int MAX_SHELLS = 24;

    /** A ceiling on the motes one shell may spend, even for a creature with a lot of model parts. */
    private static final int MAX_MOTES_PER_ENTITY = 120;

    /** A ceiling on how many chakra silhouettes the eye draws at once, nearest first. */
    private static final int MAX_SILHOUETTES = 16;

    /** How many tenketsu nodes a body shows. */
    private static final int MAX_TENKETSU = 8;

    /** How many captured corners the node picker looks at before spreading the nodes. */
    private static final int TENKETSU_SAMPLE = 256;

    /** How many floats one captured model may spend; a hard stop for a renderer gone wild. */
    private static final int MAX_CAPTURE_FLOATS = 60_000;

    /** For how many ticks a line-of-sight answer is trusted. Walls move slowly. */
    private static final int SIGHT_TICKS = 4;

    /**
     * The camera's view rotation, lifted from the pose stack of the world pass. The stage that hands
     * out the view is not the one where the depth buffer can be cleared, and the view does not change
     * between them in a frame, so it is captured early and used at the end.
     */
    private static final Matrix4f VIEW = new Matrix4f();
    private static boolean hasView;

    /** The last line-of-sight answer per entity, so a wall check is not run every frame. */
    private static final Map<Integer, SightSample> SIGHT = new HashMap<>();

    /** The view captured this frame, for the other after-level renderers (the ore sense). */
    public static Matrix4f viewMatrix() {
        return VIEW;
    }

    public static boolean hasView() {
        return hasView;
    }

    public static void renderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            // AFTER_PARTICLES is dispatched with the level pose stack, which at that point is the
            // camera view rotation and nothing else. AFTER_LEVEL, by contrast, gets the projection.
            VIEW.set(event.getPoseStack().last().pose());
            hasView = true;
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !hasView) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) {
            return;
        }
        float fade = ClientSensingData.getFade();
        if (fade <= 0f) {
            return;
        }
        Map<LivingEntity, Float> sensed = ClientSensingData.sensed();
        if (sensed.isEmpty()) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        float partial = event.getPartialTick();
        PoseStack pose = new PoseStack();
        pose.mulPoseMatrix(VIEW);

        List<Map.Entry<LivingEntity, Float>> entries = new ArrayList<>(sensed.entrySet());
        // Nearest first: if there are more creatures than the model budget allows, the ones the
        // caster is most likely to care about are the ones that get a body.
        entries.sort(Comparator.comparingDouble(entry -> entry.getKey().distanceToSqr(cam)));

        // With the modelview pinned to identity the pose stack carries the whole transform itself:
        // vertex = view * (world - camera), then projection.
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();
        try {
            if (ClientSensingData.isByakugan()) {
                drawEye(mc, pose, camera, cam, partial, fade, entries);
            } else {
                drawShells(mc, pose, camera, cam, partial, fade, entries);
                drawBodies(mc, pose, cam, partial, fade, entries);
            }
        } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
        }
    }

    // ---------------------------------------------------------------------------------------
    // The Byakugan: chakra silhouettes, tenketsu nodes and depth cueing.
    // ---------------------------------------------------------------------------------------

    /**
     * Draws every body inside the eye's bubble as chakra rather than as a picture of the creature.
     *
     * <p>Alpha follows distance so the picture carries depth: a creature at the edge of the bubble
     * is a ghost, one just behind the wall is bright. Bodies in direct sight are not drawn at all -
     * the level pass already has them - but get an aura and the nodes, so the caster can tell at a
     * glance who is standing in front of them and who is only felt through the stone.</p>
     */
    private static void drawEye(Minecraft mc, PoseStack pose, Camera camera, Vec3 cam, float partial,
                                float fade, List<Map.Entry<LivingEntity, Float>> entries) {
        Matrix4f matrix = pose.last().pose();
        Vector3f left = camera.getLeftVector();
        Vector3f upVector = camera.getUpVector();
        Vec3 right = new Vec3(-left.x(), -left.y(), -left.z());
        Vec3 up = new Vec3(upVector.x(), upVector.y(), upVector.z());
        long time = mc.level.getGameTime();
        float maxDistance = Math.max(1f, ClientSensingData.getRadius());

        List<Body> bodies = new ArrayList<>();
        for (Map.Entry<LivingEntity, Float> entry : entries) {
            LivingEntity entity = entry.getKey();
            if (entity.isRemoved() || entity == mc.player) {
                continue;
            }
            if (bodies.size() >= MAX_SILHOUETTES) {
                break;
            }
            Vec3 position = entity.getPosition(partial);
            Vec3 centre = position.add(0d, entity.getBbHeight() * 0.55d, 0d).subtract(cam);
            double distance = centre.length();
            if (distance < 0.01d) {
                continue;
            }
            // The brief's depth cue: bright behind the next wall, a ghost near the rim of the bubble.
            float alpha = Mth.clamp(1f - (float) distance / maxDistance, 0.15f, 0.85f) * fade;
            float[] rgb = chakraColour(entity);
            // The body is captured even when it is in plain sight: the tenketsu sit on the model's
            // own corners, so the nodes follow the pose instead of hanging on a guessed skeleton.
            ShapeCapture shape = captureShape(mc, entity, partial, cam);
            boolean visible = hasLineOfSight(mc, entity, cam);
            AABB box = shape == null ? interpolatedBox(entity, partial).move(-cam.x, -cam.y, -cam.z) : null;
            float pulse = entity instanceof Player || entity.getMaxHealth() >= 100f
                    ? 0.8f + 0.2f * Mth.sin(time * 0.2f) : 1f;
            bodies.add(new Body(entity, rgb, alpha, pulse, visible, shape, box, centre,
                    tenketsuOf(shape, box), distance));
        }
        if (bodies.isEmpty()) {
            return;
        }

        drawAuras(bodies, matrix, right, up, time);
        drawSilhouettes(bodies, matrix, right, up, time);
    }

    /** Soft additive halos: an aura on bodies in sight, a halo around every tenketsu node. */
    private static void drawAuras(List<Body> bodies, Matrix4f matrix, Vec3 right, Vec3 up, long time) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        boolean any = false;
        for (Body body : bodies) {
            float pulse = body.pulse * (0.9f + 0.1f * Mth.sin(time * 0.12f + body.entity.getId()));
            if (body.visible) {
                double radius = Math.max(body.entity.getBbWidth(), 0.5d) * 1.5d + 0.25d;
                emitGlow(builder, matrix, body.centre, right, up, radius, body.rgb, body.alpha * 0.10f * pulse);
                any = true;
            }
            double nodeSize = 0.08d + body.distance * 0.005d;
            for (Vec3 node : body.nodes) {
                emitGlow(builder, matrix, node, right, up, nodeSize, body.rgb, body.alpha * 0.16f * pulse);
                any = true;
            }
        }
        if (!any) {
            builder.end();
            return;
        }
        beginAdditive();
        BufferUploader.drawWithShader(builder.end());
        endAdditive();
    }

    /** The chakra body itself: flat additive model quads, plus the bright core of each node. */
    private static void drawSilhouettes(List<Body> bodies, Matrix4f matrix, Vec3 right, Vec3 up, long time) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        boolean any = false;
        for (Body body : bodies) {
            if (!body.visible) {
                if (body.shape != null) {
                    any |= emitShape(builder, matrix, body.shape, body.rgb, body.alpha * 0.34f, time,
                            (float) body.entity.getY());
                } else if (body.box != null) {
                    any |= emitBox(builder, matrix, body.box, body.rgb, body.alpha * 0.14f);
                }
            }
            double core = 0.028d + body.distance * 0.0018d;
            for (int i = 0; i < body.nodes.size(); i++) {
                Vec3 node = body.nodes.get(i);
                float flicker = 0.75f + 0.25f * Mth.sin(time * 0.25f + i * 1.7f + body.entity.getId());
                // Only a quarter of the way to white: a node should read as its owner's chakra
                // colour, not as eight headlights burning the picture out.
                float r = body.rgb[0] * 0.75f + 0.25f;
                float g = body.rgb[1] * 0.75f + 0.25f;
                float b = body.rgb[2] * 0.75f + 0.25f;
                emitBillboard(builder, matrix, node, right, up, core,
                        r, g, b, body.alpha * 0.55f * flicker * body.pulse);
                any = true;
            }
        }
        if (!any) {
            builder.end();
            return;
        }
        beginAdditive();
        BufferUploader.drawWithShader(builder.end());
        endAdditive();
    }

    /**
     * Emits a captured model as additive flat quads. Texture, light and normal were dropped by the
     * capture, so what is left is the shape of the chakra and nothing of the creature's skin.
     */
    private static boolean emitShape(BufferBuilder builder, Matrix4f matrix, ShapeCapture shape,
                                     float[] rgb, float alpha, long time, float originY) {
        float[] data = shape.data();
        int quads = shape.size() / 4;
        for (int quad = 0; quad < quads; quad++) {
            for (int corner = 0; corner < 4; corner++) {
                int i = (quad * 4 + corner) * 3;
                float x = data[i];
                float y = data[i + 1];
                float z = data[i + 2];
                // A slow shimmer travelling up the body: the chakra is flowing, not a decal. The
                // phase is taken from the world height, so the band stays on the body when the
                // camera moves instead of sliding through it.
                float shimmer = 0.78f + 0.22f * Mth.sin(time * 0.12f + (originY + y) * 1.8f);
                builder.vertex(matrix, x, y, z)
                        .color(rgb[0] * shimmer, rgb[1] * shimmer, rgb[2] * shimmer, alpha)
                        .endVertex();
            }
        }
        return quads > 0;
    }

    /** A plain box, used only when a creature's renderer refused to be captured outside its pass. */
    private static boolean emitBox(BufferBuilder builder, Matrix4f matrix, AABB box,
                                   float[] rgb, float alpha) {
        float x0 = (float) box.minX, y0 = (float) box.minY, z0 = (float) box.minZ;
        float x1 = (float) box.maxX, y1 = (float) box.maxY, z1 = (float) box.maxZ;
        quad(builder, matrix, rgb, alpha, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(builder, matrix, rgb, alpha, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
        quad(builder, matrix, rgb, alpha, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(builder, matrix, rgb, alpha, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        quad(builder, matrix, rgb, alpha, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        quad(builder, matrix, rgb, alpha, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        return true;
    }

    private static void quad(BufferBuilder builder, Matrix4f matrix, float[] rgb, float alpha,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        builder.vertex(matrix, ax, ay, az).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
        builder.vertex(matrix, bx, by, bz).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
        builder.vertex(matrix, cx, cy, cz).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
        builder.vertex(matrix, dx, dy, dz).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
    }

    /**
     * A radial glow built from triangles: one bright centre and a transparent rim. Drawn additively,
     * so overlapping halos simply add up instead of banding.
     */
    private static void emitGlow(BufferBuilder builder, Matrix4f matrix, Vec3 centre, Vec3 right, Vec3 up,
                                 double radius, float[] rgb, float alpha) {
        final int segments = 12;
        double cx = centre.x;
        double cy = centre.y;
        double cz = centre.z;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2d * i / segments;
            double a1 = Math.PI * 2d * (i + 1) / segments;
            builder.vertex(matrix, (float) cx, (float) cy, (float) cz)
                    .color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
            double x0 = cx + (right.x * Math.cos(a0) + up.x * Math.sin(a0)) * radius;
            double y0 = cy + (right.y * Math.cos(a0) + up.y * Math.sin(a0)) * radius;
            double z0 = cz + (right.z * Math.cos(a0) + up.z * Math.sin(a0)) * radius;
            builder.vertex(matrix, (float) x0, (float) y0, (float) z0)
                    .color(rgb[0], rgb[1], rgb[2], 0f).endVertex();
            double x1 = cx + (right.x * Math.cos(a1) + up.x * Math.sin(a1)) * radius;
            double y1 = cy + (right.y * Math.cos(a1) + up.y * Math.sin(a1)) * radius;
            double z1 = cz + (right.z * Math.cos(a1) + up.z * Math.sin(a1)) * radius;
            builder.vertex(matrix, (float) x1, (float) y1, (float) z1)
                    .color(rgb[0], rgb[1], rgb[2], 0f).endVertex();
        }
    }

    /** One camera-facing quad, used for the hard bright core of a tenketsu node. */
    private static void emitBillboard(BufferBuilder builder, Matrix4f matrix, Vec3 centre, Vec3 right, Vec3 up,
                                      double size, float r, float g, float b, float a) {
        double cx = centre.x;
        double cy = centre.y;
        double cz = centre.z;
        builder.vertex(matrix, (float) (cx - (right.x + up.x) * size), (float) (cy - (right.y + up.y) * size),
                (float) (cz - (right.z + up.z) * size)).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) (cx + (right.x - up.x) * size), (float) (cy + (right.y - up.y) * size),
                (float) (cz + (right.z - up.z) * size)).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) (cx + (right.x + up.x) * size), (float) (cy + (right.y + up.y) * size),
                (float) (cz + (right.z + up.z) * size)).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) (cx - (right.x - up.x) * size), (float) (cy - (right.y - up.y) * size),
                (float) (cz - (right.z - up.z) * size)).color(r, g, b, a).endVertex();
    }

    /**
     * The tenketsu of a body, taken from the model's own captured corners rather than from a guessed
     * humanoid skeleton. The chakra sense already works this way: it runs the creature's renderer
     * into a sink and uses the positions the model actually emitted, so the points bend with the pose
     * instead of hanging in the air. The corners are spread with farthest-point sampling, which keeps
     * the nodes on the extremities of the silhouette - hands, feet, head - and leaves the shape
     * readable at a glance.
     */
    private static List<Vec3> tenketsuOf(ShapeCapture shape, AABB box) {
        if (shape != null) {
            List<Vec3> points = shape.points();
            if (points.size() >= MAX_TENKETSU) {
                return farthestPoints(points, MAX_TENKETSU);
            }
            if (!points.isEmpty()) {
                return points;
            }
        }
        return box == null ? List.of() : boxPoints(box);
    }

    /**
     * Picks {@code count} points that are as far apart from each other as possible. The first node is
     * the top of the model - the head is the one point a viewer looks for first - and every next one
     * is the corner with the largest distance to everything already chosen, which spreads the rest
     * over hands, feet and the corners of the body.
     */
    private static List<Vec3> farthestPoints(List<Vec3> points, int count) {
        List<Vec3> chosen = new ArrayList<>(count);
        // The head is picked from the full list, not the thinned pool, so the top of the model is
        // never skipped by the stride.
        Vec3 first = points.get(0);
        for (Vec3 point : points) {
            if (point.y > first.y) {
                first = point;
            }
        }
        chosen.add(first);

        int stride = Math.max(1, points.size() / TENKETSU_SAMPLE);
        List<Vec3> pool = new ArrayList<>(Math.min(points.size(), TENKETSU_SAMPLE) + 1);
        for (int i = 0; i < points.size(); i += stride) {
            pool.add(points.get(i));
        }
        if (pool.isEmpty()) {
            return chosen;
        }

        while (chosen.size() < count) {
            Vec3 next = null;
            double nextBest = -1.0d;
            for (Vec3 point : pool) {
                double nearest = Double.MAX_VALUE;
                for (Vec3 selected : chosen) {
                    nearest = Math.min(nearest, point.distanceToSqr(selected));
                }
                if (nearest > nextBest) {
                    nextBest = nearest;
                    next = point;
                }
            }
            // Once the remaining corners coincide there is nothing left worth marking.
            if (next == null || nextBest < 1.0e-4d) {
                break;
            }
            chosen.add(next);
        }
        return chosen;
    }

    /** The fallback nodes for a renderer that refused to be captured: the box's own corners. */
    private static List<Vec3> boxPoints(AABB box) {
        List<Vec3> points = new ArrayList<>(8);
        for (int i = 0; i < 8; i++) {
            points.add(new Vec3(
                    (i & 1) == 0 ? box.minX : box.maxX,
                    (i & 2) == 0 ? box.minY : box.maxY,
                    (i & 4) == 0 ? box.minZ : box.maxZ));
        }
        return points;
    }

    /** Chakra colours by kind: hostile, animal, and the deep blue-white of people and bosses. */
    private static float[] chakraColour(LivingEntity entity) {
        if (entity instanceof Player || entity.getMaxHealth() >= 100f) {
            // Blue rather than white-blue: the additive blend pushes anything bright towards white
            // and the picture loses every colour the eye is supposed to be distinguishing.
            return new float[]{0.42f, 0.70f, 1.0f};
        }
        if (entity instanceof Enemy) {
            return new float[]{0.95f, 0.42f, 0.62f};
        }
        return new float[]{0.45f, 0.95f, 0.80f};
    }

    /**
     * @return whether the creature can be seen from the camera without a block in the way. The
     *         answer is cached for a few ticks: the ray is cheap, but it is not free and a wall does
     *         not move between frames.
     */
    private static boolean hasLineOfSight(Minecraft mc, LivingEntity entity, Vec3 cam) {
        long now = mc.level.getGameTime();
        SightSample cached = SIGHT.get(entity.getId());
        if (cached != null && now - cached.tick < SIGHT_TICKS) {
            return cached.visible;
        }
        Vec3 target = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.65d, entity.getZ());
        boolean visible = mc.level.clip(new ClipContext(cam, target, ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE, mc.player)).getType() == HitResult.Type.MISS;
        if (SIGHT.size() > 256) {
            SIGHT.clear();
        }
        SIGHT.put(entity.getId(), new SightSample(visible, now));
        return visible;
    }

    private static void beginAdditive() {
        RenderSystem.enableBlend();
        // Additive: the chakra is light, not paint, so it adds to whatever is behind it.
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    private static void endAdditive() {
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** One creature prepared for the eye pass: colour, depth fade, silhouette and nodes. */
    private static final class Body {

        final LivingEntity entity;
        final float[] rgb;
        final float alpha;
        final float pulse;
        final boolean visible;
        final ShapeCapture shape;
        final AABB box;
        final Vec3 centre;
        final List<Vec3> nodes;
        final double distance;

        Body(LivingEntity entity, float[] rgb, float alpha, float pulse, boolean visible,
             ShapeCapture shape, AABB box, Vec3 centre, List<Vec3> nodes, double distance) {
            this.entity = entity;
            this.rgb = rgb;
            this.alpha = alpha;
            this.pulse = pulse;
            this.visible = visible;
            this.shape = shape;
            this.box = box;
            this.centre = centre;
            this.nodes = nodes;
            this.distance = distance;
        }
    }

    /** A cached line-of-sight answer, valid for {@link #SIGHT_TICKS} ticks. */
    private static final class SightSample {

        final boolean visible;
        final long tick;

        SightSample(boolean visible, long tick) {
            this.visible = visible;
            this.tick = tick;
        }
    }

    // ---------------------------------------------------------------------------------------
    // The ordinary chakra sense: staged motes and, at full resolve, the creature's own body.
    // ---------------------------------------------------------------------------------------

    /**
     * Draws the dim motes that stand in for a creature that has not resolved all the way yet.
     *
     * <p>The motes are placed on the creature's own model, not on its bounding box: the renderer is
     * run once into a vertex sink and the positions it would have drawn are collected, so the shell
     * traces the actual shape - head, legs, held sword and all. A beginner sees only a share of them
     * and a master sees nearly all, which is what makes one creature recognisable before another.</p>
     *
     * <p>While the sense is dissolving even the fully resolved bodies fall back to motes, so the
     * whole picture comes apart as specks of chakra rather than a body blinking out.</p>
     */
    private static void drawShells(Minecraft mc, PoseStack pose, Camera camera, Vec3 cam, float partial,
                                   float fade, List<Map.Entry<LivingEntity, Float>> entries) {
        boolean dissolving = fade < 1f;
        Matrix4f matrix = pose.last().pose();
        Vector3f left = camera.getLeftVector();
        Vector3f upVector = camera.getUpVector();
        Vec3 right = new Vec3(-left.x(), -left.y(), -left.z());
        Vec3 up = new Vec3(upVector.x(), upVector.y(), upVector.z());
        long time = mc.level.getGameTime();

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        boolean any = false;
        int shells = 0;
        for (Map.Entry<LivingEntity, Float> entry : entries) {
            LivingEntity entity = entry.getKey();
            float intensity = entry.getValue();
            if (entity.isRemoved() || (intensity >= 1f && !dissolving)) {
                continue;
            }
            if (shells++ >= MAX_SHELLS) {
                break;
            }
            // Even the faintest first reading keeps a handful of motes on the creature, so a new
            // sense never looks broken; past that the share of the model drawn follows the intensity.
            float density = dissolving ? 1f : Math.max(intensity, 0.15f);
            float alpha = fade * (0.3f + 0.7f * density);
            float[] rgb = colourOf(entity);
            double distance = Math.sqrt(entity.distanceToSqr(cam));
            // Motes grow with distance so a creature a few chunks away still reads as a speck
            // instead of a single pixel.
            double size = 0.02d + distance * 0.004d;
            RandomSource random = RandomSource.create(entity.getId() * 341873128712L + time / 4L);

            List<Vec3> shape = shapePoints(mc, entity, partial, cam);
            if (shape.isEmpty()) {
                // Nothing could be captured - an invisible creature, or a renderer that refuses to
                // run outside its usual pass. Fall back to the old box outline rather than nothing.
                AABB box = interpolatedBox(entity, partial);
                int count = 6 + (int) (density * 30f);
                for (int i = 0; i < count; i++) {
                    Vec3 point = surfacePoint(box, random);
                    emitMote(builder, matrix, point.x - cam.x, point.y - cam.y, point.z - cam.z,
                            right, up, size, rgb[0], rgb[1], rgb[2], alpha);
                    any = true;
                }
                continue;
            }

            // The points already sit on the model's surface, deduplicated down to its corners. Only
            // a share is drawn at low intensity; as the sense resolves the share climbs to all of
            // them and the full body takes over from the shell.
            int stride = Math.max(1, shape.size() / MAX_MOTES_PER_ENTITY);
            for (int i = random.nextInt(stride); i < shape.size(); i += stride) {
                if (random.nextFloat() > density) {
                    continue;
                }
                Vec3 point = shape.get(i);
                emitMote(builder, matrix, point.x, point.y, point.z,
                        right, up, size, rgb[0], rgb[1], rgb[2], alpha);
                any = true;
            }
        }
        if (!any) {
            builder.end();
            return;
        }
        // Drawn after every depth-tested thing in the level with the depth test off, so the shell is
        // seen through whatever stands between the caster and the creature.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /** Draws fully resolved creatures as their own bodies, in place, over the terrain. */
    private static void drawBodies(Minecraft mc, PoseStack pose, Vec3 cam, float partial, float fade,
                                   List<Map.Entry<LivingEntity, Float>> entries) {
        if (fade < 1f) {
            return;
        }
        List<LivingEntity> bodies = new ArrayList<>();
        for (Map.Entry<LivingEntity, Float> entry : entries) {
            if (entry.getValue() >= 1f && !entry.getKey().isRemoved()) {
                bodies.add(entry.getKey());
                if (bodies.size() >= MAX_MODELS) {
                    break;
                }
            }
        }
        if (bodies.isEmpty()) {
            return;
        }
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        // The light layer is normally only bound while the vanilla pass needs it, so it is brought
        // back for the bodies; they are lit full-bright anyway, but the sampler still has to point
        // at the lightmap or the entity shader reads whatever texture happened to be bound last.
        LightTexture light = mc.gameRenderer.lightTexture();
        light.turnOnLightLayer();
        try {
            // The bodies are meant to be seen through the terrain, so the depth buffer is wiped
            // clean first: without that a wall in front of a creature would hide it and full ESP
            // would be no more than the outline. The Byakugan never comes through here - it draws
            // chakra silhouettes instead of textures.
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.enableDepthTest();
            for (LivingEntity entity : bodies) {
                Vec3 p = entity.getPosition(partial);
                float yaw = Mth.rotLerp(partial, entity.yRotO, entity.getYRot());
                mc.getEntityRenderDispatcher().render(entity,
                        p.x - cam.x, p.y - cam.y, p.z - cam.z,
                        yaw, partial, pose, buffer, LightTexture.FULL_BRIGHT);
            }
            buffer.endBatch();
        } finally {
            light.turnOffLightLayer();
        }
    }

    /**
     * Runs the creature's own renderer into a vertex sink and collects the camera-relative
     * positions it would have drawn. This is the only way to get the real model shape without
     * reimplementing every renderer: the sink sees exactly what the model emits, after all of the
     * entity, pose and animation transforms.
     */
    @SuppressWarnings("unchecked")
    private static List<Vec3> shapePoints(Minecraft mc, LivingEntity entity, float partial, Vec3 cam) {
        EntityRenderer<LivingEntity> renderer =
                (EntityRenderer<LivingEntity>) mc.getEntityRenderDispatcher().getRenderer(entity);
        PointCapture capture = new PointCapture();
        PoseStack pose = new PoseStack();
        Vec3 position = entity.getPosition(partial);
        // A fresh identity pose, translated only to the entity's camera-relative place: the recorded
        // points are then camera-relative world positions, ready for the view matrix above.
        pose.translate(position.x - cam.x, position.y - cam.y, position.z - cam.z);
        float yaw = Mth.rotLerp(partial, entity.yRotO, entity.getYRot());
        try {
            renderer.render(entity, yaw, partial, pose, capture.source(), LightTexture.FULL_BRIGHT);
        } catch (RuntimeException ignored) {
            // A third-party renderer is not required to like being invoked outside the usual entity
            // pass; the shell simply falls back to the box for that creature.
            return List.of();
        }
        return capture.points();
    }

    /**
     * Runs the creature's renderer into a raw vertex sink, keeping the model's quads in order.
     *
     * <p>Unlike {@link PointCapture} this keeps duplicates: the quads are meant to be filled, not
     * sampled, and dropping repeated corners would break the faces apart. Texture, colour and light
     * are ignored on purpose - the eye draws chakra, not the creature's skin.</p>
     */
    @SuppressWarnings("unchecked")
    private static ShapeCapture captureShape(Minecraft mc, LivingEntity entity, float partial, Vec3 cam) {
        EntityRenderer<LivingEntity> renderer =
                (EntityRenderer<LivingEntity>) mc.getEntityRenderDispatcher().getRenderer(entity);
        ShapeCapture capture = new ShapeCapture();
        PoseStack pose = new PoseStack();
        Vec3 position = entity.getPosition(partial);
        pose.translate(position.x - cam.x, position.y - cam.y, position.z - cam.z);
        float yaw = Mth.rotLerp(partial, entity.yRotO, entity.getYRot());
        try {
            renderer.render(entity, yaw, partial, pose, capture.source(), LightTexture.FULL_BRIGHT);
        } catch (RuntimeException ignored) {
            return null;
        }
        return capture.size() >= 4 ? capture : null;
    }

    private static AABB interpolatedBox(LivingEntity entity, float partial) {
        Vec3 p = entity.getPosition(partial);
        return entity.getBoundingBox().move(p.x - entity.getX(), p.y - entity.getY(), p.z - entity.getZ());
    }

    /** A random point on one of the six faces of the box, so the motes trace an outline, not a fog. */
    private static Vec3 surfacePoint(AABB box, RandomSource random) {
        double u = random.nextDouble();
        double v = random.nextDouble();
        return switch (random.nextInt(6)) {
            case 0 -> new Vec3(box.minX, Mth.lerp(u, box.minY, box.maxY), Mth.lerp(v, box.minZ, box.maxZ));
            case 1 -> new Vec3(box.maxX, Mth.lerp(u, box.minY, box.maxY), Mth.lerp(v, box.minZ, box.maxZ));
            case 2 -> new Vec3(Mth.lerp(u, box.minX, box.maxX), box.minY, Mth.lerp(v, box.minZ, box.maxZ));
            case 3 -> new Vec3(Mth.lerp(u, box.minX, box.maxX), box.maxY, Mth.lerp(v, box.minZ, box.maxZ));
            case 4 -> new Vec3(Mth.lerp(u, box.minX, box.maxX), Mth.lerp(v, box.minY, box.maxY), box.minZ);
            default -> new Vec3(Mth.lerp(u, box.minX, box.maxX), Mth.lerp(v, box.minY, box.maxY), box.maxZ);
        };
    }

    /** Green for ordinary creatures, red for hostile ones, aqua for other people. */
    private static float[] colourOf(LivingEntity entity) {
        Player self = Minecraft.getInstance().player;
        if (self != null && entity != self && entity.isAlliedTo(self)) {
            return new float[]{0.55f, 1.0f, 0.55f};
        }
        if (entity instanceof Player) {
            return new float[]{0.45f, 0.85f, 1.0f};
        }
        if (entity instanceof Enemy) {
            return new float[]{1.0f, 0.35f, 0.35f};
        }
        return new float[]{0.4f, 1.0f, 0.5f};
    }

    /** Emits one camera-facing quad: the mote is a billboard, not a flat sheet. */
    private static void emitMote(BufferBuilder builder, Matrix4f matrix, double x, double y, double z,
                                 Vec3 right, Vec3 up, double size, float r, float g, float b, float a) {
        builder.vertex(matrix, (float) (x - (right.x + up.x) * size), (float) (y - (right.y + up.y) * size),
                (float) (z - (right.z + up.z) * size)).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) (x + (right.x - up.x) * size), (float) (y + (right.y - up.y) * size),
                (float) (z + (right.z - up.z) * size)).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) (x + (right.x + up.x) * size), (float) (y + (right.y + up.y) * size),
                (float) (z + (right.z + up.z) * size)).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) (x - (right.x - up.x) * size), (float) (y - (right.y - up.y) * size),
                (float) (z - (right.z - up.z) * size)).color(r, g, b, a).endVertex();
    }

    /**
     * A vertex consumer that keeps only where it was asked to draw, deduplicated to the model's
     * corners. Everything else - colour, texture, normal, light - is irrelevant to a particle shell.
     */
    private static final class PointCapture implements VertexConsumer {

        private final List<Vec3> points = new ArrayList<>();
        private final Set<Vec3> unique = new HashSet<>();
        private final Vector3f scratch = new Vector3f();

        MultiBufferSource source() {
            return type -> this;
        }

        List<Vec3> points() {
            return this.points;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            add(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
            matrix.transformPosition(x, y, z, this.scratch);
            add(this.scratch.x(), this.scratch.y(), this.scratch.z());
            return this;
        }

        private void add(double x, double y, double z) {
            // A cube repeats each corner once per face; rounding to a hundredth of a block folds
            // those copies (and parts that happen to overlap) into one point.
            Vec3 point = new Vec3(Math.round(x * 100d) / 100d,
                    Math.round(y * 100d) / 100d, Math.round(z * 100d) / 100d);
            if (this.unique.add(point)) {
                this.points.add(point);
            }
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public void endVertex() {
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
        }

        @Override
        public void unsetDefaultColor() {
        }
    }

    /**
     * A vertex consumer that keeps the model's quads in order and drops everything else. Unlike
     * {@link PointCapture} the raw vertex stream keeps repeated corners, because a face needs all
     * four of them; {@link #points()} folds them back into the surface points the tenketsu use.
     */
    private static final class ShapeCapture implements VertexConsumer {

        private float[] data = new float[3072];
        private int count;
        private final Vector3f scratch = new Vector3f();

        MultiBufferSource source() {
            return type -> this;
        }

        float[] data() {
            return this.data;
        }

        /** @return how many vertices were captured. */
        int size() {
            return this.count / 3;
        }

        /**
         * The model's corners, deduplicated the same way the chakra sense's point capture does it:
         * a cube repeats every corner once per face, so rounding to a hundredth of a block folds the
         * copies (and parts that happen to overlap) into one point on the surface.
         */
        List<Vec3> points() {
            List<Vec3> points = new ArrayList<>();
            Set<Vec3> unique = new HashSet<>();
            for (int i = 0; i + 2 < this.count; i += 3) {
                Vec3 point = new Vec3(
                        Math.round(this.data[i] * 100f) / 100d,
                        Math.round(this.data[i + 1] * 100f) / 100d,
                        Math.round(this.data[i + 2] * 100f) / 100d);
                if (unique.add(point)) {
                    points.add(point);
                }
            }
            return points;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            add((float) x, (float) y, (float) z);
            return this;
        }

        @Override
        public VertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
            matrix.transformPosition(x, y, z, this.scratch);
            add(this.scratch.x(), this.scratch.y(), this.scratch.z());
            return this;
        }

        private void add(float x, float y, float z) {
            if (this.count + 3 > this.data.length) {
                if (this.data.length >= MAX_CAPTURE_FLOATS) {
                    return;
                }
                this.data = Arrays.copyOf(this.data, Math.min(this.data.length * 2, MAX_CAPTURE_FLOATS));
            }
            this.data[this.count++] = x;
            this.data[this.count++] = y;
            this.data[this.count++] = z;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public void endVertex() {
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
        }

        @Override
        public void unsetDefaultColor() {
        }
    }
}
