# Provider Flow — Domain Discovery (DDD + BDD)

Defined via Event Storming before any Provider-side code exists, so the model is right the first
time. See `docs/DOMAIN.md` for the ubiquitous language this assumes (`User`, `ProviderProfile`,
`Service`, `Booking`, `TimeSlot`, `Availability`, `BookingStatus`).

## Event list (final)

1. Se activó el modo Proveedor
2. Se completó el perfil de Proveedor
3. Se agregó un Servicio
4. Se editó un Servicio
5. Se deshabilitó un Servicio *(y su inverso: se re-habilitó — mismo evento 5 en sentido contrario, añadido en #37)*
6. Se definió el horario semanal
7. Se modificó el horario semanal
8. Se bloqueó un día/hora específico
9. Se pausó el perfil temporalmente (modo vacaciones)
10. Se recibió una solicitud de reserva
11. El Proveedor aceptó / rechazó (con motivo) / la solicitud expiró sin respuesta (24h)
12. Se notificó al cliente correspondientemente
13. El Proveedor marcó una cita confirmada como completada
14. El Proveedor canceló una cita ya aceptada (con motivo)
15. Se recibió una calificación

*(Deferred to V2, not in this list: sugerir al Proveedor actualizar su horario tras un rechazo por
no-disponibilidad; que el Proveedor responda a una reseña.)*

---

## Grupo 1: Gestión de Perfil de Proveedor
*(eventos 1, 2, 9)*

| Evento | Comando | Actor | Agregado |
|---|---|---|---|
| Se activó el modo Proveedor | `ActivarModoProveedor` | Usuario | `User` → crea `ProviderProfile` |
| Se completó el perfil | `CompletarPerfilDeProveedor` | Proveedor | `ProviderProfile` |
| Se pausó el perfil | `PausarPerfil` | Proveedor | `ProviderProfile` |

```gherkin
Escenario: Un usuario activa el modo Proveedor
  Dado que un Usuario tiene una cuenta activa sin perfil de Proveedor
  Cuando el Usuario activa el modo Proveedor
  Entonces se crea un ProviderProfile vacío asociado a su cuenta
  Y el Usuario puede acceder a las pantallas de gestión de Proveedor

Escenario: El Proveedor completa su perfil
  Dado que el Proveedor activó el modo Proveedor pero no ha completado su perfil
  Cuando ingresa nombre, foto, descripción y ubicación
  Entonces el perfil se marca como completo
  Y queda listo para agregar Servicios

Escenario: El Proveedor intenta completar el perfil sin ubicación
  Dado que el Proveedor está completando su perfil
  Cuando intenta guardar sin especificar su ubicación
  Entonces el sistema rechaza el guardado
  Y muestra un error indicando que la ubicación es obligatoria

Escenario: El Proveedor pausa su perfil por un rango de fechas
  Dado que el Proveedor tiene un perfil activo
  Cuando selecciona pausar su perfil del 10 al 20 de agosto
  Entonces el perfil no aparece en las búsquedas de Clientes durante ese rango
  Y las citas ya aceptadas antes de la pausa no se cancelan automáticamente

Escenario: El Proveedor reactiva su perfil antes de tiempo
  Dado que el Proveedor tiene su perfil pausado
  Cuando decide reactivarlo manualmente
  Entonces el perfil vuelve a aparecer en las búsquedas inmediatamente
```

---

## Grupo 2: Gestión de Servicios
*(eventos 3, 4, 5)*

| Evento | Comando | Actor | Agregado |
|---|---|---|---|
| Se agregó un Servicio | `AgregarServicio` | Proveedor | `Service` (nuevo) |
| Se editó un Servicio | `EditarServicio` | Proveedor | `Service` |
| Se deshabilitó un Servicio | `DeshabilitarServicio` | Proveedor | `Service` |
| Se re-habilitó un Servicio | `HabilitarServicio` | Proveedor | `Service` |
| *(consulta, sin evento)* Ver mis Servicios | `ObtenerServiciosDelProveedor` | Proveedor | `Service` (lectura) |
| *(consulta, sin evento)* Ver un Servicio | `ObtenerServicio` | Proveedor | `Service` (lectura) |

Nota: solo existe "deshabilitar", no "eliminar" — ver `docs/DOMAIN.md` § Deliberate scope
decisions para el razonamiento (evitar romper `Booking.serviceId` de citas históricas).
`HabilitarServicio` es el inverso exacto de `DeshabilitarServicio` (solo cambia `isActive`).
Las dos consultas alimentan la lista de Servicios (Figma P4) y el formulario de edición (P5):
la lista del Proveedor incluye **todos** sus Servicios, también los deshabilitados, ordenados por
creación (el más antiguo primero) — a diferencia de la búsqueda de Clientes, que solo ve los
activos (ticket aparte, no implementado aquí).

```gherkin
Escenario: El Proveedor agrega un nuevo Servicio
  Dado que el Proveedor tiene un perfil completo
  Cuando agrega un Servicio con título, foto, descripción, precio, duración y modalidad
  Entonces el Servicio queda visible para los Clientes que busquen ese tipo

Escenario: El Proveedor agrega un Servicio sin foto
  Dado que el Proveedor está agregando un Servicio
  Cuando intenta guardar sin foto
  Entonces el sistema rechaza el guardado
  Y muestra un error indicando que la foto es obligatoria

Escenario: El Proveedor edita un Servicio existente
  Dado que el Proveedor tiene un Servicio publicado
  Cuando modifica su precio o duración
  Entonces los cambios aplican a partir de ese momento
  Y las citas ya reservadas conservan el precio/duración original acordado

Escenario: El Proveedor deshabilita un Servicio
  Dado que el Proveedor tiene un Servicio publicado
  Cuando lo deshabilita
  Entonces el Servicio deja de aparecer en las búsquedas de Clientes
  Y las citas ya aceptadas para ese Servicio no se cancelan
  Y el historial de citas pasadas conserva la referencia al Servicio

Escenario: El Proveedor ve todos sus Servicios, incluidos los deshabilitados
  Dado que el Proveedor tiene un Servicio activo y otro deshabilitado
  Y otro Proveedor tiene sus propios Servicios
  Cuando abre su lista de Servicios
  Entonces ve ambos Servicios, el activo y el deshabilitado, en orden de creación
  Y no ve los Servicios del otro Proveedor

Escenario: El Proveedor consulta un Servicio para editarlo
  Dado que el Proveedor tiene un Servicio, activo o deshabilitado
  Cuando abre ese Servicio en el formulario de edición
  Entonces ve todos sus datos actuales: título, foto, descripción, precio, duración y modalidad

Escenario: El Proveedor consulta o habilita un Servicio que no existe
  Dado que no existe ningún Servicio con el identificador indicado
  Cuando el Proveedor intenta abrirlo o habilitarlo
  Entonces el sistema responde que el Servicio no fue encontrado
  Y no se crea ni se modifica ningún Servicio

Escenario: El Proveedor re-habilita un Servicio deshabilitado
  Dado que el Proveedor tiene un Servicio deshabilitado
  Cuando lo habilita
  Entonces el Servicio vuelve a aparecer en las búsquedas de Clientes
  Y conserva sin cambios su título, foto, descripción, precio, duración y modalidad
  Y las citas ya existentes para ese Servicio no se ven afectadas

Escenario: El Proveedor habilita un Servicio que ya estaba activo
  Dado que el Proveedor tiene un Servicio activo
  Cuando lo habilita de nuevo
  Entonces el Servicio sigue activo y sin cambios
  Y no se produce ningún error
```

---

## Grupo 3: Gestión de Horario
*(eventos 6, 7, 8)*

| Evento | Comando | Actor | Agregado |
|---|---|---|---|
| Se definió el horario semanal | `DefinirHorarioSemanal` | Proveedor | `Availability` (por `providerId`) |
| Se modificó el horario semanal | `ModificarHorarioSemanal` | Proveedor | `Availability` |
| Se bloqueó un día/hora específico | `BloquearFechaHora` | Proveedor | `Availability` |
| Se desbloqueó un día/hora *(inverso de bloquear, añadido en #16)* | `DesbloquearFechaHora` | Proveedor | `Availability` |
| *(consulta, sin evento)* Ver mi horario y fechas bloqueadas | `ObtenerHorario` | Proveedor | `Availability` (lectura) |
| *(consulta, sin evento)* Calcular los TimeSlots reservables | `GenerarTimeSlots` | Cliente / sistema | `Availability` + duración del Servicio (cálculo puro) |

Notas de modelado (#16):

- `Availability` es un value object **separado** de `ProviderProfile`, referenciado solo por
  `providerId` (mismo principio que `Service`). Contiene el horario semanal (como máximo una franja
  por día de la semana, con interruptor activo/inactivo — Figma P6) y los periodos bloqueados (un
  día completo o un rango de horas dentro de un día — Figma P7). El **rango de pausa** (Grupo 1)
  sigue viviendo en `ProviderProfile.pausedRange`; la generación de TimeSlots lo recibe como
  entrada explícita en vez de duplicarlo.
- `DefinirHorarioSemanal` fija la semana completa (reemplaza lo que hubiera); `ModificarHorarioSemanal`
  cambia solo los días indicados de un horario **ya existente** y falla con "no encontrado" si el
  Proveedor aún no definió ninguno.
- Validación (ambos comandos y `BloquearFechaHora`), siempre antes de cualquier persistencia: la hora
  de fin de un día activo —o de un rango bloqueado— debe ser posterior a la de inicio, y un día de la
  semana no puede repetirse en un mismo horario. Un día inactivo conserva su rango sin validarse.
- Generación de TimeSlots: por cada fecha del rango pedido, solo los días **activos** y no pausados;
  los TimeSlots van consecutivos desde la hora de inicio del día mientras el slot **completo** quepa
  antes de la hora de fin; se omiten los que se solapan con un rango bloqueado (el solape es
  semiabierto: un slot que termina justo cuando empieza el bloqueo sigue disponible) y todo el día si
  está bloqueado completo.
- **Resuelto en #18 (`Booking`):** descontar los TimeSlots ocupados por una `Booking` pendiente o
  confirmada se hace en `ObtenerTimeSlotsDisponibles` (Grupo 4), no en `GenerarTimeSlots`, que sigue
  siendo un cálculo puro del horario. Una cita aceptada se sigue respetando aunque su hora quede
  después fuera del horario semanal: el descuento solo quita TimeSlots, nunca cancela citas.

```gherkin
Escenario: El Proveedor define su horario semanal
  Dado que el Proveedor tiene un perfil completo sin horario definido
  Cuando define sus horas disponibles para cada día de la semana
  Entonces los Clientes pueden ver y reservar dentro de esas horas

Escenario: El Proveedor modifica su horario semanal
  Dado que el Proveedor ya tiene un horario definido
  Cuando cambia sus horas disponibles de un día
  Entonces las citas ya aceptadas fuera del nuevo horario NO se cancelan automáticamente
  Y el nuevo horario aplica solo a futuras solicitudes

Escenario: El Proveedor bloquea un día específico
  Dado que el Proveedor tiene un horario semanal activo
  Cuando bloquea el 25 de diciembre
  Entonces ese día no aparece como disponible para nuevas reservas
  Y las citas ya aceptadas ese día no se ven afectadas

Escenario: El Proveedor bloquea solo un rango de horas de un día
  Dado que el Proveedor tiene un horario semanal activo
  Cuando bloquea de 12:00 a 14:00 de un día concreto
  Entonces los TimeSlots que se solapan con ese rango no aparecen como disponibles
  Y el resto de horas de ese día siguen disponibles

Escenario: El Proveedor desbloquea una fecha
  Dado que el Proveedor tiene una fecha o rango de horas bloqueado
  Cuando lo desbloquea
  Entonces ese día/rango vuelve a aparecer como disponible para nuevas reservas
  Y desbloquear algo que no estaba bloqueado no produce ningún error

Escenario: El Proveedor consulta su horario
  Dado que el Proveedor tiene un horario semanal y fechas bloqueadas
  Cuando abre el editor de horario o el calendario de bloqueo
  Entonces ve sus horas por día de la semana, en orden de lunes a domingo
  Y sus fechas bloqueadas en orden cronológico
  Y un Proveedor sin horario definido ve un horario vacío, no un error

Escenario: El Proveedor define o modifica un horario con horas inválidas
  Dado que el Proveedor está definiendo o modificando su horario semanal
  Cuando un día activo tiene una hora de fin igual o anterior a la de inicio
  O el mismo día de la semana aparece dos veces
  Entonces el sistema rechaza el guardado
  Y el horario guardado no cambia

Escenario: El Proveedor bloquea un rango de horas inválido
  Dado que el Proveedor está bloqueando un rango de horas
  Cuando la hora de fin es igual o anterior a la de inicio
  Entonces el sistema rechaza el bloqueo
  Y no se bloquea nada

Escenario: El Proveedor modifica un horario que aún no definió
  Dado que el Proveedor no tiene horario semanal definido
  Cuando intenta modificar las horas de un día
  Entonces el sistema responde que no hay horario que modificar
  Y no se guarda ningún horario

Escenario: Se generan los TimeSlots según el horario y la duración del Servicio
  Dado que el Proveedor trabaja los lunes de 09:00 a 12:00
  Y un Servicio dura 60 minutos
  Cuando se generan los TimeSlots de un lunes
  Entonces hay TimeSlots a las 09:00, 10:00 y 11:00
  Y un tramo final más corto que la duración del Servicio no genera TimeSlot

Escenario: Los días inactivos y los días sin horario no generan TimeSlots
  Dado que el Proveedor tiene el miércoles desactivado y no definió el domingo
  Cuando se generan los TimeSlots de un rango de fechas
  Entonces no hay ningún TimeSlot en miércoles ni en domingo

Escenario: Un perfil pausado no genera TimeSlots durante la pausa
  Dado que el Proveedor pausó su perfil del 10 al 20 de agosto
  Cuando se generan los TimeSlots de un rango que incluye esas fechas
  Entonces no hay TimeSlots del 10 al 20 de agosto, ambos incluidos
  Y los días fuera de la pausa generan TimeSlots con normalidad
```

---

## Grupo 4: Gestión de Reservas y Calificaciones
*(eventos 10-15)*

| Evento | Comando | Actor | Agregado |
|---|---|---|---|
| Se recibió una solicitud de reserva | `SolicitarReserva` | Cliente | `Booking` (nuevo) |
| El Proveedor aceptó | `AceptarReserva` | Proveedor | `Booking` |
| El Proveedor rechazó (con motivo) | `RechazarReserva` | Proveedor | `Booking` |
| Expiró sin respuesta (24h) | `ExpirarSolicitud` | Sistema (job automático) | `Booking` |
| Se marcó como completada | `CompletarCita` | Proveedor (manual, no automático) | `Booking` |
| Se canceló una cita aceptada (con motivo) | `CancelarReservaAceptada` | Proveedor | `Booking` |
| Se recibió una calificación | `CalificarCita` | Cliente | `Booking` |
| *(efecto de calificar)* Se recalculó el promedio del Proveedor | `RecalcularCalificacionDelProveedor` | Sistema (lo dispara `CalificarCita`) | `ProviderProfile` (resumen calculado desde las `Booking` calificadas) |
| *(consulta, sin evento)* Calcular los TimeSlots realmente disponibles | `ObtenerTimeSlotsDisponibles` | Cliente / sistema | `Availability` + `ProviderProfile.pausedRange` + `Booking`s activas (lectura) |
| *(consulta, sin evento)* Bandeja de solicitudes pendientes (Figma P8) | `ObtenerSolicitudesPendientes` | Proveedor | `Booking` (lectura) |
| *(consulta, sin evento)* Detalle de una reserva (P9, P10) | `ObtenerReserva` | Proveedor / Cliente | `Booking` (lectura) |
| *(consulta, sin evento)* Agenda del Proveedor por estado (P10) | `ObtenerReservasDelProveedor` | Proveedor | `Booking` (lectura) |
| *(consulta, sin evento)* Reseñas del Proveedor (P11) | `ObtenerCalificacionesDelProveedor` | Proveedor / Cliente | `Booking` (lectura) |

Motivos predefinidos (rechazo y cancelación — mismo listado, ver nota de asunción en
`docs/DOMAIN.md`): *"No disponible en este horario"*, *"Fuera de mi zona de servicio"*,
*"Servicio no disponible temporalmente"*, *"Otro"* (+ texto libre opcional).

Notas de modelado (#18):

- **Un solo state machine.** `BookingStatus` contiene la única tabla de transiciones permitidas
  (`Requested → Confirmed | Rejected | Expired`, `Confirmed → Completed | CancelledByProvider |
  CancelledByCustomer`; el resto son estados terminales). Todo cambio de estado pasa por las
  funciones de transición de `Booking` (`confirm`, `reject`, `expire`, `complete`,
  `cancelByProvider`), que devuelven `DomainError.InvalidInput` ante una transición inválida —
  nunca se permite ni se ignora en silencio. Los casos de uso solo cargan, transicionan y guardan.
  La transición `CancelledByCustomer` está en la tabla, pero su función y la regla de 3 horas son de
  #25.
- **Plazo de 24 horas.** Una solicitud vence cuando `requestedAt + 24 h <= ahora` (el instante
  exacto ya cuenta como vencida). Aceptar o rechazar una solicitud vencida se rechaza aunque el
  barrido aún no la haya pasado a `Expired`; `Expired` solo se alcanza una vez vencido el plazo. Los
  casos de uso que dependen de "ahora" reciben un `Clock` (por defecto `Clock.System`).
- **Motivos.** Un `Reason` es un código predefinido + nota opcional. El Proveedor usa
  `ProviderReasonCode` (los tres motivos + "Otro"); la lista del Cliente (#25) será otro enum
  (`CustomerReasonCode`) bajo la misma interfaz sellada `ReasonCode`. El texto libre es opcional y
  una nota en blanco se guarda como ausente.
- **Snapshots.** `Booking` guarda por ID `providerId`/`serviceId`/`customerId` y copia, al solicitar,
  el precio, la duración y (solo si el Servicio lo requiere) la dirección de entrega
  (`Address`: línea + lat/lng). Un Servicio `Domicilio` exige dirección; uno `Local` la descarta;
  `Ambos` la guarda si viene.
- **Hora de la cita.** `Booking.scheduledAt` es un `LocalDateTime` (fecha + hora de inicio locales
  del Proveedor, igual que `TimeSlot`); aún no hay zonas horarias. Los instantes reales
  (`requestedAt`, `respondedAt`, `completedAt`) son `Instant`.
- **Calificación.** Se guarda en la `Booking` (`Rating`: 1..5 estrellas + comentario opcional); el
  promedio y el conteo del Proveedor se recalculan desde *todas* sus `Booking` calificadas y se
  guardan en `ProviderProfile` (calculado, no incremental).
- **Diferido (no hay sistema de notificaciones):** todo "el Cliente/Proveedor es notificado" y el
  mensaje fijo de expiración quedan fuera de #18. Tampoco se valida que el TimeSlot solicitado no
  esté en el pasado (sin zona horaria no se puede comparar con "ahora"), ni que "Completar" solo
  esté disponible tras la hora de la cita (afordancia de UI, Figma P10).
  Esa afordancia ya está implementada en la UI (#19): el botón de completar queda deshabilitado, con el
  texto "Disponible al finalizar la cita", hasta que `scheduledAt` + `durationMinutesSnapshot` (leído en
  la zona horaria del dispositivo) ya no esté en el futuro según un `Clock` inyectado.
- **Fuera de alcance, para #27:** evitar la doble reserva ante dos solicitudes simultáneas requiere
  una restricción en la base de datos; el caso de uso solo verifica antes de insertar.

```gherkin
Escenario: Un Cliente solicita una reserva
  Dado que un Servicio tiene un TimeSlot disponible
  Cuando el Cliente solicita reservar ese TimeSlot
  Entonces se crea una Booking con estado "Requested"
  Y el TimeSlot deja de estar disponible para otros Clientes mientras la solicitud está pendiente
  Y el Proveedor tiene 24 horas para responder

Escenario: El Proveedor acepta una solicitud
  Dado que existe una Booking en estado "Requested"
  Cuando el Proveedor la acepta
  Entonces la Booking pasa a estado "Confirmed"
  Y el Cliente es notificado de la confirmación

Escenario: El Proveedor rechaza una solicitud
  Dado que existe una Booking en estado "Requested"
  Cuando el Proveedor la rechaza eligiendo un motivo predefinido (u "Otro" con texto libre)
  Entonces la Booking pasa a estado "Rejected"
  Y el TimeSlot vuelve a estar disponible para otros Clientes
  Y el Cliente recibe el motivo + mensaje, con opción de reagendar con el mismo Proveedor u otro

Escenario: Una solicitud expira sin respuesta
  Dado que existe una Booking en estado "Requested" por más de 24 horas
  Cuando el sistema detecta que venció el plazo sin respuesta del Proveedor
  Entonces la Booking pasa a estado "Expired" automáticamente
  Y el TimeSlot vuelve a estar disponible
  Y el Cliente recibe el mensaje fijo del sistema, con opción de reagendar con el mismo Proveedor u otro

Escenario: El Proveedor marca una cita confirmada como completada
  Dado que existe una Booking en estado "Confirmed"
  Cuando el Proveedor la marca manualmente como completada
  Entonces la Booking pasa a estado "Completed"
  Y el Cliente puede dejar una calificación

Escenario: El Proveedor cancela una cita ya confirmada
  Dado que existe una Booking en estado "Confirmed"
  Cuando el Proveedor la cancela eligiendo un motivo predefinido (u "Otro")
  Entonces la Booking pasa a estado "CancelledByProvider"
  Y el Cliente recibe el motivo + mensaje, con opción de reagendar

Escenario: El Cliente califica una cita completada
  Dado que existe una Booking en estado "Completed" sin calificación
  Cuando el Cliente deja una calificación (estrellas + comentario opcional)
  Entonces la calificación queda asociada a esa Booking
  Y se recalcula el promedio general de calificación del Proveedor (visible en su perfil, junto
    con los comentarios individuales)

Escenario: El Cliente intenta calificar una cita no completada
  Dado que existe una Booking en estado "Requested" o "Confirmed"
  Cuando el Cliente intenta dejar una calificación
  Entonces el sistema rechaza la acción
  Y muestra un mensaje indicando que solo se puede calificar después de completada

Escenario: Una solicitud solo puede cambiar de estado por las transiciones permitidas
  Dado que existe una Booking en un estado que no es el de partida de la acción
    (por ejemplo "Confirmed" y el Proveedor intenta aceptarla, "Requested" y intenta completarla,
    o "Completed", "Rejected", "Expired" o cualquier cancelada y intenta cualquier acción)
  Cuando el Proveedor intenta aceptar, rechazar, completar o cancelar la Booking
  Entonces el sistema rechaza la acción con un error de validación
  Y la Booking conserva su estado y no se guarda nada

Escenario: El Proveedor intenta responder una solicitud vencida
  Dado que existe una Booking en estado "Requested" cuyo plazo de 24 horas ya venció
  Cuando el Proveedor intenta aceptarla o rechazarla
  Entonces el sistema rechaza la acción indicando que la solicitud venció
  Y la Booking solo puede pasar a "Expired"

Escenario: Una solicitud todavía dentro de su plazo no expira
  Dado que existe una Booking en estado "Requested" con menos de 24 horas
  Cuando el sistema revisa las solicitudes vencidas
  Entonces la Booking sigue en "Requested"
  Y a partir de las 24 horas exactas la Booking pasa a "Expired"
  Y revisar de nuevo no expira nada más (la revisión es idempotente)

Escenario: Un Cliente solicita un TimeSlot que ya no está disponible
  Dado que otra Booking en estado "Requested" o "Confirmed" ocupa ese TimeSlot
  Cuando el Cliente solicita reservarlo
  Entonces el sistema rechaza la solicitud indicando que el horario ya no está disponible
  Y no se crea ninguna Booking

Escenario: Los TimeSlots disponibles excluyen las reservas activas
  Dado que el Proveedor tiene TimeSlots según su horario y una Booking "Requested" o "Confirmed"
  Cuando se consultan los TimeSlots disponibles para un Servicio
  Entonces no aparece ningún TimeSlot que se solape con esa Booking (por intervalo de tiempo,
    aunque sea de otro Servicio con distinta duración)
  Y los TimeSlots que solo colindan con ella siguen disponibles
  Y las Booking "Rejected", "Expired", "CancelledByProvider", "CancelledByCustomer" y "Completed"
    no ocupan ningún TimeSlot
  Y una cita aceptada fuera del horario semanal actual no produce ningún error

Escenario: La reserva conserva lo acordado aunque el Servicio cambie
  Dado que existe una Booking creada para un Servicio de cierto precio y duración
  Cuando el Proveedor edita o deshabilita ese Servicio
  Entonces la Booking sigue mostrando el precio y la duración con que se solicitó

Escenario: Una reserva a domicilio exige dirección
  Dado que el Servicio es de modalidad "Domicilio"
  Cuando el Cliente solicita reservar sin dirección de entrega
  Entonces el sistema rechaza la solicitud y no se crea ninguna Booking
  Y con dirección, la Booking guarda una copia de ella (no una referencia a la dirección del Cliente)
  Y en un Servicio "Local" la dirección se descarta

Escenario: Un Cliente solicita un Servicio no reservable
  Dado que el Servicio no existe, está deshabilitado, o el TimeSlot es de otro Proveedor
  Cuando el Cliente intenta solicitar la reserva
  Entonces el sistema rechaza la solicitud y no se crea ninguna Booking

Escenario: El Cliente intenta calificar una cita ya calificada
  Dado que existe una Booking en estado "Completed" que ya tiene calificación
  Cuando el Cliente intenta calificarla otra vez
  Entonces el sistema rechaza la acción
  Y la calificación original no cambia

Escenario: El Cliente deja una calificación fuera del rango permitido
  Dado que existe una Booking en estado "Completed" sin calificación
  Cuando el Cliente elige menos de 1 o más de 5 estrellas
  Entonces el sistema rechaza la calificación y no se guarda nada

Escenario: El promedio del Proveedor se recalcula desde todas sus calificaciones
  Dado que un Proveedor tiene citas completadas con 5, 4 y 3 estrellas
  Cuando el Cliente califica otra cita con 2 estrellas
  Entonces el promedio del Proveedor es 3.5 y su conteo es 4
  Y el promedio se obtiene de todas sus Booking calificadas, no sumando a un valor anterior

Escenario: El Proveedor consulta su bandeja de solicitudes pendientes
  Dado que el Proveedor tiene solicitudes en distintos estados
  Cuando abre la bandeja
  Entonces ve solo las "Requested" que aún puede responder, la más antigua primero
  Y una solicitud vencida pero aún no marcada "Expired" no aparece
  Y un Proveedor sin solicitudes ve una lista vacía, no un error

Escenario: El Proveedor consulta el detalle y su agenda de reservas
  Dado que el Proveedor tiene reservas en distintos estados
  Cuando abre el detalle de una reserva
  Entonces ve la Booking con su precio, duración, dirección, nota del Cliente, motivo y calificación
  Y una reserva inexistente responde "no encontrado"
  Cuando consulta su agenda filtrando por estado (por ejemplo "Confirmed")
  Entonces ve solo las reservas de esos estados, la más próxima primero

Escenario: El Proveedor consulta sus calificaciones
  Dado que el Proveedor tiene citas completadas, algunas con calificación
  Cuando abre su perfil público
  Entonces ve las reseñas (estrellas + comentario), la más reciente primero
  Y un Proveedor sin calificaciones ve una lista vacía, no un error
```
