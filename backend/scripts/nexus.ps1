# Cliente interactivo de terminal para Nexus Hotel PMS.
# Uso:  powershell -ExecutionPolicy Bypass -File scripts\nexus.ps1  [-BaseUrl http://localhost:8081]
# Requiere el backend en marcha.

param([string]$BaseUrl = "http://localhost:8081")

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [Text.Encoding]::UTF8

$script:token = $null
$script:rol = $null
$script:sedeId = $null
$script:nombre = $null

# ---------------------------------------------------------------- utilidades de pantalla
function Cabecera {
    Clear-Host
    Write-Host ""
    Write-Host "  NEXUS HOTEL PMS · cliente de terminal" -ForegroundColor Cyan
    $quien = if ($script:token) { "$script:nombre · $script:rol" } else { "sin sesión" }
    Write-Host "  $BaseUrl  ·  $quien" -ForegroundColor Gray
    Write-Host ("  " + ("─" * 70)) -ForegroundColor DarkGray
}

function Pausa { Write-Host ""; Read-Host "  [Enter para volver al menú]" | Out-Null }

function Pedir([string]$texto, [string]$defecto = "") {
    $sufijo = if ($defecto) { " [$defecto]" } else { "" }
    $v = Read-Host "  $texto$sufijo"
    if ([string]::IsNullOrWhiteSpace($v)) { return $defecto }
    return $v.Trim()
}

function Ok([string]$m) { Write-Host "  ✔ $m" -ForegroundColor Green }
function Mal([string]$m) { Write-Host "  ✘ $m" -ForegroundColor Red }
function Info([string]$m) { Write-Host "  $m" -ForegroundColor Gray }

# ---------------------------------------------------------------- llamadas a la API
function Api([string]$metodo, [string]$ruta, $cuerpo = $null) {
    $h = @{}
    if ($script:token) { $h["Authorization"] = "Bearer $script:token" }
    $p = @{ Uri = "$BaseUrl$ruta"; Method = $metodo; Headers = $h; UseBasicParsing = $true }
    if ($null -ne $cuerpo) {
        $p.Body = [Text.Encoding]::UTF8.GetBytes(($cuerpo | ConvertTo-Json -Depth 6))
        $p.ContentType = "application/json; charset=utf-8"
    }
    $bytes = [byte[]]@(); $status = 0
    try {
        $r = Invoke-WebRequest @p
        $status = [int]$r.StatusCode; $bytes = $r.RawContentStream.ToArray()
    } catch {
        $resp = $_.Exception.Response
        if ($null -eq $resp) { throw "No hay conexión con $BaseUrl. ¿Está el backend en marcha?" }
        $status = [int]$resp.StatusCode
        $ms = New-Object IO.MemoryStream
        $resp.GetResponseStream().CopyTo($ms); $bytes = $ms.ToArray()
    }
    $txt = [Text.Encoding]::UTF8.GetString($bytes)
    $json = $null
    try { $json = $txt | ConvertFrom-Json } catch { }
    [pscustomobject]@{ Status = $status; Json = $json; Texto = $txt; Bytes = $bytes }
}

# Muestra el mensaje de error del backend de forma legible.
function MostrarError($r) {
    $msg = if ($r.Json -and $r.Json.mensaje) { $r.Json.mensaje } else { $r.Texto }
    Mal "HTTP $($r.Status): $msg"
}

function Mostrar-Tabla($items, [string[]]$columnas) {
    if (-not $items -or @($items).Count -eq 0) { Info "(sin resultados)"; return }
    $items | Format-Table -Property $columnas -AutoSize | Out-String -Width 200 | ForEach-Object { $_.TrimEnd() } | Where-Object { $_ } | ForEach-Object { "  $_" } | Write-Host
}

# ---------------------------------------------------------------- sesión
function Iniciar-Sesion {
    Cabecera
    Write-Host "  Usuarios de prueba:" -ForegroundColor Yellow
    Write-Host "    1  Administrador            admin@hotel.com"
    Write-Host "    2  Recepcionista Sede Norte recepcionista@hotel.com"
    Write-Host "    3  Recepcionista Sede Sur   recepcion.sur@hotel.com"
    Write-Host "    4  Otro usuario (escribir correo y contraseña)"
    Write-Host "    0  Salir"
    $op = Pedir "Elige" "1"
    $mapa = @{
        "1" = @("admin@hotel.com", "Admin123*")
        "2" = @("recepcionista@hotel.com", "Recep123*")
        "3" = @("recepcion.sur@hotel.com", "Recep123*")
    }
    if ($op -eq "0") { return $false }
    if ($op -eq "4") {
        $email = Pedir "Correo"
        $clave = Read-Host "  Contraseña" -AsSecureString
        $clave = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($clave))
    } elseif ($mapa.ContainsKey($op)) {
        $email, $clave = $mapa[$op]
    } else { Mal "Opción no válida"; Pausa; return $false }

    $r = Api "POST" "/api/auth/login" @{ email = $email; password = $clave }
    if ($r.Status -eq 200) {
        $script:token = $r.Json.token
        $script:rol = $r.Json.rol
        $script:sedeId = $r.Json.sedeId
        $script:nombre = $r.Json.nombre
        Ok "Sesión iniciada como $($r.Json.nombre) ($($r.Json.rol))"
        Pausa
        return $true
    }
    MostrarError $r
    Pausa
    return $false
}

