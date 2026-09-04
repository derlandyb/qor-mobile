package br.com.qualorock.androidApp.di

import br.com.qualorock.androidApp.ui.viewmodel.EmailVerificationViewModel
import br.com.qualorock.androidApp.ui.viewmodel.EventDetailViewModel
import br.com.qualorock.androidApp.ui.viewmodel.ExploreViewModel
import br.com.qualorock.androidApp.ui.viewmodel.HomeFeedViewModel
import br.com.qualorock.androidApp.ui.viewmodel.LoginViewModel
import br.com.qualorock.androidApp.ui.viewmodel.PasswordRecoveryViewModel
import br.com.qualorock.androidApp.ui.viewmodel.ProfileViewModel
import br.com.qualorock.androidApp.ui.viewmodel.SignupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * A1 — Android-only half of the Koin graph: ViewModels, layered on top of `shared`'s
 * `di.sharedModule` (I1 — moved there so Android and iOS resolve the same repository/use-case
 * instances instead of each platform redeclaring the same bindings).
 */
val viewModelModule = module {
    viewModel { LoginViewModel(get()) }
    viewModel { SignupViewModel(get()) }
    viewModel { EmailVerificationViewModel(get()) }
    viewModel { PasswordRecoveryViewModel(get()) }
    viewModel { HomeFeedViewModel(get(), get()) }
    viewModel { ExploreViewModel(get(), get()) }
    viewModel { EventDetailViewModel(get()) }
    viewModel { ProfileViewModel(get(), get()) }
}
