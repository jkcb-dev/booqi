# Customer Flow — Domain Discovery (DDD + BDD)

Defined via the same Event Storming process as `docs/domain/provider-flow.md`. See
`docs/DOMAIN.md` for the ubiquitous language this assumes.

**Scope note**, same as the Provider flow: assumes the Customer already has an account (Identity
context — Google/Facebook/Apple ID/email login — not detailed here).

## Event list (final)

1. Se buscó un Servicio (por texto libre y/o por chip de categoría)
2. Se filtraron los resultados por zona/distancia
3. Se visualizó el detalle de un Servicio
4. Se visualizó el perfil completo de un Proveedor (todos sus Servicios, calificación general)
5. Se seleccionó un TimeSlot disponible
6. *(si es a domicilio y sin dirección guardada)* Se agregó la dirección del Cliente
7. Se solicitó una reserva *(con nota adicional opcional)* — evento compartido con
   `provider-flow.md` (`SolicitarReserva`)
8. Se recibió la respuesta del Proveedor: confirmada / rechazada / expirada — ya definido en
   `provider-flow.md`
9. El Cliente canceló una reserva confirmada — hasta 3 horas antes, con motivo
10. Se completó la cita — ya definido en `provider-flow.md` (lo marca el Proveedor)
11. El Cliente calificó la cita completada — ya definido en `provider-flow.md`
12. Se consultó el historial de reservas del Cliente
13. Se actualizó la dirección guardada *(en cualquier momento, no solo al reservar)*

*(Deferred to V2: favoritos — guardar un Proveedor para encontrarlo rápido después.)*

---

## Grupo 1: Búsqueda y Descubrimiento
*(eventos 1-4)*

Todos estos son **consultas de lectura** (queries), no comandos que mutan un agregado — se listan
en la misma tabla por consistencia con el resto del documento, pero no producen un evento de
dominio propio en el sentido estricto de DDD.

| Evento | Query | Actor |
|---|---|---|
| Se buscó un Servicio | `BuscarServicios` | Cliente |
| Se filtraron los resultados por zona/distancia | `FiltrarPorDistancia` | Cliente |
| Se visualizó el detalle de un Servicio | `VerDetalleServicio` | Cliente |
| Se visualizó el perfil completo de un Proveedor | `VerPerfilProveedor` | Cliente |

```gherkin
Escenario: El Cliente busca un Servicio por texto
  Dado que existen Servicios activos en el catálogo
  Cuando el Cliente escribe "manicure" en el buscador
  Entonces se muestran los Servicios cuyo título o descripción coincide con "manicure"

Escenario: El Cliente busca por categoría (chip)
  Dado que existen Servicios activos de varias categorías
  Cuando el Cliente selecciona el chip "Barbería"
  Entonces se muestran solo los Servicios de esa categoría

Escenario: El Cliente filtra por distancia
  Dado que el Cliente tiene su ubicación activada (GPS)
  Cuando aplica un filtro de distancia de 5 km
  Entonces solo se muestran Servicios de Proveedores dentro de ese radio

Escenario: El Cliente ve el detalle de un Servicio
  Dado que el Cliente seleccionó un Servicio de los resultados
  Cuando abre su detalle
  Entonces ve título, fotos, descripción, precio, duración, modalidad, calificación del
    Proveedor, y horarios disponibles

Escenario: El Cliente ve el perfil completo de un Proveedor
  Dado que el Cliente está viendo el detalle de un Servicio
  Cuando toca el nombre/foto del Proveedor
  Entonces ve todos los Servicios que ofrece ese Proveedor, su calificación general, y sus
    comentarios individuales
```

### Reglas de la búsqueda (implementadas en #20 — `BuscarServicios`/`FiltrarPorDistancia`)

