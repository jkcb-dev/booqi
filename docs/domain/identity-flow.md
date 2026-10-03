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
- **`providerId` contract** (open in #50): resolved when this flow is implemented — the domain docs
  say `providerId == ProviderProfile.id`; the current placeholders use the user id.

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

## Open questions (resolve when implementing, not now)

- Whether a deleted Provider's still-public reviews should keep showing on their (now hidden)
  profile — moot while the profile is hidden; revisit if profiles become reachable by link.
- The "name saved in the booking" in the update-profile scenario implies a **customer name
  snapshot on `Booking`** (consistent with the snapshot rule in `DOMAIN.md`) — confirm when the
  Customer booking flow (#25) is built.
