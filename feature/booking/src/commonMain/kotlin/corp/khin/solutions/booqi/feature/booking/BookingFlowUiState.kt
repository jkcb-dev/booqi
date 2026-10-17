package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.ServiceDetail
import corp.khin.solutions.booqi.domain.model.ServiceModality
import corp.khin.solutions.booqi.domain.model.TimeSlot
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** The two steps of the request flow: pick a TimeSlot (C5), then confirm and send (C7). */
enum class BookingStep { SELECT_SLOT, CONFIRM }

/** One cell of the C5 grid: a slot the schedule produces, and whether it can still be taken. */
data class SlotCell(val slot: TimeSlot, val isAvailable: Boolean)

/** What the saved-address lookup (`ObtenerMiDireccion`) answered at C7. */
enum class AddressLookup {
    /** Not asked yet, or in flight. */
    PENDING,
    FOUND,

    /** Signed in without a saved address: a Domicilio request is blocked until one is added. */
    MISSING,

    /** No session: the address cannot be read; sending asks the visitor to sign in. */
    SIGNED_OUT,
    FAILED,
}

/** Why the Identity guard refused the request. */
enum class AccessIssue { SIGN_IN_REQUIRED, EMAIL_NOT_VERIFIED }

/**
 * Immutable state of the request flow (C5 + C7). [serviceId] is the Service this state was
 * loaded *for*: the ViewModel instance is reused across visits, so [BookingFlowScreen] only trusts
 * the state when it matches (never a flash of the previous Service's flow). [today] and [date] are
 * the device's calendar days, supplied by the ViewModel's injected clock.
 */
data class BookingFlowUiState(
    val today: LocalDate,
    val date: LocalDate,
    val serviceId: String? = null,
    val step: BookingStep = BookingStep.SELECT_SLOT,
    val isLoading: Boolean = true,
    val detail: ServiceDetail? = null,
    val loadError: DomainError? = null,
    val isLoadingSlots: Boolean = false,
    val slotCells: List<SlotCell> = emptyList(),
    val slotsError: DomainError? = null,
    val selectedSlot: TimeSlot? = null,
    val atHome: Boolean = false,
    val note: String = "",
    val savedAddress: Address? = null,
    val addressLookup: AddressLookup = AddressLookup.PENDING,
    val access: AccessIssue? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
) {
    private val modality: ServiceModality? get() = detail?.service?.modality

    /** Domicilio always needs the address; "Local & Dom." only when the Customer chose delivery. */
    val wantsDelivery: Boolean
        get() = modality == ServiceModality.DOMICILIO || (modality == ServiceModality.AMBOS && atHome)

    /** A Domicilio request is blocked while the signed-in Customer has no saved address. */
    val isAddressBlocking: Boolean
        get() = wantsDelivery && addressLookup in BLOCKING_LOOKUPS

    val canGoToPreviousDay: Boolean get() = date > today

    val canGoToNextDay: Boolean get() = date < today.plus(BOOKING_HORIZON_DAYS, DateTimeUnit.DAY)

    val canSubmit: Boolean
        get() = selectedSlot != null && !isSubmitting && !isAddressBlocking

    companion object {
        /** How many days ahead a Customer can browse for a slot. */
        const val BOOKING_HORIZON_DAYS = 60

        private val BLOCKING_LOOKUPS = setOf(AddressLookup.MISSING, AddressLookup.PENDING, AddressLookup.FAILED)

        fun initial(today: LocalDate, serviceId: String? = null) =
            BookingFlowUiState(today = today, date = today, serviceId = serviceId)
    }
}