La búsqueda es **una sola consulta** sobre Servicios con filtros opcionales combinables (texto,
categoría, ubicación + radio). Un Servicio solo es candidato si está activo, su Proveedor tiene el
perfil completo y no está pausado hoy. **Orden**: distancia ascendente (si el Cliente dio su
ubicación; los resultados sin distancia al final), luego título (sin distinguir mayúsculas ni
acentos), luego id. Sin ubicación: título, luego id. La tarjeta del resultado lleva el Servicio, el
nombre del Proveedor, su calificación (promedio/cantidad) y la distancia si hay ubicación. Las
categorías de los chips: Barbería, Uñas, Limpieza, Masajes, Técnico ("Todos" = sin filtro; los
Servicios sin categoría elegida son "Otro" y solo aparecen bajo "Todos").

```gherkin
Escenario: La búsqueda de texto ignora mayúsculas y acentos y exige todas las palabras
  Dado que existe un Servicio activo "Uñas esculpidas" con descripción "A domicilio"
  Cuando el Cliente escribe "UNAS domicilio" en el buscador
  Entonces se muestra ese Servicio
  Y no se muestra si escribe "unas yoga"

Escenario: Texto y categoría se combinan
  Dado que existen los Servicios "Corte clásico" (Barbería) y "Corte de uñas" (Uñas)
  Cuando el Cliente escribe "corte" y selecciona el chip "Barbería"
  Entonces solo se muestra "Corte clásico"

Escenario: Sin coincidencias no es un error
  Dado que ningún Servicio coincide con lo buscado
  Cuando el Cliente busca
  Entonces ve una lista vacía (contador en 0), no un error

Escenario: Un Servicio deshabilitado no aparece
  Dado que el Proveedor deshabilitó un Servicio
  Cuando el Cliente busca algo que coincide con él
  Entonces no se muestra

Escenario: Un Proveedor con el perfil incompleto no aparece
  Dado que un Proveedor activó el modo Proveedor pero no completó su perfil
  Cuando el Cliente busca
  Entonces no se muestran sus Servicios

Escenario: Un Proveedor pausado hoy no aparece
  Dado que un Proveedor pausó su perfil del 1 al 5 de octubre
  Cuando el Cliente busca el 5 de octubre (el último día de la pausa, inclusive)
  Entonces sus Servicios no se muestran
  Y el 6 de octubre vuelven a aparecer (igual que el día antes del 1)

Escenario: El radio incluye el borde y excluye a los Proveedores sin ubicación
  Dado que el Cliente tiene su ubicación activada
  Cuando aplica un filtro de 5 km
  Entonces se muestran los Proveedores a 5 km o menos, ordenados del más cercano al más lejano
  Y no se muestran los Proveedores cuya ubicación no tiene coordenadas

Escenario: Sin filtro de distancia no se descarta a nadie por falta de coordenadas
  Dado que el Cliente dio su ubicación pero no eligió radio
  Cuando busca
  Entonces ve todos los resultados, los que tienen distancia primero, ordenados por cercanía

Escenario: El filtro de distancia necesita una ubicación y un radio válidos
  Cuando el Cliente aplica un radio sin ubicación, un radio de 0 o negativo, o coordenadas imposibles
  Entonces la búsqueda falla con un error de validación y no consulta el catálogo

Escenario: El detalle de un Servicio deshabilitado o de un perfil incompleto no existe para el Cliente
  Dado que el Servicio fue deshabilitado, o su Proveedor no tiene el perfil completo
  Cuando el Cliente intenta abrir su detalle
  Entonces recibe "no encontrado"
  (un Proveedor pausado solo desaparece de la búsqueda: su detalle y su perfil siguen abiertos,
  y reservar queda bloqueado porque no tiene horarios disponibles)

Escenario: El perfil de un Proveedor solo lista sus Servicios activos
  Dado que un Proveedor tiene un Servicio activo y uno deshabilitado
  Cuando el Cliente abre su perfil
  Entonces ve solo el activo, el promedio y la cantidad de calificaciones del Proveedor, y sus
    reseñas de la más nueva a la más vieja (estrellas, comentario y fecha; sin el nombre de quien
    reseña hasta que exista Identity, #50)
```

