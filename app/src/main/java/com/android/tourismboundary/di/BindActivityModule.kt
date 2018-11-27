package com.android.tourismboundary.di

import com.android.tourismboundary.ui.MainActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class BindActivityModule {
    @ContributesAndroidInjector()
    abstract fun contributeMainActivity(): MainActivity
}
