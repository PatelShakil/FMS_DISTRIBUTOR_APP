package com.shakilpatel.boloindia.distributor.di

import android.content.Context
import com.shakilpatel.boloindia.distributor.data.local.PrefsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {


    @Provides
    fun provideContext(
        @ApplicationContext context: Context
    ): Context = context

    @Provides
    @Singleton
    fun providePrefsManager(
        @ApplicationContext context: Context
    ): PrefsManager = PrefsManager(context)
}