*Join Servicio → Proveedor:* `Service.providerId == ProviderProfile.id`, tal como documenta
`DOMAIN.md`. Un Servicio cuyo `providerId` no es el id de un perfil (hoy: las pantallas del
Proveedor usan el id de *usuario* como placeholder) **no se une** y no aparece en el catálogo hasta
que #50 deje los ids consistentes; no hay un join alternativo por `userId`.

---

## Grupo 2: Dirección del Cliente
*(eventos 6, 13)*

| Evento | Comando | Actor | Agregado |
|---|---|---|---|
| Se agregó la dirección del Cliente | `AgregarDireccion` | Cliente | `User` |
| Se actualizó la dirección guardada | `ActualizarDireccion` | Cliente | `User` |

Regla importante: `Booking` guarda una **copia** de la dirección en el momento de solicitar la
reserva (value object embebido), no una referencia viva a `User.direccion` — mismo principio que
ya aplicamos con precio/duración de `Service` en `provider-flow.md`. Si el Cliente actualiza su
dirección después, las reservas ya solicitadas no cambian retroactivamente.

```gherkin
Escenario: El Cliente agrega su dirección al solicitar una reserva a domicilio
  Dado que el Cliente está solicitando una reserva de un Servicio a domicilio
  Y no tiene una dirección guardada en su perfil
  Cuando busca su dirección en Google Maps o la selecciona en el mapa
  Entonces la dirección queda guardada en su perfil
  Y se usa para esta reserva

Escenario: El Cliente ya tiene dirección guardada al solicitar a domicilio
  Dado que el Cliente ya tiene una dirección guardada
  Cuando solicita una reserva a domicilio
  Entonces se usa automáticamente esa dirección, sin pedirla de nuevo

Escenario: El Cliente actualiza su dirección guardada
  Dado que el Cliente tiene una dirección guardada
  Cuando la actualiza desde su perfil
  Entonces la nueva dirección reemplaza a la anterior para reservas futuras
  Y las reservas ya solicitadas conservan la dirección que tenían al momento de solicitarse

Escenario: El Cliente intenta guardar una dirección incompleta
  Dado que el Cliente está agregando o actualizando su dirección
  Cuando la dirección no tiene texto, o su ubicación en el mapa no es válida
  Entonces el sistema rechaza el guardado y le pide completarla
  Y su dirección guardada no cambia

Escenario: Solo se guarda una dirección
  Dado que el Cliente ya tiene una dirección guardada
  Cuando intenta agregar otra en lugar de actualizar la existente
  Entonces el sistema rechaza la operación: solo hay una dirección y se cambia actualizándola

Escenario: Un visitante sin sesión intenta usar la dirección guardada
  Dado que no hay una sesión iniciada
  Cuando intenta agregar, actualizar o consultar la dirección guardada
  Entonces el sistema responde que debe iniciar sesión

Escenario: El Cliente elimina su cuenta con una dirección guardada
  Dado que el Cliente tiene una dirección guardada y puede eliminar su cuenta
  Cuando elimina su cuenta
  Entonces su dirección guardada se borra junto con el resto de sus datos personales
```

### Notas de implementación (#22, domain & data)

- La dirección guardada es `User.address: Address?` (mismo value object `Address` del snapshot de
  `Booking`; columnas `profiles.address_line/lat/lng`). **Ambas coordenadas son obligatorias**:
  la dirección siempre sale de una búsqueda de Google Maps o de un pin en el mapa, y ambos traen
  posición. El texto en blanco o una posición fuera de rango / no numérica es `InvalidInput`.
  El selector de mapa y el geocoding son #23/#24.
