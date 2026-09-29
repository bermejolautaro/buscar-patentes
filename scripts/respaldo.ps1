<#
.SYNOPSIS
    Respalda y restaura los datos de buscar-patentes desde el telefono a la PC.

.DESCRIPTION
    Reinstalar la app —no actualizarla, reinstalarla— borra la base y las fotos. Durante el
    desarrollo eso pasa seguido, y lo que se pierde es evidencia que solo existia ahi: las
    coordenadas y la hora de una patente no se pueden volver a generar sin volver al lugar.

    Este script usa `run-as`, que funciona porque el build es de debug. No necesita root ni
    `adb backup`, que en Android moderno esta roto.

    Se respaldan tres cosas:
      - la base Room, con sus archivos -wal y -shm (sin ellos el .db suele venir vacio)
      - las fotos de las patentes
      - las preferencias: geofences activos y deduplicacion de avisos

.PARAMETER Accion
    respaldar  Copia los datos del telefono a respaldos/<fecha-hora>/
    restaurar  Escribe un respaldo de vuelta al telefono
    listar     Muestra los respaldos que hay en la PC

.PARAMETER Desde
    Para restaurar: la carpeta del respaldo. Si se omite, usa el mas reciente.

.EXAMPLE
    ./scripts/respaldo.ps1 respaldar
    ./scripts/respaldo.ps1 listar
    ./scripts/respaldo.ps1 restaurar
    ./scripts/respaldo.ps1 restaurar -Desde respaldos/2026-08-29_001500
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet('respaldar', 'restaurar', 'listar')]
    [string]$Accion,

    [string]$Desde
)

$ErrorActionPreference = 'Stop'

$PAQUETE = 'ar.lauta.buscarpatentes'
$RAIZ = Join-Path (Split-Path -Parent $PSScriptRoot) 'respaldos'
$BASE = 'buscar-patentes.db'
$ARCHIVOS_BASE = @($BASE, "$BASE-shm", "$BASE-wal")
$PREFS = @('geofences.xml', 'avisos.xml')

function Get-Adb {
    $candidatos = @(
        $env:ADB,
        (Get-Command adb -ErrorAction SilentlyContinue).Source,
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "$env:USERPROFILE\AppData\Local\Android\Sdk\platform-tools\adb.exe"
    ) | Where-Object { $_ -and (Test-Path $_) }

    if (-not $candidatos) { throw "No encontre adb. Pone su ruta en la variable ADB." }
    return $candidatos[0]
}

function Assert-Dispositivo($adb) {
    $lineas = & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\sdevice$' }
    if (-not $lineas) { throw "No hay ningun dispositivo conectado por adb." }

    $instalado = & $adb shell pm list packages $PAQUETE
    if (-not $instalado) { throw "$PAQUETE no esta instalado en el dispositivo." }

    # run-as solo funciona sobre builds debuggables. Si falla, el resto no tiene sentido.
    & $adb shell run-as $PAQUETE ls > $null 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "run-as fallo. Solo funciona con el build de debug, no con uno de release."
    }
}

function Copiar-DelTelefono($adb, $rutaRemota, $destinoLocal) {
    # exec-out para que el binario salga sin que adb le traduzca los saltos de linea.
    & $adb exec-out run-as $PAQUETE cat $rutaRemota > $destinoLocal 2>$null
    if ((Get-Item $destinoLocal).Length -eq 0) {
        Remove-Item $destinoLocal
        return $false
    }
    return $true
}

function Copiar-AlTelefono($adb, $origenLocal, $rutaRemota) {
    $temporal = "/data/local/tmp/restaurar_" + [System.IO.Path]::GetFileName($origenLocal)
    & $adb push $origenLocal $temporal > $null
    & $adb shell run-as $PAQUETE cp $temporal $rutaRemota
    $ok = ($LASTEXITCODE -eq 0)
    # `cp` crea el archivo con el umask del shell, no con los permisos del original: sin
    # esto un archivo restaurado queda mas abierto que el que reemplazo.
    if ($ok) { & $adb shell run-as $PAQUETE chmod 660 $rutaRemota > $null 2>&1 }
    & $adb shell rm -f $temporal > $null 2>&1
    return $ok
}

$adb = Get-Adb

