# Identity Flow — Domain Discovery (DDD + BDD)

Defined via Event Storming before any Identity code exists (issue #50), same method as
`provider-flow.md` and `customer-flow.md`. Decisions below were confirmed with the product owner on
2026-10-03. See `docs/DOMAIN.md` for `User` / `ProviderProfile`.

## Decisions (recorded so they don't get silently re-litigated)

- **Exploring without an account is allowed.** Search, service detail and provider profiles are
  public. An account is required only to **request a booking** or to **activate Provider mode**.
- **Sign-in methods:** Google, Facebook, Apple ID, email + password (already confirmed in
  `docs/DATABASE.md`; Supabase Auth).
- **Registration data:** display name (required — this replaces the "Cliente" placeholder shown to
  Providers) + optional photo. Social providers usually prefill both. No phone number in V1.
- **Email verification:** email/password accounts can sign in immediately, but must verify their
  email before requesting a booking or activating Provider mode. Social sign-ins count as verified.
- **Account deletion is in V1** (App Store requires in-app account deletion when accounts can be
  created):
  - **Blocked while the user has active bookings** (`Requested` or `Confirmed`, as Customer *or* as
    Provider). The app lists which ones must be cancelled or completed first.
  - **History is anonymized, not deleted:** name, photo and personal data are erased; completed
    bookings and reviews stay, shown as "Usuario eliminado", so the other party's history and the
    Provider's rating aggregate don't change.
- **`providerId` contract** (open in #50): **settled in #56** — `providerId == ProviderProfile.id`
  (see `docs/DOMAIN.md`); the Provider screens' user-id placeholders are changed by the UI
  sub-ticket.

## Event list

1. Se registró un Usuario
2. Se inició sesión
3. Se verificó el correo
4. Se actualizó el perfil de usuario (nombre, foto)
5. Se cerró sesión
6. Se solicitó eliminar la cuenta
7. Se eliminó (anonimizó) la cuenta

| Evento | Comando | Actor | Agregado |
|---|---|---|---|
| Se registró un Usuario | `RegistrarUsuario` | Visitante | `User` (nuevo) |
| Se inició sesión | `IniciarSesion` | Visitante | `User` (sesión) |
| Se verificó el correo | `VerificarCorreo` | Usuario | `User` |
| Se actualizó el perfil | `ActualizarPerfilDeUsuario` | Usuario | `User` |
| Se cerró sesión | `CerrarSesion` | Usuario | `User` (sesión) |
| Se eliminó la cuenta | `EliminarCuenta` | Usuario | `User` (+ anonimiza `Booking`/`Rating` históricos, `ProviderProfile`) |

Queries: `ObtenerUsuarioActual` (null when browsing without an account).

```gherkin
Escenario: Un visitante explora sin cuenta
  Dado que un visitante abre la app sin haber iniciado sesión
  Cuando busca servicios y abre el detalle de un Servicio o el perfil de un Proveedor
  Entonces puede ver todo el contenido público
  Y no se le pide iniciar sesión

Escenario: Un visitante intenta reservar sin cuenta
  Dado que un visitante sin sesión eligió un TimeSlot
  Cuando intenta solicitar la reserva
  Entonces se le pide registrarse o iniciar sesión
  Y al terminar vuelve a la reserva que estaba haciendo, con el TimeSlot elegido

Escenario: Un visitante se registra con Google, Facebook o Apple
  Dado que un visitante elige registrarse con un proveedor social
  Cuando autoriza el acceso
  Entonces se crea su cuenta con el nombre y la foto que trae el proveedor
  Y su correo se considera verificado

Escenario: Un visitante se registra con correo y contraseña
  Dado que un visitante elige registrarse con correo
  Cuando ingresa nombre, correo y contraseña
  Entonces se crea su cuenta y queda con la sesión iniciada
  Y se le envía un correo de verificación

Escenario: El registro sin nombre se rechaza
  Dado que un visitante está registrándose con correo
  Cuando intenta crear la cuenta sin nombre
  Entonces el sistema rechaza el registro
  Y muestra un error indicando que el nombre es obligatorio

Escenario: Un Usuario con correo sin verificar intenta reservar
  Dado que un Usuario se registró con correo y no verificó su correo
  Cuando intenta solicitar una reserva o activar el modo Proveedor
  Entonces el sistema rechaza la acción
  Y le indica que debe verificar su correo, con opción de reenviar el correo de verificación

Escenario: El Usuario actualiza su perfil
  Dado que un Usuario tiene la sesión iniciada
  Cuando cambia su nombre o su foto
  Entonces los cambios se ven en sus próximas reservas y reseñas
  Y las reservas ya hechas no cambian el nombre que se guardó en ellas

Escenario: El Usuario cierra sesión
  Dado que un Usuario tiene la sesión iniciada
  Cuando cierra sesión
  Entonces vuelve a navegar como visitante
  Y no puede reservar hasta iniciar sesión de nuevo

Escenario: El Usuario intenta eliminar su cuenta con reservas activas
  Dado que un Usuario tiene reservas en estado "Requested" o "Confirmed", como Cliente o como Proveedor
  Cuando solicita eliminar su cuenta
  Entonces el sistema rechaza la eliminación
  Y le muestra qué reservas debe cancelar o completar primero

Escenario: El Usuario elimina su cuenta sin reservas activas
  Dado que un Usuario no tiene reservas "Requested" ni "Confirmed"
  Cuando confirma que quiere eliminar su cuenta
  Entonces se borran su nombre, foto y datos personales, y se cierra la sesión
  Y sus reservas completadas y reseñas se conservan como "Usuario eliminado"
  Y el rating de los Proveedores que calificó no cambia
  Y si tenía perfil de Proveedor, deja de aparecer en las búsquedas
```

## Implementation notes (#56, domain & data)

What implementing revealed, so the decisions above stay honest:

- **Deletion carries data when refused**, so `EliminarCuenta` returns `AccountDeletionResult`
  (`Deleted` / `Blocked(activeBookings by role)`) instead of a `DomainError`; a `Failure` means
  something actually broke.
- **"Personal data" on Bookings:** besides the name/photo (which live on the `User`), the Customer's
  delivery-address snapshot and free-text note on their Bookings, and their saved address (#22,
  `User.address`), are erased on deletion; status and
  rating stay.
- **Hiding the Provider** reuses the Catalog's existing visibility gate (`isComplete = false` plus
  erasing the profile's personal data) instead of a new "deleted" flag on `ProviderProfile`; the
  rating summary stays.
- **The access guard** (`RequerirCuentaVerificada`) answers `Unauthorized` (no session) or
  `InvalidInput` with a fixed message (unverified email). A dedicated `DomainError` variant was
  considered and left out because it breaks the exhaustive `when (error)` in `feature:*`.

## Implementation notes (#58, UI)

- **Sign-in is pushed, not a tab.** `Auth` (sign in and sign up share one destination) and
  `VerifyEmail` are pushed over the screen that needed an account, so "volver a donde estaba" is a
  plain back. After "Reservar" the visitor lands on the same Service/Provider page; the tap itself is
  not replayed (the booking flow is #26 and will decide whether to resume with the chosen TimeSlot).
- **Verification flow:** a new email account goes sign-up → verification notice → back. Signing in
  with an unverified account does not push the notice; the guard does it at the first "Reservar" /
  "Convertirme en Proveedor" instead, and the profile shows a "Verificar correo" card.
- **Deletion is confirm-then-check:** the use case has no dry run (it deletes or refuses), so the
  dialog asks for confirmation first and, if refused, replaces itself with the blocking Bookings.
- **Validation messages are field-less** (`InvalidInput(message)`), so the forms attach each to its
  field by the subject the use cases put first in the text (`fieldOf`); an unknown message falls back
  to the form level.
- **Photo** is a link in a text field for now; picking one from the device needs the platform
  pickers and an image loader (Platform Integration). Social buttons run against the fake.

## Open questions (resolve when implementing, not now)

- Whether a deleted Provider's still-public reviews should keep showing on their (now hidden)
  profile — moot while the profile is hidden; revisit if profiles become reachable by link.

## Resolved

- **Customer-name snapshot on `Booking` (resolved in #25).** Yes: `Booking.customerNameSnapshot`
  (`bookings.customer_name_snapshot`, nullable) is copied from the signed-in user's display name by
  `SolicitarReserva` when the request is made, so a rename later does not change it (scenario in
  `customer-flow.md` § Grupo 3). It is `null` for Bookings that predate it, and — as personal data —
  it is erased on account deletion together with the address and note (`EliminarCuenta`; the
  Booking and its rating stay). The Provider's inbox still shows the name resolved through
  `ObtenerUsuarioPublico`; it can switch to the snapshot (with "Usuario eliminado" for a `null` one)
  in a UI follow-up.
- **`SolicitarReserva` applies the access guard itself (#25).** It resolves the user through
  `RequerirCuentaVerificada` (sin sesión → `Unauthorized`, correo sin verificar → `InvalidInput`)
  and requires `customerId` to be that user.