- Se mantienen **dos comandos** porque son dos eventos distintos del dominio (6 y 13) y tienen
  precondiciones opuestas: `AgregarDireccion` falla con `InvalidInput` si ya hay una (solo hay
  una); `ActualizarDireccion` falla con `NotFound` si no hay ninguna. `ObtenerMiDireccion`
  devuelve `Address?` (`null` = "pedirla"), y todos fallan con `Unauthorized` sin sesión.
- Usar la dirección guardada como snapshot por defecto al solicitar una reserva a domicilio
  (escenarios 1 y 2 del lado de la solicitud) es de **#26**; `Booking` y `SolicitarReserva` no se
  tocan aquí.

---

## Grupo 3: Reservas y Calificaciones (lado Cliente)
*(eventos 5, 7-12 — varios ya definidos en `provider-flow.md`, reutilizados aquí desde la
perspectiva del Cliente)*

| Evento | Comando | Actor | Agregado |
|---|---|---|---|
| Se seleccionó un TimeSlot | *(parte de `SolicitarReserva`)* | Cliente | — |
| Se solicitó una reserva | `SolicitarReserva` | Cliente | `Booking` (nuevo) — *ya definido* |
| Se recibió la respuesta del Proveedor | — | Proveedor | `Booking` — *ya definido* |
| El Cliente canceló una reserva confirmada | `CancelarReservaCliente` | Cliente | `Booking` |
| Se completó la cita | — | Proveedor | `Booking` — *ya definido* |
| El Cliente calificó la cita | `CalificarCita` | Cliente | `Booking` — *ya definido* |
| Se consultó el historial de reservas | `VerHistorialReservas` | Cliente | *(query)* |

Motivos predefinidos de cancelación del Cliente (distintos a los del Proveedor): *"Cambio de
planes"*, *"Encontré otro Proveedor"*, *"Ya no necesito el servicio"*, *"Otro"* (+ texto libre).
Ventana de cancelación: **hasta 3 horas antes** de la cita (más amplia que si fuera 2h, para
compensar factores como tráfico en servicios a domicilio).

### Notas de implementación (#25 — `CancelarReservaCliente` / `VerHistorialReservas`)

- **Un solo state machine.** La cancelación del Cliente es `Booking.cancelByCustomer`
  (`Confirmed → CancelledByCustomer`, la misma tabla de `BookingStatus` que usa todo el resto);
  desde cualquier otro estado es `InvalidInput` (transición inválida), también en terminales.
- **Motivos.** `CustomerReasonCode` (`CHANGE_OF_PLANS`, `FOUND_ANOTHER_PROVIDER`,
  `NO_LONGER_NEEDED`, `OTHER`, códigos guardados `customer_*`) bajo la interfaz sellada
  `ReasonCode`; `cancelByCustomer` solo acepta esa lista (y `reject`/`cancelByProvider` solo la del
  Proveedor), así no se mezclan por error. El texto libre es opcional; en blanco se guarda ausente.
- **Plazo de 3 horas, inclusivo.** Se permite mientras `ahora <= inicio − 3 h`: con exactamente 3
  horas por delante todavía se puede ("hasta 3 horas antes"); un instante después se rechaza.
  `scheduledAt` es la hora local del Proveedor (sin zona horaria todavía) y se interpreta en la zona
  del dispositivo, igual que el resto de la app; `Clock` y zona se inyectan para poder probarlo.
- **Solo la reserva propia.** La reserva debe ser del usuario con sesión (`customerId == user.id`);
  la de otro, o no tener sesión, es `Unauthorized`. Una reserva inexistente es `NotFound`.
- **El TimeSlot se libera** solo, porque `CancelledByCustomer` no ocupa horario
  (`BookingStatus.occupiesSlot`); `ObtenerTimeSlotsDisponibles` vuelve a ofrecerlo.
- **Historial.** `VerHistorialReservas` devuelve un `BookingHistory`: un grupo por estado (solo los
  que tienen reservas) en orden fijo — pendientes, confirmadas, completadas, canceladas por el
  Cliente, canceladas por el Proveedor, rechazadas, vencidas. Dentro del grupo, las abiertas
  (pendiente/confirmada) van de la más próxima a la más lejana y las cerradas de la más reciente a la
  más antigua (por `scheduledAt`, luego id). Sin reservas es una lista vacía; sin sesión,
  `Unauthorized`.
