# Checks that every narutoaddon: asset reference inside assets/narutoaddon resolves
# to a file in the same namespace. Base-mod (narutomod:) and vanilla references are
# resolved against $BaseAssets when it exists.
param(
    [string]$Assets = "src/main/resources/assets/narutoaddon",
    [string]$BaseAssets = "tmp/basejar/assets/narutomod"
)

$root = (Resolve-Path $Assets).Path
$jsonFiles = Get-ChildItem $root -Recurse -Filter *.json
$errors = New-Object System.Collections.Generic.List[string]
$checked = 0

function Resolve-Ref([string]$ref, [string]$kind) {
    # kind: model | texture
    $idx = $ref.IndexOf(':')
    $ns = if ($idx -ge 0) { $ref.Substring(0, $idx) } else { "minecraft" }
    $path = if ($idx -ge 0) { $ref.Substring($idx + 1) } else { $ref }
    return @{ ns = $ns; path = $path }
}

foreach ($f in $jsonFiles) {
    $checked++
    $raw = Get-Content $f.FullName -Raw
    try { $json = $raw | ConvertFrom-Json } catch {
        $raw2 = $raw -replace '"":', '"_":'
        try { $json = $raw2 | ConvertFrom-Json } catch { $errors.Add("INVALID JSON: $($f.FullName)"); continue }
    }

    # collect all string values anywhere in the document
    $refs = New-Object System.Collections.Generic.List[string]
    $stack = New-Object System.Collections.Stack
    $stack.Push($json)
    while ($stack.Count -gt 0) {
        $node = $stack.Pop()
        if ($null -eq $node) { continue }
        if ($node -is [string]) { $refs.Add($node); continue }
        if ($node -is [System.Management.Automation.PSCustomObject]) {
            foreach ($p in $node.PSObject.Properties) { $stack.Push($p.Value) }
        } elseif ($node -is [System.Collections.IEnumerable]) {
            foreach ($i in $node) { $stack.Push($i) }
        }
    }

    $isSounds = ($f.Name -eq 'sounds.json')
    foreach ($r in $refs) {
        if ($r -notmatch '^[a-z0-9_.-]+:[a-z0-9_./-]+$') { continue }
        if ($r -notmatch '^(narutoaddon|narutomod):(.+)$') { continue }
        $ns = $Matches[1]; $p = $Matches[2]
        if ($ns -eq 'narutoaddon') {
            if ($isSounds) {
                $ok = Test-Path (Join-Path $root "sounds/$p.ogg")
            } elseif ($p -match '\.(obj|png|ogg|json)$') {
                $ok = Test-Path (Join-Path $root $p)
            } else {
                $ok = (Test-Path (Join-Path $root "models/$p.json")) -or
                      (Test-Path (Join-Path $root "textures/$p.png"))
            }
            if (-not $ok) { $errors.Add("UNRESOLVED narutoaddon ref '$r' in $($f.FullName.Substring($root.Length+1))") }
        } else {
            # A narutomod: reference can also be satisfied by a file this mod ships itself
            # (assets/narutomod/** carries the fork assets the base 1.8.1 does not have).
            $ownBase = "src\main\resources\assets\narutomod"
            $ok = (Test-Path (Join-Path $ownBase "$p")) -or
                  (Test-Path (Join-Path $ownBase "models/$p.json")) -or
                  (Test-Path (Join-Path $ownBase "textures/$p.png")) -or
                  (Test-Path (Join-Path $ownBase "blockstates/$p.json"))
            if (-not $ok -and (Test-Path $BaseAssets)) {
                $ok = (Test-Path (Join-Path $BaseAssets "models/$p.json")) -or
                      (Test-Path (Join-Path $BaseAssets "textures/$p.png")) -or
                      (Test-Path (Join-Path $BaseAssets "blockstates/$p.json"))
            }
            if (-not $ok) { $errors.Add("UNRESOLVED narutomod ref '$r' in $($f.FullName.Substring($root.Length+1))") }
        }
    }
}

"checked json files: $checked"
"errors: $($errors.Count)"
$errors | Select-Object -First 60