function Cerrar-Sesion {
    $script:token = $null; $script:rol = $null; $script:sedeId = $null; $script:nombre = $null
    Ok "Sesión cerrada"
    Start-Sleep -Milliseconds 600
}

function Es-Admin { return $script:rol -eq "Administrador" }

# Lista de sedes para elegir (el administrador puede elegir cualquiera).
function Elegir-Sede {
    $sedes = (Api "GET" "/api/sedes").Json
    if (-not (Es-Admin)) { return $script:sedeId }
    Write-Host ""
    $i = 1
    foreach ($s in $sedes) { Write-Host "    $i  $($s.nombre)"; $i++ }
    $n = [int](Pedir "Sede" "1")
    return $sedes[$n - 1].id
}

# ---------------------------------------------------------------- funciones
function Ver-Sedes {
    Cabecera
    Mostrar-Tabla ((Api "GET" "/api/sedes").Json) @("nombre", "direccion", "activa")
    Pausa
}

function Disponibilidad {
    Cabecera
    $sede = Elegir-Sede
    $en = Pedir "Fecha de entrada (AAAA-MM-DD)" ((Get-Date).AddDays(3).ToString("yyyy-MM-dd"))
    $sa = Pedir "Fecha de salida (AAAA-MM-DD)" ((Get-Date).AddDays(5).ToString("yyyy-MM-dd"))
    $hu = Pedir "Número de huéspedes" "2"
    $r = Api "GET" "/api/sedes/$sede/disponibilidad?fechaEntrada=$en&fechaSalida=$sa&huespedes=$hu"
    if ($r.Status -eq 200) {
        Ok "$($r.Json.habitaciones.Count) habitaciones disponibles"
        Mostrar-Tabla $r.Json.habitaciones @("numero", "tipo", "capacidadMax", "precioBase")
    } else { MostrarError $r }
    Pausa
}

function Crear-Reserva {
    Cabecera
    $sede = Elegir-Sede
    $en = Pedir "Fecha de entrada (AAAA-MM-DD)" (Get-Date).ToString("yyyy-MM-dd")
    $sa = Pedir "Fecha de salida (AAAA-MM-DD)" (Get-Date).AddDays(2).ToString("yyyy-MM-dd")
    $hu = Pedir "Número de huéspedes" "2"
    # Solo habitaciones libres en esas fechas y con capacidad suficiente.
    $disp = Api "GET" "/api/sedes/$sede/disponibilidad?fechaEntrada=$en&fechaSalida=$sa&huespedes=$hu"
    if ($disp.Status -ne 200) { MostrarError $disp; Pausa; return }
    $hab = @($disp.Json.habitaciones)
    if ($hab.Count -eq 0) { Mal "No hay habitaciones disponibles para $hu huésped(es) en esas fechas"; Pausa; return }
    Mostrar-Tabla $hab @("numero", "tipo", "capacidadMax", "precioBase")
    $num = Pedir "Número de habitación" $hab[0].numero
    $habitacion = $hab | Where-Object { $_.numero -eq $num } | Select-Object -First 1
    if (-not $habitacion) { Mal "Esa habitación no está en la lista"; Pausa; return }
    $nombre = Pedir "Nombre del huésped" "Cliente Demo"
    $doc = Pedir "Documento" "1000$(Get-Random -Maximum 99999)"
    $mail = Pedir "Correo" "cliente@correo.com"
    $r = Api "POST" "/api/reservas" @{
        sedeId = $sede; habitacionId = $habitacion.id; fechaEntrada = $en; fechaSalida = $sa
        numHuespedes = [int]$hu; clienteNombre = $nombre; clienteDocumento = $doc; clienteEmail = $mail }
    if ($r.Status -eq 201) {
        Ok "Reserva creada: código $($r.Json.codigo)  ·  estado $($r.Json.estado)  ·  total $($r.Json.valorTotal)"
        Info "Queda bloqueada 15 minutos hasta el pago."
    } else { MostrarError $r }
    Pausa
}

function Buscar-Reservas {
    Cabecera
    Write-Host "    1  Por cédula"
    Write-Host "    2  Por código o ID"
    $op = Pedir "Buscar por" "1"
    if ($op -eq "2") { $q = "id=$(Pedir 'Código o ID de la reserva')" }
    else { $q = "cedula=$(Pedir 'Documento del huésped')" }
    $r = Api "GET" "/api/reservas/buscar?$q"
    if ($r.Status -eq 200) {
        Ok "$($r.Json.reservas.Count) reserva(s)"
        Mostrar-Tabla $r.Json.reservas @("codigo", "clienteNombre", "habitacionNumero", "fechaEntrada", "fechaSalida", "estado", "valorTotal")
    } else { MostrarError $r }
    Pausa
}

