package com.novabank.novapay.core.di;

import com.novabank.novapay.data.remote.server.AccountLookupApi;
import com.novabank.novapay.data.remote.server.PaymentApi;
import com.novabank.novapay.data.remote.server.TransactionApi;
import com.novabank.novapay.data.repository.BankRepositoryImpl;
import com.novabank.novapay.data.repository.BeneficiaryRepositoryImpl;
import com.novabank.novapay.data.repository.SourceAccountRepositoryImpl;
import com.novabank.novapay.data.repository.TransactionRepositoryImpl;
import com.novabank.novapay.data.repository.UtilityRepositoryImpl;
import com.novabank.novapay.domain.repository.AccountRepository;
import com.novabank.novapay.domain.repository.BankRepository;
import com.novabank.novapay.domain.repository.BeneficiaryRepository;
import com.novabank.novapay.domain.repository.SourceAccountRepository;
import com.novabank.novapay.domain.repository.TransactionRepository;
import com.novabank.novapay.domain.repository.UtilityRepository;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.BindsOptionalOf;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import retrofit2.Retrofit;

/**
 * Hilt DI Module độc quyền của Phân hệ B.
 * Khai báo @BindsOptionalOf AccountRepository để biên dịch độc lập trước khi merge với Phân hệ A.
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class MoneyRepositoryModule {

    @BindsOptionalOf
    public abstract AccountRepository optionalAccountRepository();

    @Binds
    @Singleton
    public abstract SourceAccountRepository bindSourceAccountRepository(
            SourceAccountRepositoryImpl impl
    );

    @Binds
    @Singleton
    public abstract TransactionRepository bindTransactionRepository(
            TransactionRepositoryImpl impl
    );

    @Binds
    @Singleton
    public abstract BankRepository bindBankRepository(
            BankRepositoryImpl impl
    );

    @Binds
    @Singleton
    public abstract BeneficiaryRepository bindBeneficiaryRepository(
            BeneficiaryRepositoryImpl impl
    );

    @Binds
    @Singleton
    public abstract UtilityRepository bindUtilityRepository(
            UtilityRepositoryImpl impl
    );

    @Provides
    @Singleton
    public static AccountLookupApi provideAccountLookupApi(@BackendRetrofit Retrofit retrofit) {
        return retrofit.create(AccountLookupApi.class);
    }

    @Provides
    @Singleton
    public static TransactionApi provideTransactionApi(@BackendRetrofit Retrofit retrofit) {
        return retrofit.create(TransactionApi.class);
    }

    @Provides
    @Singleton
    public static PaymentApi providePaymentApi(@BackendRetrofit Retrofit retrofit) {
        return retrofit.create(PaymentApi.class);
    }
}
