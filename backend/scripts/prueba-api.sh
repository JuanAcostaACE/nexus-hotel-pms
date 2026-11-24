B=${B:-http://localhost:8081}
j(){ sed -n 's/.*"id":"\([^"]*\)".*/\1/p' | head -1; }
tok(){ curl -s -X POST $B/api/auth/login -H 'Content-Type: application/json' -d "{\"email\":\"$1\",\"password\":\"$2\"}" | sed 's/.*"token":"\([^"]*\)".*/\1/'; }
A=$(tok admin@hotel.com 'Admin123*'); R=$(tok recepcionista@hotel.com 'Recep123*')
SN=$(curl -s $B/api/sedes | sed 's/.*"nombre":"Sede Norte".*"id":"\([^"]*\)".*/\1/' | head -1)
SN=$(curl -s $B/api/sedes | python -c "import json,sys;print([s['id'] for s in json.load(sys.stdin) if s['nombre']=='Sede Norte'][0])")
c(){ printf "%-45s %s\n" "$1" "$2"; }
# HU-13 sedes
S=$(curl -s -X POST $B/api/admin/sedes -H "Authorization: Bearer $A" -H 'Content-Type: application/json' -d '{"nombre":"Sede Prueba","direccion":"Calle 1"}' | j); c "HU-13 crear sede" "$S"
c "HU-13 editar sede" "$(curl -s -o /dev/null -w %{http_code} -X PUT $B/api/admin/sedes/$S -H "Authorization: Bearer $A" -H 'Content-Type: application/json' -d '{"nombre":"Sede Prueba 2","direccion":"Calle 2"}')"
c "HU-13 inactivar sede" "$(curl -s -o /dev/null -w %{http_code} -X POST $B/api/admin/sedes/$S/estado -H "Authorization: Bearer $A" -H 'Content-Type: application/json' -d '{"activa":false}')"
# HU-14 usuarios
E="prueba$RANDOM@hotel.com"
U=$(curl -s -X POST $B/api/admin/usuarios -H "Authorization: Bearer $A" -H 'Content-Type: application/json' -d "{\"nombre\":\"Prueba\",\"email\":\"$E\",\"password\":\"Prueba123*\",\"sedeId\":\"$SN\"}" | j); c "HU-14 crear recepcionista" "$U"
c "HU-14 inactivar usuario" "$(curl -s -o /dev/null -w %{http_code} -X DELETE $B/api/admin/usuarios/$U -H "Authorization: Bearer $A")"
c "HU-14 login inactivo (esperado 401/403)" "$(curl -s -o /dev/null -w %{http_code} -X POST $B/api/auth/login -H 'Content-Type: application/json' -d "{\"email\":\"$E\",\"password\":\"Prueba123*\"}")"
# HU-15 habitaciones
N=$((900 + RANDOM % 90)); H=$(curl -s -X POST $B/api/admin/habitaciones -H "Authorization: Bearer $A" -H 'Content-Type: application/json' -d "{\"sedeId\":\"$SN\",\"numero\":\"$N\",\"tipo\":\"Doble\",\"capacidadMax\":2,\"precioBase\":150000}" | j); c "HU-15 crear habitacion $N" "$H"
c "HU-15 editar habitacion" "$(curl -s -o /dev/null -w %{http_code} -X PUT $B/api/admin/habitaciones/$H -H "Authorization: Bearer $A" -H 'Content-Type: application/json' -d '{"tipo":"Suite","capacidadMax":3,"precioBase":200000}')"
c "HU-15 a Mantenimiento" "$(curl -s -o /dev/null -w %{http_code} -X POST $B/api/admin/habitaciones/$H/estado -H "Authorization: Bearer $A" -H 'Content-Type: application/json' -d '{"estado":"Mantenimiento"}')"
c "HU-15 de vuelta a Disponible" "$(curl -s -o /dev/null -w %{http_code} -X POST $B/api/admin/habitaciones/$H/estado -H "Authorization: Bearer $A" -H 'Content-Type: application/json' -d '{"estado":"Disponible"}')"
# HU-16 catálogo
I=$(curl -s -X POST $B/api/admin/catalogo -H "Authorization: Bearer $A" -H 'Content-Type: application/json' -d "{\"sedeId\":\"$SN\",\"nombre\":\"Gaseosa\",\"precio\":4000,\"categoria\":\"Minibar\"}" | j); c "HU-16 crear item" "$I"
c "HU-16 editar item" "$(curl -s -o /dev/null -w %{http_code} -X PUT $B/api/admin/catalogo/$I -H "Authorization: Bearer $A" -H 'Content-Type: application/json' -d '{"nombre":"Gaseosa 350","precio":4500,"categoria":"Minibar"}')"
c "HU-16 inactivar item" "$(curl -s -o /dev/null -w %{http_code} -X POST $B/api/admin/catalogo/$I/estado -H "Authorization: Bearer $A" -H 'Content-Type: application/json' -d '{"activa":false}')"
# HU-02..HU-12 flujo completo en la habitación nueva
F=$(curl -s -X POST $B/api/reservas -H 'Content-Type: application/json' -d "{\"sedeId\":\"$SN\",\"habitacionId\":\"$H\",\"fechaEntrada\":\"$(date +%F)\",\"fechaSalida\":\"$(date -d "+2 days" +%F 2>/dev/null || date +%F)\",\"numHuespedes\":2,\"clienteNombre\":\"Ana Prueba\",\"clienteDocumento\":\"1098$RANDOM\",\"clienteEmail\":\"ana@correo.com\"}" | j); c "HU-02 crear reserva" "$F"
c "HU-03 pagar PSE aprobado" "$(curl -s -o /dev/null -w %{http_code} -X POST $B/api/reservas/$F/pago -H 'Content-Type: application/json' -d '{"banco":"1007","simulacion":"APROBADO"}')"
c "HU-05 comprobante" "$(curl -s -o /dev/null -w %{http_code} $B/api/reservas/$F/comprobante)"
c "HU-08 check-in" "$(curl -s -o /dev/null -w %{http_code} -X POST $B/api/reservas/$F/checkin -H "Authorization: Bearer $R")"
c "HU-09 consumo (hab ocupada)" "$(curl -s -o /dev/null -w %{http_code} -X POST $B/api/habitaciones/$H/consumos -H "Authorization: Bearer $R" -H 'Content-Type: application/json' -d "{\"catalogoItemId\":\"$(curl -s "$B/api/catalogo?sedeId=$SN" -H "Authorization: Bearer $R" | python -c "import json,sys;print([i['id'] for i in json.load(sys.stdin) if i['nombre'].startswith('Agua')][0])")\",\"cantidad\":2}")"
c "HU-10 checkout confirmar" "$(curl -s -o /dev/null -w %{http_code} -X POST "$B/api/habitaciones/$H/checkout?confirmar=true" -H "Authorization: Bearer $R")"
c "HU-11 habilitar" "$(curl -s -o /dev/null -w %{http_code} -X POST $B/api/habitaciones/$H/habilitar -H "Authorization: Bearer $R")"
c "HU-12 factura" "$(curl -s -o /dev/null -w %{http_code} $B/api/reservas/$F/factura -H "Authorization: Bearer $R")"