function Accion-Reserva([string]$tipo) {
    Cabecera
    $id = Pedir "Código o ID de la reserva"
    if (-not $id) { return }
    switch ($tipo) {
        "pagar" {
            Write-Host "    1 APROBADO   2 RECHAZADO   3 PENDIENTE"
            $op = Pedir "Resultado del pago" "1"
            $sim = @{ "1" = "APROBADO"; "2" = "RECHAZADO"; "3" = "PENDIENTE" }[$op]
            $r = Api "POST" "/api/reservas/$id/pago" @{ banco = "1007"; simulacion = $sim }
            if ($r.Status -eq 200) { Ok "Pago: $($r.Json.estadoPago)  ·  reserva: $($r.Json.estadoReserva)" } else { MostrarError $r }
        }
        "checkin" {
            $r = Api "POST" "/api/reservas/$id/checkin"
            if ($r.Status -eq 200) { Ok "Check-in registrado · estado: $($r.Json.estado)" } else { MostrarError $r }
        }
        "cancelar" {
            $r = Api "POST" "/api/reservas/$id/cancelar"
            if ($r.Status -eq 200) { Ok "Cancelada · política: $($r.Json.politica) · reembolso: $($r.Json.reembolso) · retenido: $($r.Json.retenido)" } else { MostrarError $r }
        }
        "noshow" {
            $r = Api "POST" "/api/reservas/$id/no-show"
            if ($r.Status -eq 200) { Ok "Marcada como No-Show" } else { MostrarError $r }
        }
        "factura" {
            $r = Api "GET" "/api/reservas/$id/factura"
            if ($r.Status -eq 200) {
                Ok "Factura $($r.Json.numeroFactura)  ·  total $($r.Json.total)"
                Info "IVA y consumo: ver factura completa en el JSON."
                $r.Json | ConvertTo-Json -Depth 5 | Out-String | ForEach-Object { $_.Trim() -split "`n" } | Select-Object -First 25 | ForEach-Object { Write-Host "    $_" -ForegroundColor DarkGray }
            } else { MostrarError $r }
        }
        "comprobante" {
            $r = Api "GET" "/api/reservas/$id/comprobante"
            if ($r.Status -eq 200) {
                $f = Join-Path $env:TEMP "comprobante-$id.pdf"
                [IO.File]::WriteAllBytes($f, $r.Bytes)
                Ok "Comprobante PDF guardado: $f"
                Start-Process $f
            } else { MostrarError $r }
        }
    }
    Pausa
}

function Consumo {
    Cabecera
    $hab = Pedir "Número de la habitación (ocupada)"
    $habs = (Api "GET" "/api/habitaciones?sedeId=$(if (Es-Admin) { Elegir-Sede } else { $script:sedeId })").Json
    $h = $habs | Where-Object { $_.numero -eq $hab } | Select-Object -First 1
    if (-not $h) { Mal "No se encontró la habitación $hab en la sede"; Pausa; return }
    $cat = (Api "GET" "/api/catalogo?sedeId=$($h.sedeId)").Json
    Mostrar-Tabla $cat @("nombre", "precio", "categoria")
    $nombre = Pedir "Nombre del ítem" ($cat | Select-Object -First 1).nombre
    $item = $cat | Where-Object { $_.nombre -eq $nombre } | Select-Object -First 1
    if (-not $item) { Mal "Ítem no encontrado"; Pausa; return }
    $cant = [int](Pedir "Cantidad" "1")
    $r = Api "POST" "/api/habitaciones/$($h.id)/consumos" @{ catalogoItemId = $item.id; cantidad = $cant }
    if ($r.Status -eq 201) { Ok "Consumo registrado: $cant x $($item.nombre)" } else { MostrarError $r }
    Pausa
}

function Check-out {
    Cabecera
    $hab = Pedir "Número de la habitación"
    $habs = (Api "GET" "/api/habitaciones?sedeId=$(if (Es-Admin) { Elegir-Sede } else { $script:sedeId })").Json
    $h = $habs | Where-Object { $_.numero -eq $hab } | Select-Object -First 1
    if (-not $h) { Mal "No se encontró la habitación"; Pausa; return }
    $res = Api "POST" "/api/habitaciones/$($h.id)/checkout"
    if ($res.Status -ne 200) { MostrarError $res; Pausa; return }
    Info "Resumen: estadía $($res.Json.costoEstadia) · consumos $($res.Json.totalConsumos) · recargo $($res.Json.recargoLateCheckout) · total $($res.Json.total)"
    if ((Pedir "¿Confirmar el check-out? (s/n)" "n") -eq "s") {
        $c = Api "POST" "/api/habitaciones/$($h.id)/checkout?confirmar=true"
        if ($c.Status -eq 200) { Ok "Check-out confirmado · reserva: $($c.Json.estadoReserva)" } else { MostrarError $c }
    }
    Pausa
}

function Habitaciones-Limpieza {
    Cabecera
    $sede = if (Es-Admin) { Elegir-Sede } else { $script:sedeId }
    $lista = (Api "GET" "/api/habitaciones?sedeId=$sede&estado=EnLimpieza").Json
    Mostrar-Tabla $lista @("numero", "tipo", "estado")
    if ($lista -and @($lista).Count -gt 0) {
        $num = Pedir "Número de la habitación que ya está lista (Enter para omitir)"
        if ($num) {
            $h = $lista | Where-Object { $_.numero -eq $num } | Select-Object -First 1
            if ($h) {
                $r = Api "POST" "/api/habitaciones/$($h.id)/habilitar"
                if ($r.Status -eq 200) { Ok "Habitación $num lista (Disponible)" } else { MostrarError $r }
            }
        }
    }
    Pausa
}

