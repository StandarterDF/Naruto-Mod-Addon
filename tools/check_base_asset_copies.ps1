# Lists which files under src/main/resources are the same bytes as files inside the
# NarutoMod jars, so that copies of the base mod's art/data can be spotted and removed.
#
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools/check_base_asset_copies.ps1
#
# Prints a summary and writes the full table to tmp/base_asset_copies.csv.
# "SamePath" means the addon ships the file under the very same resource path the base
# mod uses (a hard duplicate: the base jar already provides it at runtime).
param(
    [string]$AddonRes = 'src/main/resources',
    [string]$BaseJar  = 'libs/NarutoMod-1.20-1.8.1-deobf.jar',
    [string]$ForkJar  = 'libs/maven/com/sekwah/narutomod/1.11.0/narutomod-1.11.0.jar',
    [string]$OutCsv   = 'tmp/base_asset_copies.csv'
)

Add-Type -AssemblyName System.IO.Compression.FileSystem
$root = (Get-Location).Path

# md5 -> list of "jar!entry" for both jars
$known = @{}
foreach ($jar in @($BaseJar, $ForkJar)) {
    if (-not (Test-Path $jar)) { Write-Warning "missing jar: $jar"; continue }
    $zip = [System.IO.Compression.ZipFile]::OpenRead((Join-Path $root $jar))
    try {
        foreach ($e in $zip.Entries) {
            if ($e.Length -eq 0) { continue }
            $ms = New-Object System.IO.MemoryStream
            $s = $e.Open(); $s.CopyTo($ms); $s.Close()
            $hash = [System.BitConverter]::ToString(
                [System.Security.Cryptography.MD5]::Create().ComputeHash($ms.ToArray())).Replace('-', '')
            $ms.Dispose()
            if (-not $known.ContainsKey($hash)) { $known[$hash] = @() }
            $known[$hash] += "$jar!$($e.FullName)"
        }
    } finally { $zip.Dispose() }
}

$rows = @()
Get-ChildItem -Recurse -File $AddonRes | ForEach-Object {
    $rel = $_.FullName.Substring((Join-Path $root $AddonRes).Length + 1).Replace('\', '/')
    $hash = (Get-FileHash $_.FullName -Algorithm MD5).Hash
    if (-not $known.ContainsKey($hash)) { return }
    $matches = $known[$hash]
    $inBase = $matches | Where-Object { $_ -like "$BaseJar!*" }
    $inFork = $matches | Where-Object { $_ -like "$ForkJar!*" }
    $samePath = @($matches | Where-Object { ($_ -split '!', 2)[1] -eq $rel }).Count -gt 0
    $rows += [pscustomobject]@{
        File      = $rel
        SamePath  = $samePath
        InBase181 = [bool]$inBase
        InFork    = [bool]$inFork
        Matches   = ($matches -join ' | ')
    }
}

"files with content identical to a base-mod file: $($rows.Count)"
"  same resource path (hard duplicate)          : $(@($rows | Where-Object SamePath).Count)"
"  present in the official 1.8.1 jar            : $(@($rows | Where-Object InBase181).Count)"
"  present only in the 1.11.0 content set       : $(@($rows | Where-Object { -not $_.InBase181 }).Count)"
""

$rows | Group-Object { ($_.File -split '/')[0..2] -join '/' } |
    Sort-Object Count -Descending |
    Select-Object Count, Name | Format-Table -AutoSize

New-Item -ItemType Directory -Force -Path (Split-Path $OutCsv) | Out-Null
$rows | Select-Object File, SamePath, InBase181, InFork | Export-Csv $OutCsv -NoTypeInformation -Encoding UTF8
"written: $OutCsv"
