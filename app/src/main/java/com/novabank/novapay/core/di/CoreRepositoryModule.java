package com.novabank.novapay.core.di;

import com.novabank.novapay.data.remote.server.EkycApi;
import com.novabank.novapay.data.remote.server.FaceCheckApi;
import com.novabank.novapay.data.remote.server.SavingsApi;
import com.novabank.novapay.data.remote.server.SecurityApi;
import com.novabank.novapay.data.repository.AccountRepositoryImpl;
import com.novabank.novapay.data.repository.AuthRepositoryImpl;
import com.novabank.novapay.data.repository.EkycRepositoryImpl;
import com.novabank.novapay.data.repository.LedgerRepositoryImpl;
import com.novabank.novapay.data.repository.NotificationRepositoryImpl;
import com.novabank.novapay.data.repository.SavingsRepositoryImpl;
import com.novabank.novapay.data.repository.SecurityRepositoryImpl;
import com.novabank.novapay.data.repository.UserRepositoryImpl;
import com.novabank.novapay.domain.repository.AccountRepository;
import com.novabank.novapay.domain.repository.AuthRepository;
import com.novabank.novapay.domain.repository.EkycRepository;
import com.novabank.novapay.domain.repository.LedgerRepository;
import com.novabank.novapay.domain.repository.NotificationRepository;
import com.novabank.novapay.domain.repository.SavingsRepository;
import com.novabank.novapay.domain.repository.SecurityRepository;
import com.novabank.novapay.domain.repository.UserRepository;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import retrofit2.Retrofit;

/**
 * Hilt Module gắn kết toàn bộ Retrofit API & Repository của Phân hệ A (Owner: Thành viên A).
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class CoreRepositoryModule {

    @Provides
    @Singleton
    public static EkycApi provideEkycApi(@BackendRetrofit Retrofit retrofit) {
        return retrofit.create(EkycApi.class);
    }

    @Provides
    @Singleton
    public static FaceCheckApi provideFaceCheckApi(@BackendRetrofit Retrofit retrofit) {
        return retrofit.create(FaceCheckApi.class);
    }

    @Provides
    @Singleton
    public static SavingsApi provideSavingsApi(@BackendRetrofit Retrofit retrofit) {
        return retrofit.create(SavingsApi.class);
    }

    @Provides
    @Singleton
    public static SecurityApi provideSecurityApi(@BackendRetrofit Retrofit retrofit) {
        return retrofit.create(SecurityApi.class);
    }

    @Binds
    @Singleton
    public abstract AccountRepository bindAccountRepository(AccountRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract AuthRepository bindAuthRepository(AuthRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract UserRepository bindUserRepository(UserRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract EkycRepository bindEkycRepository(EkycRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract LedgerRepository bindLedgerRepository(LedgerRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract NotificationRepository bindNotificationRepository(NotificationRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract SavingsRepository bindSavingsRepository(SavingsRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract SecurityRepository bindSecurityRepository(SecurityRepositoryImpl impl);
}