function Admin-Usuarios {
    Cabecera
    Mostrar-Tabla ((Api "GET" "/api/admin/usuarios").Json) @("nombre", "email", "rol", "activo", "bloqueado")
    Pausa
}

function Admin-Sede-Nueva {
    Cabecera
    $n = Pedir "Nombre de la sede"
    $d = Pedir "Dirección"
    $r = Api "POST" "/api/admin/sedes" @{ nombre = $n; direccion = $d }
    if ($r.Status -eq 201) { Ok "Sede creada: $($r.Json.nombre)" } else { MostrarError $r }
    Pausa
}

function Seleccionar($lista, [scriptblock]$etiqueta, [string]$pregunta = "Número") {
    $lista = @($lista)
    if ($lista.Count -eq 0) { Info "(no hay elementos)"; return $null }
    $i = 1
    foreach ($x in $lista) { Write-Host ("    {0,2}  {1}" -f $i, (& $etiqueta $x)); $i++ }
    Write-Host ""
    $n = Pedir "$pregunta (Enter para cancelar)"
    if (-not $n) { return $null }
    $idx = 0
    if (-not [int]::TryParse($n, [ref]$idx) -or $idx -lt 1 -or $idx -gt $lista.Count) { Mal "Número no válido"; return $null }
    return $lista[$idx - 1]
}

function Sedes-Todas { return @((Api "GET" "/api/admin/sedes").Json) }

function Etiqueta-Sede($s) {
    $est = if ($s.activa) { "ACTIVA" } else { "INACTIVA" }
    return ("{0,-9} {1,-24} {2}" -f $est, $s.nombre, $s.direccion)
}

function Elegir-Sede-Admin([string]$pregunta = "Sede") {
    return (Seleccionar (Sedes-Todas) { param($x) Etiqueta-Sede $x } $pregunta)
}

# ---------------------------------------------------------------- HU-13 · Sedes
function Sedes-Listar {
    Cabecera
    Write-Host "  Todas las sedes (activas e inactivas):" -ForegroundColor Yellow
    foreach ($s in (Sedes-Todas)) {
        $color = if ($s.activa) { "Green" } else { "DarkYellow" }
        Write-Host ("    " + (Etiqueta-Sede $s)) -ForegroundColor $color
    }
    Pausa
}

function Sedes-Editar {
    Cabecera
    $s = Elegir-Sede-Admin "Sede a editar"
    if (-not $s) { return }
    $n = Pedir "Nombre" $s.nombre
    $d = Pedir "Dirección" $s.direccion
    $r = Api "PUT" "/api/admin/sedes/$($s.id)" @{ nombre = $n; direccion = $d }
    if ($r.Status -eq 200) { Ok "Sede actualizada: $($r.Json.nombre)" } else { MostrarError $r }
    Pausa
}

function Sedes-Reservas {
    Cabecera
    $s = Elegir-Sede-Admin "Sede cuyas reservas quieres ver"
    if (-not $s) { return }
    $r = Api "GET" "/api/admin/reservas?sedeId=$($s.id)"
    if ($r.Status -eq 200) {
        Ok "$(@($r.Json).Count) reserva(s) en $($s.nombre)"
        Mostrar-Tabla $r.Json @("codigo", "clienteNombre", "habitacionNumero", "fechaEntrada", "fechaSalida", "estado", "valorTotal")
    } else { MostrarError $r }
    Pausa
}

function Menu-Admin-Sedes {
    while ($true) {
        Cabecera
        Write-Host "  SEDES (HU-13)" -ForegroundColor Yellow
        Write-Host "    1  Ver todas las sedes (con su estado)"
        Write-Host "    2  Crear sede"
        Write-Host "    3  Editar sede"
        Write-Host "    4  Activar / inactivar sede"
        Write-Host "    5  Ver las reservas de una sede"
        Write-Host ""
        Write-Host "    0  Volver"
        switch (Pedir "Opción") {
            "1" { Sedes-Listar }
            "2" { Admin-Sede-Nueva }
            "3" { Sedes-Editar }
            "4" { Gestionar-Sedes }
            "5" { Sedes-Reservas }
            "0" { return }
            default { Mal "Opción no válida"; Start-Sleep -Milliseconds 700 }
        }
    }
}

# ---------------------------------------------------------------- HU-14 · Usuarios
function Etiqueta-Usuario($u) {
    $est = if ($u.bloqueado) { "BLOQUEADO" } elseif ($u.activo) { "activo" } else { "INACTIVO" }
    return ("{0,-10} {1,-32} {2,-14} {3}" -f $est, $u.email, $u.rol, $u.nombre)
}

