# Provider Flow — Domain Discovery (DDD + BDD)

Defined via Event Storming before any Provider-side code exists, so the model is right the first
time. See `docs/DOMAIN.md` for the ubiquitous language this assumes (`User`, `ProviderProfile`,
`Service`, `Booking`, `TimeSlot`, `Availability`, `BookingStatus`).

## Event list (final)

1. Se activó el modo Proveedor
2. Se completó el perfil de Proveedor
3. Se agregó un Servicio
4. Se editó un Servicio
5. Se deshabilitó un Servicio
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

Nota: solo existe "deshabilitar", no "eliminar" — ver `docs/DOMAIN.md` § Deliberate scope
decisions para el razonamiento (evitar romper `Booking.serviceId` de citas históricas).

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
```

---

## Grupo 3: Gestión de Horario
*(eventos 6, 7, 8)*

| Evento | Comando | Actor | Agregado |
|---|---|---|---|
| Se definió el horario semanal | `DefinirHorarioSemanal` | Proveedor | `ProviderProfile` |
| Se modificó el horario semanal | `ModificarHorarioSemanal` | Proveedor | `ProviderProfile` |
| Se bloqueó un día/hora específico | `BloquearFechaHora` | Proveedor | `ProviderProfile` |

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

Motivos predefinidos (rechazo y cancelación — mismo listado, ver nota de asunción en
`docs/DOMAIN.md`): *"No disponible en este horario"*, *"Fuera de mi zona de servicio"*,
*"Servicio no disponible temporalmente"*, *"Otro"* (+ texto libre opcional).

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
```
