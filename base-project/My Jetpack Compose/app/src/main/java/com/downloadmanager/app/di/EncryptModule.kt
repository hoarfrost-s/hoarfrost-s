package com.downloadmanager.app.di

import com.downloadmanager.app.encrypt.AESFileEncryptor
import com.downloadmanager.app.encrypt.FileEncryptor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EncryptModule {

    @Binds
    @Singleton
    abstract fun bindFileEncryptor(impl: AESFileEncryptor): FileEncryptor
}
