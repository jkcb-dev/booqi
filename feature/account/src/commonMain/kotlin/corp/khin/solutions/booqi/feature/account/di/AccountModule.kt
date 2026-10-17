package corp.khin.solutions.booqi.feature.account.di

import corp.khin.solutions.booqi.feature.account.DeleteAccountViewModel
import corp.khin.solutions.booqi.feature.account.ProfileQueries
import corp.khin.solutions.booqi.feature.account.ProfileViewModel
import corp.khin.solutions.booqi.feature.account.SignInViewModel
import corp.khin.solutions.booqi.feature.account.SignUpViewModel
import corp.khin.solutions.booqi.feature.account.VerifyEmailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Registered in `shared/.../di/InitKoin.kt`. Every ViewModel takes only domain use cases, so the
 * module needs nothing beyond `domainModule`.
 */
val accountModule = module {
    viewModel { SignInViewModel(iniciarSesion = get()) }
    viewModel { SignUpViewModel(registrarUsuario = get()) }
    viewModel {
        VerifyEmailViewModel(
            obtenerUsuarioActual = get(),
            verificarCorreo = get(),
            reenviarCorreo = get(),
        )
    }
    viewModel {
        ProfileViewModel(
            queries = ProfileQueries(currentUser = get(), myProviderProfile = get()),
            actualizarPerfil = get(),
            cerrarSesion = get(),
            requerirCuentaVerificada = get(),
        )
    }
    viewModel { DeleteAccountViewModel(eliminarCuenta = get(), obtenerServicio = get()) }
}
