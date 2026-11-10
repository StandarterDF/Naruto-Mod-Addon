package com.sekwah.narutoaddon.client;

/**
 * Client-side cache of the local player's size scale, synced from the server.
 */
public final class ClientSizeData {

    private ClientSizeData() {
    }

    private static float scale = 1.0f;

    public static float get() {
        return scale;
    }

    public static void set(float scale) {
        ClientSizeData.scale = scale;
    }
}