# Reports every narutoaddon:<id> reference in resources that does not correspond to a
# thing the addon actually registers (or, for items, an aliased base item).
param([string]$ProjectRoot = ".")

$java = Join-Path $ProjectRoot "src/main/java/com/sekwah/narutoaddon"
$res  = Join-Path $ProjectRoot "src/main/resources"

function Get-Registered([string]$file, [string]$pattern) {
    if (-not (Test-Path $file)) { return @() }
    Select-String -Path $file -Pattern $pattern -AllMatches |
        ForEach-Object { $_.Matches } | ForEach-Object { $_.Groups[1].Value }
}

# items the addon registers itself
$items = Get-Registered (Join-Path $java "item/NarutoItems.java") 'ITEMS\.register\("([a-z0-9_]+)"'
$items += Get-Registered (Join-Path $java "block/NarutoBlocks.java") 'BLOCK_ITEMS\.register\("([a-z0-9_]+)"'
# items that are aliases of the base mod
$alias = @()
$addonItemsSrc = Join-Path $java "item/NarutoItems.java"
if (Test-Path $addonItemsSrc) {
    $alias = Select-String -Path $addonItemsSrc -Pattern '= com\.sekwah\.narutomod\.item\.NarutoItems\.([A-Z_0-9]+)' -AllMatches |
        ForEach-Object { $_.Matches } | ForEach-Object { $_.Groups[1].Value.ToLower() }
}
# blocks registered by the addon
$blocks = Get-Registered (Join-Path $java "block/NarutoBlocks.java") 'BLOCKS\.register\("([a-z0-9_]+)"'
# effects
$effects = Get-Registered (Join-Path $java "effect/NarutoEffects.java") 'EFFECTS\.register\("([a-z0-9_]+)"'
# lore-relevant: also accept anything registered in other DeferredRegisters with the same id shape
$other = @()
Get-ChildItem $java -Recurse -Filter *.java | Select-String -Pattern '\.register\("([a-z0-9_]+)"' -AllMatches |
    ForEach-Object { $_.Matches } | ForEach-Object { $_.Groups[1].Value } | ForEach-Object { $other += $_ }

$known = New-Object System.Collections.Generic.HashSet[string]
foreach ($x in ($items + $blocks + $effects + $other + $alias)) { if ($x) { [void]$known.Add($x) } }

# dynamic item ids: every addon item has a model under models/item
$itemModelDir = Join-Path $ProjectRoot "src/main/resources/assets/narutoaddon/models/item"
if (Test-Path $itemModelDir) {
    Get-ChildItem $itemModelDir -Filter *.json | ForEach-Object { [void]$known.Add($_.BaseName) }
}

# advancement triggers/ids, loot modifier/table names, damage types and worldgen ids are valid narutoaddon: ids
foreach ($sub in @('advancements','loot_modifiers','loot_tables','damage_type','worldgen')) {
    $dir = Join-Path $ProjectRoot "src/main/resources/data/narutoaddon/$sub"
    if (Test-Path $dir) {
        Get-ChildItem $dir -Recurse -Filter *.json | ForEach-Object { [void]$known.Add($_.BaseName) }
    }
}

# advancement trigger ids registered in code (CriteriaTriggers.register)
$triggersSrc = Join-Path $ProjectRoot "src/main/java/com/sekwah/narutoaddon/triggers/NarutoTriggers.java"
if (Test-Path $triggersSrc) {
    $traw = Get-Content $triggersSrc -Raw
    foreach ($tm in [regex]::Matches($traw, 'new ResourceLocation\("narutoaddon", "([^"]+)"\)')) {
        [void]$known.Add($tm.Groups[1].Value)
    }
}

"own item ids: $($items.Count), own block ids: $($blocks.Count), alias ids: $($alias.Count)"
"total known short ids: $($known.Count)"

$assetRoot = Join-Path $ProjectRoot "src/main/resources/assets/narutoaddon"

$broken = @{}
$files = Get-ChildItem $res -Recurse -Filter *.json
foreach ($f in $files) {
    if ($f.FullName -match '\\lang\\' -or $f.FullName -match '/lang/') { continue }
    $raw = Get-Content $f.FullName -Raw
    foreach ($m in [regex]::Matches($raw, 'narutoaddon:([a-z0-9_./-]+)')) {
        $id = $m.Groups[1].Value
        $after = $raw.Substring($m.Index + $m.Length)
        if ($after -match '^\.(desc|name)') { continue }
        if ($id -match '\.(desc|name)$') { continue }
        # resource paths that exist on disk are not registry ids
        $assetPath = Join-Path $assetRoot $id
        if ((Test-Path $assetPath) -or (Test-Path "$assetPath.json") -or (Test-Path "$assetPath.png") -or (Test-Path "$assetPath.ogg")) { continue }
        # obvious resource-path prefixes
        if ($id -match '^(items/|item/|models/|sounds/|jutsu/)') { continue }
        $short = ($id -split '/')[-1]
        if ($known.Contains($id)) { continue }
        if ($known.Contains($short)) { continue }
        # item ids are the only ones that end up under the item registry; report unknown short ids
        $rel = $f.FullName.Substring($res.Length + 1)
        if (-not $broken.ContainsKey($id)) { $broken[$id] = New-Object System.Collections.Generic.List[string] }
        if (-not $broken[$id].Contains($rel)) { $broken[$id].Add($rel) }
    }
}

"=== unknown narutoaddon: ids ($($broken.Count)) ==="
foreach ($k in ($broken.Keys | Sort-Object)) {
    $files = ($broken[$k] | Select-Object -First 3) -join ", "
    "$k  <- $files"
}
