package com.novabank.novapay.core.di;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

/**
 * Qualifier cho đối tượng Retrofit gọi về Backend Server Node.js của NovaPay
 * (có AuthInterceptor tự động đính kèm Firebase Bearer Token).
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface BackendRetrofit {
}