function Usuarios-Ver {
    Cabecera
    Write-Host "    1  Todos los usuarios"
    Write-Host "    2  Solo los de una sede"
    $ruta = "/api/admin/usuarios"
    if ((Pedir "Ver" "1") -eq "2") {
        $s = Elegir-Sede-Admin "Sede"
        if (-not $s) { return }
        $ruta += "?sedeId=$($s.id)"
    }
    $lista = @((Api "GET" $ruta).Json)
    Write-Host ""
    foreach ($u in $lista) { Write-Host ("    " + (Etiqueta-Usuario $u)) -ForegroundColor Gray }
    if ($lista.Count -eq 0) { Info "(sin usuarios)" }
    Pausa
}

function Usuarios-Editar {
    Cabecera
    $u = Seleccionar ((Api "GET" "/api/admin/usuarios").Json) { param($x) Etiqueta-Usuario $x } "Usuario a editar"
    if (-not $u) { return }
    $n = Pedir "Nombre" $u.nombre
    $e = Pedir "Correo" $u.email
    $r = Api "PUT" "/api/admin/usuarios/$($u.id)" @{ nombre = $n; email = $e }
    if ($r.Status -eq 200) { Ok "Usuario actualizado (el rol y la sede no cambian)" } else { MostrarError $r }
    Pausa
}

function Usuarios-Reasignar {
    Cabecera
    $recs = @((Api "GET" "/api/admin/usuarios").Json | Where-Object { $_.rol -eq "Recepcionista" })
    $u = Seleccionar $recs { param($x) Etiqueta-Usuario $x } "Recepcionista a reasignar"
    if (-not $u) { return }
    Write-Host ""
    $s = Elegir-Sede-Admin "Nueva sede"
    if (-not $s) { return }
    $r = Api "PUT" "/api/admin/usuarios/$($u.id)/sede" @{ sedeId = $s.id }
    if ($r.Status -eq 200) { Ok "$($u.email) ahora pertenece a $($s.nombre)" } else { MostrarError $r }
    Pausa
}

function Usuarios-Baja {
    Cabecera
    $act = @((Api "GET" "/api/admin/usuarios").Json | Where-Object { $_.activo })
    $u = Seleccionar $act { param($x) Etiqueta-Usuario $x } "Usuario a desactivar"
    if (-not $u) { return }
    if ((Pedir "¿Desactivar la cuenta de $($u.email)? (s/n)" "n") -ne "s") { Info "Sin cambios."; Pausa; return }
    $r = Api "DELETE" "/api/admin/usuarios/$($u.id)"
    if ($r.Status -eq 200) { Ok "Cuenta desactivada: ya no puede iniciar sesión" } else { MostrarError $r }
    Pausa
}

function Menu-Admin-Usuarios {
    while ($true) {
        Cabecera
        Write-Host "  USUARIOS Y ROLES (HU-14)" -ForegroundColor Yellow
        Write-Host "    1  Ver usuarios (todos o por sede)"
        Write-Host "    2  Crear recepcionista"
        Write-Host "    3  Editar nombre o correo"
        Write-Host "    4  Cambiar de sede a un recepcionista"
        Write-Host "    5  Desactivar cuenta"
        Write-Host "    6  Desbloquear cuenta (HU-06)"
        Write-Host ""
        Write-Host "    0  Volver"
        switch (Pedir "Opción") {
            "1" { Usuarios-Ver }
            "2" { Crear-Recepcionista }
            "3" { Usuarios-Editar }
            "4" { Usuarios-Reasignar }
            "5" { Usuarios-Baja }
            "6" { Desbloquear-Usuario }
            "0" { return }
            default { Mal "Opción no válida"; Start-Sleep -Milliseconds 700 }
        }
    }
}

# ---------------------------------------------------------------- HU-15 · Habitaciones
function Etiqueta-Hab($h) {
    return ("{0,-6} {1,-10} cap. {2}  {3,9}   {4}" -f $h.numero, $h.tipo, $h.capacidadMax, $h.precioBase, $h.estado)
}

function Hab-Listar {
    Cabecera
    $s = Elegir-Sede-Admin "Sede"
    if (-not $s) { return }
    $lista = @((Api "GET" "/api/habitaciones?sedeId=$($s.id)").Json)
    Write-Host ""
    foreach ($h in $lista) { Write-Host ("    " + (Etiqueta-Hab $h)) -ForegroundColor Gray }
    Pausa
}

function Hab-Crear {
    Cabecera
    $s = Elegir-Sede-Admin "Sede de la nueva habitación"
    if (-not $s) { return }
    $num = Pedir "Número de habitación"
    $tipo = Pedir "Tipo (Simple, Doble, Familiar, Suite…)" "Doble"
    $cap = Pedir "Capacidad máxima (huéspedes)"
    $pre = Pedir "Precio base por noche"
    $cuerpo = @{ sedeId = $s.id; numero = $num; tipo = $tipo }
    if ($cap) { $cuerpo.capacidadMax = [int]$cap }
    if ($pre) { $cuerpo.precioBase = [decimal]$pre }
    $r = Api "POST" "/api/admin/habitaciones" $cuerpo
    if ($r.Status -eq 201) { Ok "Habitación $($r.Json.numero) creada en $($s.nombre) (estado $($r.Json.estado))" } else { MostrarError $r }
    Pausa
}