switch ($Accion) {

    'listar' {
        if (-not (Test-Path $RAIZ)) { "No hay respaldos todavia."; break }
        Get-ChildItem $RAIZ -Directory | Sort-Object Name -Descending | ForEach-Object {
            $tam = (Get-ChildItem $_.FullName -Recurse -File | Measure-Object Length -Sum).Sum
            $registros = Join-Path $_.FullName 'resumen.txt'
            $nota = if (Test-Path $registros) { (Get-Content $registros -First 1) } else { '' }
            "{0}  {1,8:N0} KB  {2}" -f $_.Name, ($tam / 1KB), $nota
        }
    }

    'respaldar' {
        Assert-Dispositivo $adb

        $destino = Join-Path $RAIZ (Get-Date -Format 'yyyy-MM-dd_HHmmss')
        New-Item -ItemType Directory -Force -Path (Join-Path $destino 'fotos') | Out-Null
        New-Item -ItemType Directory -Force -Path (Join-Path $destino 'prefs') | Out-Null

        foreach ($archivo in $ARCHIVOS_BASE) {
            $ok = Copiar-DelTelefono $adb "databases/$archivo" (Join-Path $destino $archivo)
            if (-not $ok -and $archivo -eq $BASE) { throw "No pude leer la base. Se aborta." }
        }

        $fotos = & $adb shell run-as $PAQUETE ls files/fotos 2>$null |
            Where-Object { $_ -and $_.Trim() }
        foreach ($foto in $fotos) {
            $nombre = $foto.Trim()
            Copiar-DelTelefono $adb "files/fotos/$nombre" (Join-Path $destino "fotos/$nombre") | Out-Null
        }

        foreach ($pref in $PREFS) {
            Copiar-DelTelefono $adb "shared_prefs/$pref" (Join-Path $destino "prefs/$pref") | Out-Null
        }

        $cuentaFotos = (Get-ChildItem (Join-Path $destino 'fotos') -File).Count
        $resumen = "$cuentaFotos fotos, base de $([math]::Round((Get-Item (Join-Path $destino $BASE)).Length / 1KB, 1)) KB"
        Set-Content -Path (Join-Path $destino 'resumen.txt') -Value $resumen -Encoding utf8NoBOM

        "Respaldo en $destino"
        $resumen
    }

    'restaurar' {
        Assert-Dispositivo $adb

        if (-not $Desde) {
            if (-not (Test-Path $RAIZ)) { throw "No hay ningun respaldo para restaurar." }
            $Desde = (Get-ChildItem $RAIZ -Directory | Sort-Object Name -Descending |
                Select-Object -First 1).FullName
        }
        if (-not (Test-Path (Join-Path $Desde $BASE))) {
            throw "En $Desde no hay una base para restaurar."
        }

        # La app tiene que estar quieta: restaurar la base bajo un proceso vivo deja la
        # copia en memoria pisando lo que acabamos de escribir.
        & $adb shell am force-stop $PAQUETE

        foreach ($archivo in $ARCHIVOS_BASE) {
            $local = Join-Path $Desde $archivo
            if (Test-Path $local) {
                if (-not (Copiar-AlTelefono $adb $local "databases/$archivo")) {
                    throw "Fallo al restaurar $archivo."
                }
            }
        }

        $fotos = Get-ChildItem (Join-Path $Desde 'fotos') -File -ErrorAction SilentlyContinue
        if ($fotos) {
            & $adb shell run-as $PAQUETE mkdir -p files/fotos > $null 2>&1
            foreach ($foto in $fotos) {
                Copiar-AlTelefono $adb $foto.FullName "files/fotos/$($foto.Name)" | Out-Null
            }
        }

        # Las preferencias van ultimas y son opcionales: si fallan, la app las reconstruye.
        # Los geofences ademas se reconcilian solos al arrancar.
        $prefs = Get-ChildItem (Join-Path $Desde 'prefs') -File -ErrorAction SilentlyContinue
        foreach ($pref in $prefs) {
            Copiar-AlTelefono $adb $pref.FullName "shared_prefs/$($pref.Name)" | Out-Null
        }

        "Restaurado desde $Desde"
        "Abri la app: los geofences se re-registran solos al arrancar."
    }
}
