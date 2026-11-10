# Every item id the addon registers itself must have an item model,
# every block id must have a blockstate, every own sound must have an ogg.
param(
    [string]$ProjectRoot = "."
)

$java = Join-Path $ProjectRoot "src/main/java/com/sekwah/narutoaddon"
$assets = Join-Path $ProjectRoot "src/main/resources/assets/narutoaddon"

$itemSrc = Join-Path $java "item/NarutoItems.java"
$blockSrc = Join-Path $java "block/NarutoBlocks.java"

$itemIds = @()
$blockIds = @()

if (Test-Path $itemSrc) {
    $itemIds = Select-String -Path $itemSrc -Pattern 'ITEMS\.register\("([a-z0-9_]+)"' -AllMatches |
        ForEach-Object { $_.Matches } | ForEach-Object { $_.Groups[1].Value }
}
if (Test-Path $blockSrc) {
    $blockIds = Select-String -Path $blockSrc -Pattern 'BLOCKS\.register\("([a-z0-9_]+)"' -AllMatches |
        ForEach-Object { $_.Matches } | ForEach-Object { $_.Groups[1].Value }
    $blockItemIds = Select-String -Path $blockSrc -Pattern 'BLOCK_ITEMS\.register\("([a-z0-9_]+)"' -AllMatches |
        ForEach-Object { $_.Matches } | ForEach-Object { $_.Groups[1].Value }
    $itemIds = $itemIds + $blockItemIds
}
$itemIds = $itemIds | Where-Object { $_ -notmatch '_$' }

$missingModels = @()
foreach ($id in $itemIds) {
    $model = Join-Path $assets "models/item/$id.json"
    $blockstate = Join-Path $assets "blockstates/$id.json"
    if (-not (Test-Path $model) -and -not (Test-Path $blockstate)) { $missingModels += $id }
}

$missingStates = @()
foreach ($id in $blockIds) {
    if (-not (Test-Path (Join-Path $assets "blockstates/$id.json"))) { $missingStates += $id }
}

$missingSounds = @()
$soundsJson = Join-Path $assets "sounds.json"
if (Test-Path $soundsJson) {
    $raw = Get-Content $soundsJson -Raw
    $own = [regex]::Matches($raw, '"narutoaddon:([a-z0-9_./-]+)"') | ForEach-Object { $_.Groups[1].Value }
    foreach ($s in ($own | Sort-Object -Unique)) {
        if (-not (Test-Path (Join-Path $assets "sounds/$s.ogg"))) { $missingSounds += $s }
    }
}

"registered own item ids: $($itemIds.Count)"
"registered own block ids: $($blockIds.Count)"
"missing item models: $($missingModels.Count)"
$missingModels | ForEach-Object { "  - $_" }
"missing blockstates: $($missingStates.Count)"
$missingStates | ForEach-Object { "  - $_" }
"missing own sounds: $($missingSounds.Count)"
$missingSounds | ForEach-Object { "  - $_" }