function Hab-Elegir {
    $s = Elegir-Sede-Admin "Sede"
    if (-not $s) { return $null }
    Write-Host ""
    return (Seleccionar (Api "GET" "/api/habitaciones?sedeId=$($s.id)").Json { param($x) Etiqueta-Hab $x } "Habitación")
}

function Hab-Editar {
    Cabecera
    $h = Hab-Elegir
    if (-not $h) { return }
    $tipo = Pedir "Tipo" $h.tipo
    $cap = Pedir "Capacidad máxima" ([string]$h.capacidadMax)
    $pre = Pedir "Precio base" ([string]$h.precioBase)
    $r = Api "PUT" "/api/admin/habitaciones/$($h.id)" @{ tipo = $tipo; capacidadMax = [int]$cap; precioBase = [decimal]$pre }
    if ($r.Status -eq 200) { Ok "Habitación $($h.numero) actualizada" } else { MostrarError $r }
    Pausa
}

function Hab-Estado {
    Cabecera
    $h = Hab-Elegir
    if (-not $h) { return }
    Write-Host "    1  Disponible   2  Mantenimiento"
    $op = Pedir "Nuevo estado" "2"
    $est = if ($op -eq "1") { "Disponible" } else { "Mantenimiento" }
    $r = Api "POST" "/api/admin/habitaciones/$($h.id)/estado" @{ estado = $est }
    if ($r.Status -eq 200) {
        Ok "Habitación $($h.numero) ahora está en $($r.Json.habitacion.estado)"
        if ($r.Json.reservasFuturasAfectadas -gt 0) { Write-Host "  ⚠ Tiene $($r.Json.reservasFuturasAfectadas) reserva(s) futura(s) que podrían verse afectadas." -ForegroundColor Yellow }
    } else { MostrarError $r }
    Pausa
}

function Menu-Admin-Habitaciones {
    while ($true) {
        Cabecera
        Write-Host "  INVENTARIO DE HABITACIONES (HU-15)" -ForegroundColor Yellow
        Write-Host "    1  Ver habitaciones de una sede"
        Write-Host "    2  Crear habitación"
        Write-Host "    3  Editar tipo, capacidad y precio"
        Write-Host "    4  Cambiar estado (Mantenimiento / Disponible)"
        Write-Host ""
        Write-Host "    0  Volver"
        switch (Pedir "Opción") {
            "1" { Hab-Listar }
            "2" { Hab-Crear }
            "3" { Hab-Editar }
            "4" { Hab-Estado }
            "0" { return }
            default { Mal "Opción no válida"; Start-Sleep -Milliseconds 700 }
        }
    }
}

# ---------------------------------------------------------------- HU-16 · Catálogo de consumos
function Etiqueta-Item($i) {
    $est = if ($i.activo) { "activo" } else { "INACTIVO" }
    return ("{0,-9} {1,-28} {2,8}  {3}" -f $est, $i.nombre, $i.precio, $i.categoria)
}

function Cat-Elegir {
    $s = Elegir-Sede-Admin "Sede"
    if (-not $s) { return $null }
    Write-Host ""
    return (Seleccionar (Api "GET" "/api/admin/catalogo?sedeId=$($s.id)").Json { param($x) Etiqueta-Item $x } "Ítem")
}

function Cat-Listar {
    Cabecera
    $s = Elegir-Sede-Admin "Sede"
    if (-not $s) { return }
    Write-Host ""
    foreach ($i in @((Api "GET" "/api/admin/catalogo?sedeId=$($s.id)").Json)) { Write-Host ("    " + (Etiqueta-Item $i)) -ForegroundColor Gray }
    Pausa
}

function Pedir-Categoria {
    Write-Host "    1  Restaurante   2  Minibar"
    return $(if ((Pedir "Categoría" "2") -eq "1") { "Restaurante" } else { "Minibar" })
}

function Cat-Crear {
    Cabecera
    $s = Elegir-Sede-Admin "Sede del nuevo ítem"
    if (-not $s) { return }
    $n = Pedir "Nombre del ítem"
    $p = Pedir "Precio"
    $c = Pedir-Categoria
    $cuerpo = @{ sedeId = $s.id; nombre = $n; categoria = $c }
    if ($p) { $cuerpo.precio = [decimal]$p }
    $r = Api "POST" "/api/admin/catalogo" $cuerpo
    if ($r.Status -eq 201) { Ok "Ítem creado: $($r.Json.nombre) (activo)" } else { MostrarError $r }
    Pausa
}

function Cat-Editar {
    Cabecera
    $i = Cat-Elegir
    if (-not $i) { return }
    $n = Pedir "Nombre" $i.nombre
    $p = Pedir "Precio" ([string]$i.precio)
    $c = Pedir-Categoria
    $r = Api "PUT" "/api/admin/catalogo/$($i.id)" @{ nombre = $n; precio = [decimal]$p; categoria = $c }
    if ($r.Status -eq 200) { Ok "Ítem actualizado. Los consumos ya registrados conservan su precio original." } else { MostrarError $r }
    Pausa
}

function Cat-Estado {
    Cabecera
    $i = Cat-Elegir
    if (-not $i) { return }
    $nuevo = -not [bool]$i.activo
    $r = Api "POST" "/api/admin/catalogo/$($i.id)/estado" @{ activa = $nuevo }
    if ($r.Status -eq 200) { Ok "Ítem '$($i.nombre)' ahora está $(if ($nuevo) { 'ACTIVO' } else { 'INACTIVO' })" } else { MostrarError $r }
    Pausa
}

