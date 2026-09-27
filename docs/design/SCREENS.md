# Figma Screens — Ticket Mapping

Maps each `role:compose-ui`/`role:platform-integration` GitHub issue to its Figma screen(s), with
the structural notes confirmed during the full design review (all 28 screens checked, 2 gaps
found and fixed — see PR history / issue comments from that session). This exists so a
`booqi-compose-ui` ticket has a concrete visual reference without needing to browse Figma itself —
the relevant details are captured here.

Figma file: "Mobile Design System for Marketplace" (Figma Make). Screens are organized in three
sections — **Original (5)**, superseded, not a reference for new work — **Proveedor (P1–P11)** and
**Cliente (C1–C12, plus C10b)**.

## Proveedor

| Ticket | Figma screen(s) | Confirmed structure |
|---|---|---|
| #13 Proveedor · Perfil | P1 Activar modo, P2 Completar perfil, P3 Pausar perfil | P1: CTA + "¿Qué incluye?" list. P2: foto, nombre profesional, descripción, ubicación. P3: aviso explícito "Las citas ya confirmadas no se cancelan" + selector de rango Desde/Hasta por fecha. |
| #15 Proveedor · Servicios | P4 Lista servicios, P5 Agregar servicio | P4: toggle habilitar/deshabilitar por servicio, precio, duración, badge de modalidad (Local/Domicilio), botón Editar. P5: foto marcada **Requerido**, título, descripción, precio, duración, modalidad. |
| #17 Proveedor · Horario | P6 Horario semanal, P7 Bloquear fecha | P6: `WeeklyScheduleEditor` — toggle + rango horario por día de la semana. P7: `DateBlockingCalendar` — calendario mensual, día actual resaltado, fechas bloqueadas en tinte distinto. |
| #19 Proveedor · Reservas y Calificaciones | P8 Bandeja, P9 Detalle solicitud, P10 Cita confirmada, P11 Perfil público | P8: lista de solicitudes pendientes (cliente, servicio, fecha/hora, precio) con Aceptar/Rechazar. P9: mismo detalle + campo "Nota del cliente" visible. P10: botón de completar dice explícitamente "Disponible al finalizar la cita" (deshabilitado hasta que pase la hora) + "Cancelar cita". P11: vista previa de cómo el Cliente ve el perfil público (rating + reseñas). |

## Cliente

| Ticket | Figma screen(s) | Confirmed structure |
|---|---|---|
| #21 Cliente · Búsqueda y Descubrimiento | C1 Búsqueda, C2 Resultados, C3 Detalle servicio, C4 Perfil prov. | C1: input de texto + chips de categoría debajo (Todos/Barber/Uñas/Limpieza/Masajes/Técnico) + sección "Recientes". C2: filtro de distancia (1/2/5/10 km), contador de resultados. C3: título, precio, duración, badge de modalidad, descripción. C4: bio + ubicación + **sección "Servicios"** (varias tarjetas, cada una con foto/título/duración+modalidad/precio/"Reservar ›") + `RatingDisplay` (histograma + reseñas individuales) — la sección de Servicios fue uno de los 2 gaps corregidos durante la revisión, confirmar que sigue presente. |
| #23 Cliente · Dirección (Compose UI) | C6 Dirección | Mensaje contextual explicando por qué se pide (servicio a domicilio) + mapa con pin (`AddressPicker`). |
| #24 Cliente · Dirección (Platform Integration) | C6 Dirección | Mismo screen — el picker de mapa es lo que necesita el SDK nativo (ver el propio issue #24 sobre no construir esto especulativamente). |
| #26 Cliente · Reservas y Calificaciones | C5 Sel. horario, C7 Confirmar, C8 Solicitud pend., C9 Cita confirmada, C10 Rechazada, C10b Expirada, C11 Mis Reservas, C12 Calificar | C5: `TimeSlotGrid` con horarios no disponibles tachados. C7: resumen completo + campo "Nota para el proveedor (opcional)" + aviso "El proveedor tiene 24h para aceptar o rechazar" + botón "Enviar solicitud". C8: ícono de reloj + "Pendiente" + cuenta regresiva textual de 24h. C9: mensaje de confirmación + cuenta regresiva a la cita + "Cancelar cita". C10: `ReasonPicker` con el motivo mostrado + "Buscar otro horario"/"Volver al inicio". C10b: mensaje fijo de expiración automática + mismas opciones de reagendar. C11: tabs Todas/Confirmada/Pendiente/Completada, `StatusBadgeES`. C12: selector de estrellas + comentario opcional, solo para citas completadas. |

## Known state (as of the review)

Both gaps found during the full 28-screen review were fixed in Figma directly (not just noted):
C4 got its missing Servicios section, and the old "Original" section's English status badges were
corrected to Spanish. If a ticket's implementation doesn't match what's described above, the
design may have drifted since — flag it rather than silently building something different from
both this doc and the live Figma file.
