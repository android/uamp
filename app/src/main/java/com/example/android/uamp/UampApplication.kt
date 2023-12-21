package com.example.android.uamp

import android.app.Application
import com.example.android.uamp.utils.AppComponent
import com.example.android.uamp.utils.DaggerAppComponent
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

class UampApplication : Application(), HasAndroidInjector {

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun onCreate() {
        super.onCreate()
        DaggerAppComponent.builder()
            .application(this).build()
            .inject(this)
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
}