function Menu-Admin-Catalogo {
    while ($true) {
        Cabecera
        Write-Host "  CATÁLOGO DE CONSUMOS (HU-16)" -ForegroundColor Yellow
        Write-Host "    1  Ver el catálogo de una sede (activos e inactivos)"
        Write-Host "    2  Crear ítem"
        Write-Host "    3  Editar nombre, precio o categoría"
        Write-Host "    4  Activar / inactivar ítem"
        Write-Host ""
        Write-Host "    0  Volver"
        switch (Pedir "Opción") {
            "1" { Cat-Listar }
            "2" { Cat-Crear }
            "3" { Cat-Editar }
            "4" { Cat-Estado }
            "0" { return }
            default { Mal "Opción no válida"; Start-Sleep -Milliseconds 700 }
        }
    }
}

# ---------------------------------------------------------------- HU-17 · Reportes
function Admin-Reporte {
    Cabecera
    $s = Elegir-Sede-Admin "Sede del reporte"
    if (-not $s) { return }
    $ini = Pedir "Fecha inicio (AAAA-MM-DD)" (Get-Date).ToString("yyyy-MM-dd")
    $fin = Pedir "Fecha fin (AAAA-MM-DD)" (Get-Date).AddDays(7).ToString("yyyy-MM-dd")
    $o = Api "GET" "/api/admin/reportes/ocupacion?sedeId=$($s.id)&fechaInicio=$ini&fechaFin=$fin"
    $i = Api "GET" "/api/admin/reportes/ingresos?sedeId=$($s.id)&fechaInicio=$ini&fechaFin=$fin"
    Write-Host ""
    if ($o.Status -eq 200) {
        Ok "Ocupación de $($s.nombre): $($o.Json.porcentajeOcupacion) %  ($($o.Json.nochesVendidas) de $($o.Json.nochesDisponibles) noches)"
        if ($o.Json.mensaje) { Info $o.Json.mensaje }
    } else { MostrarError $o }
    if ($i.Status -eq 200) {
        Ok "Ingresos de $($s.nombre) ($($i.Json.reservasIncluidas) reserva(s))"
        Write-Host "      Estadía:              $($i.Json.ingresosEstadia)"
        Write-Host "      Recargo salida tardía: $($i.Json.ingresosRecargoSalidaTardia)"
        Write-Host "      Consumos:             $($i.Json.ingresosConsumos)"
        Write-Host "      TOTAL GENERAL:        $($i.Json.total)" -ForegroundColor Cyan
        if ($i.Json.mensaje) { Info $i.Json.mensaje }
    } else { MostrarError $i }
    Pausa
}

# ---------------------------------------------------------------- menú principal del administrador
function Menu-Admin {
    while ($script:token) {
        Cabecera
        Write-Host "  ADMINISTRACIÓN (todas las sedes)" -ForegroundColor Yellow
        Write-Host "    1  Sedes (HU-13)"
        Write-Host "    2  Usuarios y roles (HU-14)"
        Write-Host "    3  Inventario de habitaciones (HU-15)"
        Write-Host "    4  Catálogo de consumos (HU-16)"
        Write-Host "    5  Reportes de ocupación e ingresos (HU-17)"
        Write-Host "    6  Consultar disponibilidad (HU-01)"
        Write-Host "    7  Buscar reserva (HU-07)"
        Write-Host "    8  Habitaciones en limpieza (HU-11)"
        Write-Host ""
        Write-Host "    0  Cerrar sesión"
        switch (Pedir "Opción") {
            "1" { Menu-Admin-Sedes }
            "2" { Menu-Admin-Usuarios }
            "3" { Menu-Admin-Habitaciones }
            "4" { Menu-Admin-Catalogo }
            "5" { Admin-Reporte }
            "6" { Disponibilidad }
            "7" { Buscar-Reservas }
            "8" { Habitaciones-Limpieza }
            "0" { Cerrar-Sesion; return }
            default { Mal "Opción no válida"; Start-Sleep -Milliseconds 700 }
        }
    }
}

# ---------------------------------------------------------------- menús
function Gestionar-Sedes {
    Cabecera
    $todas = @((Api "GET" "/api/admin/sedes" $null).Json)
    if ($todas.Count -eq 0) { Mal "No hay sedes"; Pausa; return }
    Write-Host "  Todas las sedes (activas e inactivas):" -ForegroundColor Yellow
    $i = 1
    foreach ($x in $todas) {
        $estado = if ($x.activa) { "ACTIVA  " } else { "INACTIVA" }
        $color = if ($x.activa) { "Green" } else { "DarkYellow" }
        Write-Host ("    {0,2}  {1}  {2,-22} {3}" -f $i, $estado, $x.nombre, $x.direccion) -ForegroundColor $color
        $i++
    }
    Write-Host ""
    $n = Pedir "Número de la sede a cambiar (Enter para volver)"
    if (-not $n) { return }
    $sel = $todas[[int]$n - 1]
    if (-not $sel) { Mal "Número no válido"; Pausa; return }
    $nuevo = -not [bool]$sel.activa
    $accion = if ($nuevo) { "ACTIVAR" } else { "INACTIVAR" }
    if ((Pedir "¿$accion la sede '$($sel.nombre)'? (s/n)" "n") -ne "s") { Info "Sin cambios."; Pausa; return }
    $r = Api "POST" "/api/admin/sedes/$($sel.id)/estado" @{ activa = $nuevo }
    if ($r.Status -eq 200) {
        Ok "Sede '$($sel.nombre)' ahora está $(if ($nuevo) { 'ACTIVA' } else { 'INACTIVA' })"
        Info "Recepción dejará de ver esta sede en el menú cuando esté inactiva."
    } else { MostrarError $r }
    Pausa
}

