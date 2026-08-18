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
```

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

```gherkin
Escenario: El Cliente cancela una reserva confirmada a tiempo
  Dado que existe una Booking en estado "Confirmed" que empieza en más de 3 horas
  Cuando el Cliente la cancela eligiendo un motivo predefinido (u "Otro")
  Entonces la Booking pasa a estado "CancelledByCustomer"
  Y el TimeSlot vuelve a estar disponible para otros Clientes
  Y el Proveedor es notificado de la cancelación y el motivo

Escenario: El Cliente intenta cancelar fuera del plazo permitido
  Dado que existe una Booking en estado "Confirmed" que empieza en menos de 3 horas
  Cuando el Cliente intenta cancelarla
  Entonces el sistema rechaza la cancelación
  Y muestra un mensaje indicando que ya no se puede cancelar (fuera del plazo de 3 horas)

Escenario: El Cliente consulta su historial de reservas
  Dado que el Cliente tiene reservas en distintos estados (pendiente, confirmada, completada,
    cancelada)
  Cuando abre "Mis reservas"
  Entonces las ve separadas o marcadas claramente por estado
```
