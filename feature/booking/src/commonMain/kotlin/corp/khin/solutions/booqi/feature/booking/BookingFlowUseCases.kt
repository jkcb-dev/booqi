package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.domain.usecase.GenerarTimeSlotsUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerHorarioUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerMiDireccionUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerTimeSlotsDisponiblesUseCase
import corp.khin.solutions.booqi.domain.usecase.RequerirCuentaVerificadaUseCase
import corp.khin.solutions.booqi.domain.usecase.VerDetalleServicioUseCase

/**
 * The read side of [BookingFlowViewModel]'s C5 step, grouped into one parameter object so the
 * constructor stays under detekt's `LongParameterList` limit (a plain holder, not a layer).
 * [schedule] + [generate] give every slot the Provider's schedule produces for a day, and
 * [available] the ones still free — the difference is what the grid strikes through.
 */
class BookingSlotQueries(
    val detail: VerDetalleServicioUseCase,
    val schedule: ObtenerHorarioUseCase,
    val generate: GenerarTimeSlotsUseCase,
    val available: ObtenerTimeSlotsDisponiblesUseCase,
)

/**
 * What [BookingFlowViewModel] asks about the signed-in Customer at C7: the access guard that
 * `SolicitarReserva` also applies (so the UI can tell "sign in" from "verify your email" before
 * sending) and the saved address used as the Domicilio snapshot.
 */
class BookingCustomerAccess(
    val requireVerifiedAccount: RequerirCuentaVerificadaUseCase,
    val savedAddress: ObtenerMiDireccionUseCase,
)