function Crear-Recepcionista {
    Cabecera
    Info "Crea un usuario recepcionista asignado a una sede (HU-14)."
    Write-Host ""
    $nombre = Pedir "Nombre completo"
    $email = Pedir "Correo (será su usuario para iniciar sesión)"
    $clave = Read-Host "  Contraseña inicial (mín. 8 caracteres)" -AsSecureString
    $clave = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($clave))
    Write-Host "  Sede asignada:"
    $sede = Elegir-Sede
    if (-not $nombre -or -not $email -or -not $clave) { Mal "Faltan datos obligatorios"; Pausa; return }
    $r = Api "POST" "/api/admin/usuarios" @{ nombre = $nombre; email = $email; password = $clave; sedeId = $sede }
    if ($r.Status -eq 201) {
        Ok "Recepcionista creado: $($r.Json.email)  ·  sede asignada"
        Info "Ya puede iniciar sesión con la opción 4 del inicio de sesión."
    } else { MostrarError $r }
    Pausa
}

function Desbloquear-Usuario {
    Cabecera
    $usuarios = @((Api "GET" "/api/admin/usuarios" $null).Json)
    $i = 1
    foreach ($u in $usuarios) {
        $est = if ($u.bloqueado) { "BLOQUEADO" } elseif ($u.activo) { "activo" } else { "inactivo" }
        $color = if ($u.bloqueado) { "Red" } else { "Gray" }
        Write-Host ("    {0,2}  {1,-10} {2,-34} {3}" -f $i, $est, $u.email, $u.rol) -ForegroundColor $color
        $i++
    }
    Write-Host ""
    $n = Pedir "Número del usuario a desbloquear (Enter para volver)"
    if (-not $n) { return }
    $sel = $usuarios[[int]$n - 1]
    if (-not $sel) { Mal "Número no válido"; Pausa; return }
    $r = Api "POST" "/api/admin/usuarios/$($sel.id)/desbloquear" $null
    if ($r.Status -eq 200) { Ok "Cuenta de $($sel.email) desbloqueada" } else { MostrarError $r }
    Pausa
}

function Menu-Recepcion {
    while ($script:token) {
        Cabecera
        Write-Host "  RECEPCIÓN" -ForegroundColor Yellow
        Write-Host "    1  Ver sedes"
        Write-Host "    2  Consultar disponibilidad"
        Write-Host "    3  Crear reserva"
        Write-Host "    4  Buscar reserva (cédula o código)"
        Write-Host "    5  Pagar reserva (PSE simulado)"
        Write-Host "    6  Descargar comprobante PDF"
        Write-Host "    7  Check-in"
        Write-Host "    8  Marcar No-Show"
        Write-Host "    9  Registrar consumo"
        Write-Host "   10  Check-out"
        Write-Host "   11  Habitaciones en limpieza"
        Write-Host "   12  Ver factura"
        Write-Host "   13  Cancelar reserva"
        Write-Host ""
        Write-Host "    0  Cerrar sesión"
        $op = Pedir "Opción"
        switch ($op) {
            "1"  { Ver-Sedes }
            "2"  { Disponibilidad }
            "3"  { Crear-Reserva }
            "4"  { Buscar-Reservas }
            "5"  { Accion-Reserva "pagar" }
            "6"  { Accion-Reserva "comprobante" }
            "7"  { Accion-Reserva "checkin" }
            "8"  { Accion-Reserva "noshow" }
            "9"  { Consumo }
            "10" { Check-out }
            "11" { Habitaciones-Limpieza }
            "12" { Accion-Reserva "factura" }
            "13" { Accion-Reserva "cancelar" }
            "0"  { Cerrar-Sesion; return }
            default { Mal "Opción no válida"; Start-Sleep -Milliseconds 700 }
        }
    }
}

# ---------------------------------------------------------------- arranque
try { $null = Api "GET" "/api/sedes" } catch {
    Cabecera; Mal "No hay conexión con $BaseUrl. Arranca el backend en IntelliJ y vuelve a intentar."; Pausa; exit 1
}

while ($true) {
    if (-not $script:token) {
        if (-not (Iniciar-Sesion)) {
            if ((Pedir "¿Salir del programa? (s/n)" "s") -eq "s") { break }
            continue
        }
    }
    if (Es-Admin) { Menu-Admin } else { Menu-Recepcion }
}
Cabecera
Write-Host "  Hasta luego." -ForegroundColor Cyan
