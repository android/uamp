package com.example.android.uamp.utils

import com.example.android.uamp.fragments.MediaItemFragment
import com.example.android.uamp.fragments.NowPlayingFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class FragmentBuildersModule {
    @ContributesAndroidInjector
    abstract fun mediaItemFragment(): MediaItemFragment

    @ContributesAndroidInjector
    abstract fun nowPlayingFragment(): NowPlayingFragment
}