- **Quién reserva.** `SolicitarReserva` resuelve al usuario por sí mismo con la guarda
  `RequerirCuentaVerificada` (sin sesión `Unauthorized`; correo sin verificar `InvalidInput`) y
  `customerId` debe ser ese usuario (otro id → `Unauthorized`). De ahí copia el nombre del Cliente
  como **snapshot** en la `Booking` (`customerNameSnapshot`, ver `identity-flow.md`).
- **Diferido** (no hay sistema de notificaciones): avisar al Proveedor de la cancelación y su motivo.

```gherkin
Escenario: El Cliente cancela una reserva confirmada a tiempo
  Dado que existe una Booking en estado "Confirmed" que empieza en más de 3 horas
  Cuando el Cliente la cancela eligiendo un motivo predefinido (u "Otro")
  Entonces la Booking pasa a estado "CancelledByCustomer"
  Y el TimeSlot vuelve a estar disponible para otros Clientes
  Y el Proveedor es notificado de la cancelación y el motivo

Escenario: El Cliente cancela justo 3 horas antes de la cita
  Dado que existe una Booking en estado "Confirmed" que empieza dentro de exactamente 3 horas
  Cuando el Cliente la cancela
  Entonces la Booking pasa a estado "CancelledByCustomer"
  (el plazo es "hasta 3 horas antes", inclusive)

Escenario: El Cliente intenta cancelar fuera del plazo permitido
  Dado que existe una Booking en estado "Confirmed" que empieza en menos de 3 horas
  Cuando el Cliente intenta cancelarla
  Entonces el sistema rechaza la cancelación
  Y muestra un mensaje indicando que ya no se puede cancelar (fuera del plazo de 3 horas)

Escenario: El Cliente solo puede cancelar reservas confirmadas
  Dado que existe una Booking en estado "Requested", "Completed", "Rejected", "Expired" o cancelada
  Cuando el Cliente intenta cancelarla
  Entonces el sistema rechaza la cancelación (transición inválida) y la Booking no cambia

Escenario: El Cliente no puede cancelar la reserva de otro
  Dado que existe una Booking "Confirmed" de otro Cliente
  Cuando un Cliente distinto intenta cancelarla
  Entonces el sistema responde "no autorizado" y la Booking no cambia

Escenario: Cancelar sin haber iniciado sesión
  Dado que el visitante no tiene una sesión iniciada
  Cuando intenta cancelar una reserva
  Entonces el sistema responde "no autorizado"

Escenario: Solicitar una reserva exige una cuenta verificada
  Dado que el visitante no inició sesión, o inició sesión con un correo sin verificar
  Cuando intenta solicitar una reserva
  Entonces no se crea ninguna Booking
  Y recibe "no autorizado" (sin sesión) o un error de validación pidiendo verificar el correo

Escenario: La reserva guarda el nombre del Cliente al momento de solicitarla
  Dado que la Cliente "Ana" tiene la sesión iniciada
  Cuando solicita una reserva
  Entonces la Booking guarda el nombre "Ana" como copia (snapshot)
  Y si después cambia su nombre a "Ana María", la Booking sigue diciendo "Ana"

Escenario: El Cliente consulta su historial de reservas
  Dado que el Cliente tiene reservas en distintos estados (pendiente, confirmada, completada,
    cancelada)
  Cuando abre "Mis reservas"
  Entonces las ve separadas o marcadas claramente por estado
  Y solo ve las suyas, en un orden determinista (ver notas)

Escenario: Un Cliente sin reservas ve su historial vacío
  Dado que el Cliente no ha hecho ninguna reserva
  Cuando abre "Mis reservas"
  Entonces ve una lista vacía, no un error
```
