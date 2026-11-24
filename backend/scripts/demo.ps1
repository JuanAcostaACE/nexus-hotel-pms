<#
  Demostración en terminal de Nexus Hotel PMS (HU-01 a HU-17).
  Uso:   powershell -ExecutionPolicy Bypass -File scripts\demo.ps1
         (opciones)  -BaseUrl http://localhost:8081   -Auto (sin pausas)   -AbrirPdf (abre los PDF al final)
  Requiere el backend en marcha. Crea datos de prueba en la base configurada.
#>
param(
    [string]$BaseUrl = "http://localhost:8081",
    [switch]$Auto,
    [switch]$AbrirPdf
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$script:total = 0
$script:correctos = 0
$script:fallos = @()
$script:pdfs = @()

# ---------------------------------------------------------------- utilidades
function Pausa {
    if (-not $Auto) { Read-Host "   [Enter para continuar]" | Out-Null }
}

function Titulo([string]$hu, [string]$texto) {
    Write-Host ""
    Write-Host ("=" * 80) -ForegroundColor DarkCyan
    Write-Host "  $hu  ·  $texto" -ForegroundColor Cyan
    Write-Host ("=" * 80) -ForegroundColor DarkCyan
}

function Nota([string]$texto) { Write-Host "   $texto" -ForegroundColor Gray }

function Api([string]$metodo, [string]$ruta, $cuerpo = $null, [string]$token = $null, $cabeceras = $null) {
    $h = @{}
    if ($token) { $h["Authorization"] = "Bearer $token" }
    if ($cabeceras) { foreach ($k in $cabeceras.Keys) { $h[$k] = $cabeceras[$k] } }
    $p = @{ Uri = "$BaseUrl$ruta"; Method = $metodo; Headers = $h; UseBasicParsing = $true }
    if ($null -ne $cuerpo) {
        $p.Body = [Text.Encoding]::UTF8.GetBytes(($cuerpo | ConvertTo-Json -Depth 6))
        $p.ContentType = "application/json; charset=utf-8"
    }
    $bytes = @()
    $status = 0
    try {
        $resp = Invoke-WebRequest @p
        $status = [int]$resp.StatusCode
        $bytes = $resp.RawContentStream.ToArray()
    } catch {
        $r = $_.Exception.Response
        if ($null -eq $r) { throw "No se pudo conectar con $BaseUrl. ¿Está el backend en marcha? ($($_.Exception.Message))" }
        $status = [int]$r.StatusCode
        if ($r -is [System.Net.HttpWebResponse]) {
            $ms = New-Object IO.MemoryStream
            $r.GetResponseStream().CopyTo($ms)
            $bytes = $ms.ToArray()
        } else {
            $bytes = [Text.Encoding]::UTF8.GetBytes([string]$_.ErrorDetails.Message)
        }
    }
    $txt = [Text.Encoding]::UTF8.GetString($bytes)
    $json = $null
    try { $json = $txt | ConvertFrom-Json } catch { }
    [pscustomobject]@{ Status = $status; Json = $json; Texto = $txt; Bytes = $bytes }
}

# Ejecuta una petición, la muestra y verifica el código HTTP esperado.
function Paso([string]$hu, [string]$que, [int]$esperado, [string]$metodo, [string]$ruta,
              $cuerpo = $null, [string]$token = $null, $cabeceras = $null, [scriptblock]$ver = $null, [int]$largo = 230) {
    Write-Host ""
    Write-Host "   » $que" -ForegroundColor White
    Write-Host "     $metodo $ruta" -ForegroundColor Yellow
    if ($null -ne $cuerpo) {
        $c = ($cuerpo | ConvertTo-Json -Compress -Depth 6)
        if ($c.Length -gt 160) { $c = $c.Substring(0, 160) + "…" }
        Write-Host "     cuerpo: $c" -ForegroundColor DarkGray
    }
    $r = Api $metodo $ruta $cuerpo $token $cabeceras
    if ($ver -and $r.Json) {
        & $ver $r.Json
    } elseif ($r.Texto -and $r.Json) {
        $t = ($r.Json | ConvertTo-Json -Compress -Depth 5)
        if ($t.Length -gt $largo) { $t = $t.Substring(0, $largo) + "…" }
        Write-Host "     respuesta: $t" -ForegroundColor Gray
    }
    $script:total++
    if ($r.Status -eq $esperado) {
        $script:correctos++
        Write-Host "     ✔ HTTP $($r.Status)  (esperado $esperado)" -ForegroundColor Green
    } else {
        $script:fallos += "$hu · $que (HTTP $($r.Status), esperado $esperado)"
        Write-Host "     ✘ HTTP $($r.Status)  (esperado $esperado)" -ForegroundColor Red
        if ($r.Texto) { Write-Host "       $($r.Texto)" -ForegroundColor Red }
    }
    return $r
}

# Llamada silenciosa para preparar datos (no se cuenta como paso de la demo).
function Prep([string]$metodo, [string]$ruta, $cuerpo = $null, [string]$token = $null) {
    $r = Api $metodo $ruta $cuerpo $token
    if ($r.Status -ge 400) { throw "Falló la preparación ($metodo $ruta): HTTP $($r.Status) $($r.Texto)" }
    return $r
}

function Login([string]$email, [string]$clave) {
    return (Api "POST" "/api/auth/login" @{ email = $email; password = $clave })
}

# ---------------------------------------------------------------- datos de la corrida
$hoy = (Get-Date).ToString("yyyy-MM-dd")
$manana = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")
$pasado = (Get-Date).AddDays(2).ToString("yyyy-MM-dd")
$en3 = (Get-Date).AddDays(3).ToString("yyyy-MM-dd")
$en5 = (Get-Date).AddDays(5).ToString("yyyy-MM-dd")
$en7 = (Get-Date).AddDays(7).ToString("yyyy-MM-dd")
$sufijo = Get-Random -Minimum 100 -Maximum 999
$cedula = "10" + (Get-Random -Minimum 10000000 -Maximum 99999999)

Clear-Host
Write-Host ""
Write-Host "  NEXUS HOTEL PMS · Demostración del backend (HU-01 a HU-17)" -ForegroundColor Cyan
Write-Host "  Spring Boot 3 · MongoDB Atlas · API REST  →  $BaseUrl" -ForegroundColor Gray
Write-Host ""

# ---------------------------------------------------------------- preparación (silenciosa)
Nota "Preparando datos de la demostración (inicio de sesión y habitaciones de prueba)…"
$a = Login "admin@hotel.com" "Admin123*"
$n = Login "recepcionista@hotel.com" "Recep123*"
$s = Login "recepcion.sur@hotel.com" "Recep123*"
if ($a.Status -ne 200 -or $n.Status -ne 200 -or $s.Status -ne 200) {
    throw "No se pudo iniciar sesión con los usuarios de prueba. ¿Está sembrada la base? (admin $($a.Status), norte $($n.Status), sur $($s.Status))"
}
$adm = $a.Json.token; $rec = $n.Json.token; $sur = $s.Json.token
$sedeNorte = $n.Json.sedeId; $sedeSur = $s.Json.sedeId

function Nueva-Habitacion([string]$tipo, [int]$cap, [int]$precio) {
    $num = [string](Get-Random -Minimum 300 -Maximum 990)
    $h = Prep "POST" "/api/admin/habitaciones" @{ sedeId = $sedeNorte; numero = $num; tipo = $tipo; capacidadMax = $cap; precioBase = $precio } $adm
    return $h.Json
}
$habFlujo = Nueva-Habitacion "Doble" 2 200000        # reserva completa HU-02..HU-12
$habTotal = Nueva-Habitacion "Doble" 2 200000        # cancelación >48 h (reembolso total)
$habParcial = Nueva-Habitacion "Doble" 2 200000      # cancelación <48 h (reembolso parcial)
$aguaId = ((Prep "GET" "/api/catalogo" $null $rec).Json | Where-Object { $_.nombre -like "Agua*" } | Select-Object -First 1).id
Nota "Listo. Habitaciones de prueba: $($habFlujo.numero), $($habTotal.numero), $($habParcial.numero) (Sede Norte)."
Pausa

# ================================================================ HU-01
Titulo "HU-01" "Consulta de disponibilidad de habitaciones (público, sin iniciar sesión)"
$null = Paso "HU-01" "Listar las sedes" 200 "GET" "/api/sedes" -ver { param($j) Write-Host "     sedes: $((($j | ForEach-Object { $_.nombre }) -join ', '))" -ForegroundColor Gray }
$null = Paso "HU-01" "Disponibilidad en Sede Norte para 2 huéspedes" 200 "GET" "/api/sedes/$sedeNorte/disponibilidad?fechaEntrada=$en3&fechaSalida=$en5&huespedes=2" -ver {
    param($j) Write-Host "     habitaciones disponibles: $($j.habitaciones.Count)  ($((($j.habitaciones | ForEach-Object { $_.numero }) -join ', ')))" -ForegroundColor Gray }
$null = Paso "HU-01" "Fechas inválidas (salida antes de la entrada) → rechazado" 422 "GET" "/api/sedes/$sedeNorte/disponibilidad?fechaEntrada=$en5&fechaSalida=$en3&huespedes=2"
Pausa

# ================================================================ HU-02
Titulo "HU-02" "Registro de reserva con bloqueo de 15 minutos y control de concurrencia"
$rsv = Paso "HU-02" "Cliente 1 reserva la habitación $($habFlujo.numero) (queda Bloqueada 15 min)" 201 "POST" "/api/reservas" @{
    sedeId = $sedeNorte; habitacionId = $habFlujo.id; fechaEntrada = $hoy; fechaSalida = $manana; numHuespedes = 2
    clienteNombre = "Ana Pérez"; clienteDocumento = $cedula; clienteEmail = "ana@correo.com" } -ver {
    param($j) Write-Host "     código: $($j.codigo)  estado: $($j.estado)  total: $($j.valorTotal)  bloqueo hasta: $($j.bloqueoExpiraAt)" -ForegroundColor Gray }
$rsvId = $rsv.Json.id
$null = Paso "HU-02" "Cliente 2 intenta la MISMA habitación al mismo tiempo → conflicto" 409 "POST" "/api/reservas" @{
    sedeId = $sedeNorte; habitacionId = $habFlujo.id; fechaEntrada = $hoy; fechaSalida = $manana; numHuespedes = 2
    clienteNombre = "Otro Cliente"; clienteDocumento = "999"; clienteEmail = "otro@correo.com" }
Nota "El bloqueo es atómico en MongoDB (findAndModify): solo un cliente logra la habitación."
Pausa

# ================================================================ HU-03
Titulo "HU-03" "Pago seguro con PSE (simulado) · patrón ADAPTER"
$null = Paso "HU-03" "Webhook de PSE sin la cabecera secreta → rechazado" 403 "POST" "/api/webhooks/pse" @{ transaccionId = "TRX-FALSA"; resultado = "Aprobado" }
$null = Paso "HU-03" "Pagar la reserva con PSE (aprobado)" 200 "POST" "/api/reservas/$rsvId/pago" @{ banco = "1007"; simulacion = "APROBADO" } -ver {
    param($j) Write-Host "     transacción: $($j.transaccionId)  pago: $($j.estadoPago)  reserva: $($j.estadoReserva)" -ForegroundColor Gray }
Nota "PseGatewayAdapter adapta PseClienteSimulado (centavos, OK/FAILED/PENDING) al puerto IPagoGateway."
Pausa

# ================================================================ HU-04
Titulo "HU-04" "Cancelación y reembolso · patrón STRATEGY"
$rt = Paso "HU-04" "Reserva con entrada en 3 días: crear" 201 "POST" "/api/reservas" @{
    sedeId = $sedeNorte; habitacionId = $habTotal.id; fechaEntrada = $en3; fechaSalida = $en5; numHuespedes = 2
    clienteNombre = "Luis Ruiz"; clienteDocumento = "77$sufijo"; clienteEmail = "luis@correo.com" } -ver { param($j) Write-Host "     código: $($j.codigo)  total: $($j.valorTotal)" -ForegroundColor Gray }
$null = Prep "POST" "/api/reservas/$($rt.Json.id)/pago" @{ banco = "1007"; simulacion = "APROBADO" }
$null = Paso "HU-04" "Cancelar con MÁS de 48 h de anticipación → ReembolsoTotal" 200 "POST" "/api/reservas/$($rt.Json.id)/cancelar" -ver {
    param($j) Write-Host "     política: $($j.politica)  reembolso: $($j.reembolso)  retenido: $($j.retenido)  estado: $($j.estado)" -ForegroundColor Gray }
$rp = Paso "HU-04" "Reserva con entrada mañana: crear" 201 "POST" "/api/reservas" @{
    sedeId = $sedeNorte; habitacionId = $habParcial.id; fechaEntrada = $manana; fechaSalida = $pasado; numHuespedes = 2
    clienteNombre = "Marta Gil"; clienteDocumento = "88$sufijo"; clienteEmail = "marta@correo.com" } -ver { param($j) Write-Host "     código: $($j.codigo)  total: $($j.valorTotal)" -ForegroundColor Gray }
$null = Prep "POST" "/api/reservas/$($rp.Json.id)/pago" @{ banco = "1007"; simulacion = "APROBADO" }
$null = Paso "HU-04" "Cancelar con MENOS de 48 h → ReembolsoParcial (retiene una noche)" 200 "POST" "/api/reservas/$($rp.Json.id)/cancelar" -ver {
    param($j) Write-Host "     política: $($j.politica)  reembolso: $($j.reembolso)  retenido: $($j.retenido)  estado: $($j.estado)" -ForegroundColor Gray }
Nota "SelectorPoliticaReembolso elige la estrategia; ReservaService#cancelar es el Context."
Pausa

# ================================================================ HU-05
Titulo "HU-05" "Comprobante de reserva en PDF con código QR"
$pdf = Api "GET" "/api/reservas/$rsvId/comprobante"
$script:total++
$esPdf = ($pdf.Status -eq 200 -and $pdf.Bytes.Length -gt 4 -and [Text.Encoding]::ASCII.GetString($pdf.Bytes[0..3]) -eq "%PDF")
Write-Host ""
Write-Host "   » Descargar el comprobante (solo existe si el pago fue aprobado)" -ForegroundColor White
Write-Host "     GET /api/reservas/$rsvId/comprobante" -ForegroundColor Yellow
if ($esPdf) {
    $archivo = Join-Path $env:TEMP "comprobante-nexus.pdf"
    [IO.File]::WriteAllBytes($archivo, $pdf.Bytes)
    $script:pdfs += $archivo
    $script:correctos++
    Write-Host "     ✔ HTTP 200 · PDF válido de $($pdf.Bytes.Length) bytes guardado en $archivo" -ForegroundColor Green
} else {
    $script:fallos += "HU-05 · comprobante (HTTP $($pdf.Status))"
    Write-Host "     ✘ No se obtuvo un PDF (HTTP $($pdf.Status))" -ForegroundColor Red
}
Pausa

# ================================================================ HU-06
Titulo "HU-06" "Autenticación, roles y bloqueo de cuenta (JWT, sesión de 15 min)"
$null = Paso "HU-06" "Acceder sin token → rechazado" 401 "GET" "/api/habitaciones?sedeId=$sedeNorte"
$null = Paso "HU-06" "Contraseña incorrecta (intento 1 de 3)" 401 "POST" "/api/auth/login" @{ email = "recepcion.sur@hotel.com"; password = "mala" }
$null = Paso "HU-06" "Contraseña incorrecta (intento 2 de 3)" 401 "POST" "/api/auth/login" @{ email = "recepcion.sur@hotel.com"; password = "mala" }
$null = Paso "HU-06" "Contraseña incorrecta (intento 3 de 3) → cuenta bloqueada" 423 "POST" "/api/auth/login" @{ email = "recepcion.sur@hotel.com"; password = "mala" }
$usuarios = (Prep "GET" "/api/admin/usuarios" $null $adm).Json
$idSur = ($usuarios | Where-Object { $_.email -eq "recepcion.sur@hotel.com" } | Select-Object -First 1).id
$null = Paso "HU-06" "El Administrador desbloquea la cuenta" 200 "POST" "/api/admin/usuarios/$idSur/desbloquear" -token $adm
$null = Paso "HU-06" "La recepcionista ya puede iniciar sesión" 200 "POST" "/api/auth/login" @{ email = "recepcion.sur@hotel.com"; password = "Recep123*" } -ver {
    param($j) Write-Host "     rol: $($j.rol)  sede: $($j.sedeId)  expira en: $($j.expiraEnMinutos) min" -ForegroundColor Gray }
$null = Paso "HU-06" "Un recepcionista intenta una función de Administrador → prohibido" 403 "GET" "/api/admin/usuarios" -token $rec
Pausa

# ================================================================ HU-07
Titulo "HU-07" "Búsqueda y localización de reservas en el mostrador"
$null = Paso "HU-07" "Buscar por cédula del huésped" 200 "GET" "/api/reservas/buscar?cedula=$cedula" -token $rec -ver {
    param($j) Write-Host "     reservas encontradas: $($j.reservas.Count)  primera: $($j.reservas[0].codigo) ($($j.reservas[0].estado))" -ForegroundColor Gray }
$null = Paso "HU-07" "Buscar por código/ID de reserva (equivale a escanear el QR)" 200 "GET" "/api/reservas/buscar?id=$rsvId" -token $rec -ver {
    param($j) Write-Host "     reservas encontradas: $($j.reservas.Count)" -ForegroundColor Gray }
Pausa

# ================================================================ HU-08
Titulo "HU-08" "Check-in con timestamp del servidor y registro de no-shows"
$null = Paso "HU-08" "No-show antes de las 23:59 del día de entrada → rechazado" 422 "POST" "/api/reservas/$rsvId/no-show" -token $rec
$null = Paso "HU-08" "Check-in del huésped (reserva Pagada)" 200 "POST" "/api/reservas/$rsvId/checkin" -token $rec -largo 300
Nota "El timestamp de check-in lo pone el servidor y es inalterable. La habitación pasa a Ocupada."
Pausa

# ================================================================ HU-09
Titulo "HU-09" "Registro y anulación de consumos (con auditoría)"
$null = Paso "HU-09" "Catálogo de consumos activos de la sede" 200 "GET" "/api/catalogo" -token $rec -ver {
    param($j) Write-Host "     ítems: $((($j | ForEach-Object { $_.nombre }) -join ', '))" -ForegroundColor Gray }
$c1 = Paso "HU-09" "Registrar consumo: Agua mineral x2 en la habitación ocupada" 201 "POST" "/api/habitaciones/$($habFlujo.id)/consumos" @{ catalogoItemId = $aguaId; cantidad = 2 } -token $rec
$null = Paso "HU-09" "Consumo en una habitación NO ocupada → rechazado" 422 "POST" "/api/habitaciones/$($habTotal.id)/consumos" @{ catalogoItemId = $aguaId; cantidad = 1 } -token $rec
$null = Paso "HU-09" "Anular consumo SIN motivo → rechazado" 422 "POST" "/api/consumos/$($c1.Json.id)/anular" @{ motivo = "" } -token $rec
$null = Paso "HU-09" "Anular consumo con motivo (queda auditado)" 200 "POST" "/api/consumos/$($c1.Json.id)/anular" @{ motivo = "Error de digitación" } -token $rec
$null = Prep "POST" "/api/habitaciones/$($habFlujo.id)/consumos" @{ catalogoItemId = $aguaId; cantidad = 3 } $rec
$null = Paso "HU-09" "Cuenta de la habitación (el consumo anulado no cuenta)" 200 "GET" "/api/habitaciones/$($habFlujo.id)/consumos" -token $rec -ver {
    param($j) Write-Host "     habitación: $($j.habitacionNumero)  reserva: $($j.reservaCodigo)  total consumos: $($j.totalConsumos)" -ForegroundColor Gray }
Pausa

# ================================================================ HU-10
Titulo "HU-10" "Check-out con recargo por salida tardía (después de las 13:00)"
$null = Paso "HU-10" "Resumen de la cuenta, sin confirmar todavía" 200 "POST" "/api/habitaciones/$($habFlujo.id)/checkout" -token $rec -ver {
    param($j) Write-Host "     estadía: $($j.costoEstadia)  consumos: $($j.totalConsumos)  salida tardía: $($j.salidaTardia)  recargo: $($j.recargoLateCheckout)  total: $($j.total)  confirmado: $($j.confirmado)" -ForegroundColor Gray }
$null = Paso "HU-10" "Confirmar el check-out" 200 "POST" "/api/habitaciones/$($habFlujo.id)/checkout?confirmar=true" -token $rec -ver {
    param($j) Write-Host "     reserva: $($j.reservaCodigo)  estado: $($j.estadoReserva)  total: $($j.total)" -ForegroundColor Gray }
Pausa

# ================================================================ HU-11
Titulo "HU-11" "Transición de la habitación a limpieza y habilitación"
$null = Paso "HU-11" "Habitaciones En Limpieza de la sede" 200 "GET" "/api/habitaciones?estado=EnLimpieza" -token $rec -ver {
    param($j) Write-Host "     en limpieza: $((($j | ForEach-Object { $_.numero }) -join ', '))" -ForegroundColor Gray }
$null = Paso "HU-11" "El recepcionista habilita la habitación → Disponible" 200 "POST" "/api/habitaciones/$($habFlujo.id)/habilitar" -token $rec
$null = Paso "HU-11" "Habilitar otra vez (ya no está En Limpieza) → rechazado" 422 "POST" "/api/habitaciones/$($habFlujo.id)/habilitar" -token $rec
Pausa

# ================================================================ HU-12
Titulo "HU-12" "Facturación final y cierre documental (reserva inmutable)"
$null = Paso "HU-12" "Factura con impuestos desglosados (IVA 19 % + consumo 8 %)" 200 "GET" "/api/reservas/$rsvId/factura" -token $rec -largo 420
$fp = Api "GET" "/api/reservas/$rsvId/factura/pdf" $null $rec
$script:total++
if ($fp.Status -eq 200 -and $fp.Bytes.Length -gt 4 -and [Text.Encoding]::ASCII.GetString($fp.Bytes[0..3]) -eq "%PDF") {
    $archivoF = Join-Path $env:TEMP "factura-nexus.pdf"
    [IO.File]::WriteAllBytes($archivoF, $fp.Bytes)
    $script:pdfs += $archivoF
    $script:correctos++
    Write-Host ""
    Write-Host "   » Descargar la factura en PDF" -ForegroundColor White
    Write-Host "     ✔ HTTP 200 · PDF válido de $($fp.Bytes.Length) bytes guardado en $archivoF" -ForegroundColor Green
} else {
    $script:fallos += "HU-12 · factura PDF (HTTP $($fp.Status))"
    Write-Host "     ✘ No se obtuvo el PDF de la factura (HTTP $($fp.Status))" -ForegroundColor Red
}
$null = Paso "HU-12" "Modificar un consumo de una reserva Cerrada → rechazado" 422 "POST" "/api/consumos/$($c1.Json.id)/anular" @{ motivo = "Intento tardío" } -token $rec
Pausa

# ================================================================ HU-13
Titulo "HU-13" "Multi-sede: configuración de sedes y aislamiento de datos"
$sede = Paso "HU-13" "El Administrador crea una sede nueva" 201 "POST" "/api/admin/sedes" @{ nombre = "Sede Demo $sufijo"; direccion = "Calle 10 # 20-30" } -token $adm -ver {
    param($j) Write-Host "     sede: $($j.nombre)  activa: $($j.activa)" -ForegroundColor Gray }
$null = Paso "HU-13" "Editar la sede" 200 "PUT" "/api/admin/sedes/$($sede.Json.id)" @{ nombre = "Sede Demo Editada $sufijo"; direccion = "Carrera 5 # 6-7" } -token $adm
$null = Paso "HU-13" "Inactivar la sede" 200 "POST" "/api/admin/sedes/$($sede.Json.id)/estado" @{ activa = $false } -token $adm -ver {
    param($j) Write-Host "     activa: $($j.activa)" -ForegroundColor Gray }
$null = Paso "HU-13" "AISLAMIENTO: la recepcionista de la Sede Sur busca la reserva de la Norte" 200 "GET" "/api/reservas/buscar?cedula=$cedula" -token $sur -ver {
    param($j) Write-Host "     reservas visibles para la Sede Sur: $($j.reservas.Count)  ($($j.mensaje))" -ForegroundColor Gray }
Pausa

# ================================================================ HU-14
Titulo "HU-14" "Gestión de usuarios y roles con asignación por sede"
$emailNuevo = "demo$sufijo@hotel.com"
$u = Paso "HU-14" "Crear un recepcionista asignado a la Sede Norte" 201 "POST" "/api/admin/usuarios" @{ nombre = "Recepcionista Demo"; email = $emailNuevo; password = "Demo123*"; sedeId = $sedeNorte } -token $adm -ver {
    param($j) Write-Host "     usuario: $($j.email)  rol: $($j.rol)  activo: $($j.activo)" -ForegroundColor Gray }
$null = Paso "HU-14" "Reasignarlo a la Sede Sur" 200 "PUT" "/api/admin/usuarios/$($u.Json.id)/sede" @{ sedeId = $sedeSur } -token $adm
$null = Paso "HU-14" "Darlo de baja (baja lógica, conserva la auditoría)" 200 "DELETE" "/api/admin/usuarios/$($u.Json.id)" -token $adm -ver {
    param($j) Write-Host "     activo: $($j.activo)" -ForegroundColor Gray }
$null = Paso "HU-14" "El usuario dado de baja ya no puede entrar" 401 "POST" "/api/auth/login" @{ email = $emailNuevo; password = "Demo123*" }
Pausa

# ================================================================ HU-15
Titulo "HU-15" "Gestión del inventario de habitaciones (CRUD y estados)"
$hn = Paso "HU-15" "Crear una habitación en la Sede Norte" 201 "POST" "/api/admin/habitaciones" @{ sedeId = $sedeNorte; numero = "D$sufijo"; tipo = "Doble"; capacidadMax = 2; precioBase = 150000 } -token $adm -ver {
    param($j) Write-Host "     habitación: $($j.numero)  tipo: $($j.tipo)  precio: $($j.precioBase)  estado: $($j.estado)" -ForegroundColor Gray }
$null = Paso "HU-15" "Editar tipo, capacidad y precio" 200 "PUT" "/api/admin/habitaciones/$($hn.Json.id)" @{ tipo = "Suite"; capacidadMax = 3; precioBase = 250000 } -token $adm
$null = Paso "HU-15" "Enviar a Mantenimiento (sale de la disponibilidad)" 200 "POST" "/api/admin/habitaciones/$($hn.Json.id)/estado" @{ estado = "Mantenimiento" } -token $adm -ver {
    param($j) Write-Host "     estado: $($j.habitacion.estado)  reservas futuras afectadas: $($j.reservasFuturasAfectadas)" -ForegroundColor Gray }
$null = Paso "HU-15" "Restaurar a Disponible" 200 "POST" "/api/admin/habitaciones/$($hn.Json.id)/estado" @{ estado = "Disponible" } -token $adm
Pausa

# ================================================================ HU-16
Titulo "HU-16" "Configuración del catálogo de consumos"
$it = Paso "HU-16" "Crear un ítem en el catálogo de la Sede Norte" 201 "POST" "/api/admin/catalogo" @{ sedeId = $sedeNorte; nombre = "Gaseosa $sufijo"; precio = 4000; categoria = "Minibar" } -token $adm -ver {
    param($j) Write-Host "     ítem: $($j.nombre)  precio: $($j.precio)  categoría: $($j.categoria)" -ForegroundColor Gray }
$null = Paso "HU-16" "Editar el precio" 200 "PUT" "/api/admin/catalogo/$($it.Json.id)" @{ nombre = "Gaseosa $sufijo 350ml"; precio = 4500; categoria = "Minibar" } -token $adm
$null = Paso "HU-16" "Inactivar el ítem (no se borra: el histórico conserva su precio)" 200 "POST" "/api/admin/catalogo/$($it.Json.id)/estado" @{ activa = $false } -token $adm -ver {
    param($j) Write-Host "     activo: $($j.activo)" -ForegroundColor Gray }
$null = Paso "HU-16" "El catálogo activo que ve el recepcionista ya no lo incluye" 200 "GET" "/api/catalogo" -token $rec -ver {
    param($j) Write-Host "     ítems activos: $((($j | ForEach-Object { $_.nombre }) -join ', '))" -ForegroundColor Gray }
Pausa

# ================================================================ HU-17
Titulo "HU-17" "Reportes operativos de ocupación e ingresos"
$null = Paso "HU-17" "Reporte de ocupación de la Sede Norte (próximos 7 días)" 200 "GET" "/api/admin/reportes/ocupacion?sedeId=$sedeNorte&fechaInicio=$hoy&fechaFin=$en7" -token $adm -ver {
    param($j) Write-Host "     noches vendidas: $($j.nochesVendidas) de $($j.nochesDisponibles)  ocupación: $($j.porcentajeOcupacion) %" -ForegroundColor Gray }
$null = Paso "HU-17" "Reporte de ingresos de la Sede Norte" 200 "GET" "/api/admin/reportes/ingresos?sedeId=$sedeNorte&fechaInicio=$hoy&fechaFin=$en7" -token $adm -ver {
    param($j) Write-Host "     ingresos del período: $($j.ingresos)" -ForegroundColor Gray }
$null = Paso "HU-17" "Rango de fechas inválido → rechazado" 422 "GET" "/api/admin/reportes/ocupacion?sedeId=$sedeNorte&fechaInicio=$en7&fechaFin=$hoy" -token $adm
$null = Paso "HU-17" "Un recepcionista no puede ver los reportes → prohibido" 403 "GET" "/api/admin/reportes/ocupacion?sedeId=$sedeNorte&fechaInicio=$hoy&fechaFin=$en7" -token $rec

# ---------------------------------------------------------------- resumen
Write-Host ""
Write-Host ("=" * 80) -ForegroundColor DarkCyan
Write-Host "  RESUMEN" -ForegroundColor Cyan
Write-Host ("=" * 80) -ForegroundColor DarkCyan
if ($script:fallos.Count -eq 0) {
    Write-Host "  ✔ $($script:correctos) de $($script:total) verificaciones correctas · HU-01 a HU-17 funcionando" -ForegroundColor Green
} else {
    Write-Host "  $($script:correctos) de $($script:total) verificaciones correctas" -ForegroundColor Yellow
    foreach ($f in $script:fallos) { Write-Host "  ✘ $f" -ForegroundColor Red }
}
Write-Host ""
if ($AbrirPdf) { foreach ($p in $script:pdfs) { Start-Process $p } }
else { foreach ($p in $script:pdfs) { Write-Host "  PDF generado: $p" -ForegroundColor Gray } }
