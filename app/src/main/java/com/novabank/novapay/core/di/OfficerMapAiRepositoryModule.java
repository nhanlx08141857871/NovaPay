package com.novabank.novapay.core.di;

import com.novabank.novapay.data.remote.maps.OpenRouteServiceApi;
import com.novabank.novapay.data.remote.server.AiApi;
import com.novabank.novapay.data.remote.server.OfficerApi;
import com.novabank.novapay.data.repository.BranchRepositoryImpl;
import com.novabank.novapay.data.repository.ChatRepositoryImpl;
import com.novabank.novapay.data.repository.InterestRateRepositoryImpl;
import com.novabank.novapay.data.repository.OfficerRepositoryImpl;
import com.novabank.novapay.domain.repository.BranchRepository;
import com.novabank.novapay.domain.repository.ChatRepository;
import com.novabank.novapay.domain.repository.InterestRateRepository;
import com.novabank.novapay.domain.repository.OfficerRepository;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import retrofit2.Retrofit;

@Module
@InstallIn(SingletonComponent.class)
public abstract class OfficerMapAiRepositoryModule {

    @Provides
    @Singleton
    public static OfficerApi provideOfficerApi(@BackendRetrofit Retrofit retrofit) {
        return retrofit.create(OfficerApi.class);
    }

    @Provides
    @Singleton
    public static AiApi provideAiApi(@BackendRetrofit Retrofit retrofit) {
        return retrofit.create(AiApi.class);
    }

    @Provides
    @Singleton
    public static OpenRouteServiceApi provideOpenRouteServiceApi(@OpenRouteRetrofit Retrofit retrofit) {
        return retrofit.create(OpenRouteServiceApi.class);
    }

    @Binds
    @Singleton
    public abstract OfficerRepository bindOfficerRepository(OfficerRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract InterestRateRepository bindInterestRateRepository(InterestRateRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract BranchRepository bindBranchRepository(BranchRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract ChatRepository bindChatRepository(ChatRepositoryImpl impl);
}
