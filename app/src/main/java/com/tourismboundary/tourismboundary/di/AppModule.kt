package com.tourismboundary.tourismboundary.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(includes = [ViewModelModule::class])
class AppModule {
    @Provides
    @Singleton
    fun provideApplicationContext(application: Application): Context = application.applicationContext
}
