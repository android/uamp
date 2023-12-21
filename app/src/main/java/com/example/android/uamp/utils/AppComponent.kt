package com.example.android.uamp.utils

import com.example.android.uamp.UampApplication
import com.example.android.uamp.viewmodels.MediaItemFragmentViewModel
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import javax.inject.Singleton


@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        InjectorUtils::class,
        ViewModelModule::class,
        MainActivityModule::class
    ]
)
interface AppComponent : AndroidInjector<UampApplication> {

    @Component.Builder
    interface Builder {
        fun build(): AppComponent

        @BindsInstance
        fun application(application: UampApplication): Builder
    }

    override fun inject(instance: UampApplication)

    fun mediaItemFragmentViewModelFactory(): MediaItemFragmentViewModel.Factory
}