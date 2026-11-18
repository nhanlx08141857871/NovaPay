package com.novabank.novapay.core.di;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.novabank.novapay.BuildConfig;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.data.remote.server.AuthInterceptor;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Hilt Module cung cấp @BackendRetrofit (có AuthInterceptor) và @OpenRouteRetrofit (không gắn Token)
 * theo SPEC_CHOT.md Q6 (Owner: Thành viên A).
 */
@Module
@InstallIn(SingletonComponent.class)
public final class NetworkModule {

    @Provides
    @Singleton
    public Gson provideGson() {
        return new GsonBuilder().create();
    }

    @Provides
    @Singleton
    public HttpLoggingInterceptor provideLoggingInterceptor() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        return interceptor;
    }

    @Provides
    @Singleton
    @BackendRetrofit
    public Retrofit provideBackendRetrofit(
            Gson gson,
            AuthInterceptor authInterceptor,
            HttpLoggingInterceptor loggingInterceptor
    ) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build();

        String baseUrl = !BuildConfig.BACKEND_BASE_URL.isEmpty()
                ? BuildConfig.BACKEND_BASE_URL
                : Constants.DEFAULT_BACKEND_BASE_URL;

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    @OpenRouteRetrofit
    public Retrofit provideOpenRouteRetrofit(
            Gson gson,
            HttpLoggingInterceptor loggingInterceptor
    ) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();

        return new Retrofit.Builder()
                .baseUrl(Constants.OPEN_ROUTE_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }
}
