package com.novabank.novapay.core.di;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

/**
 * Qualifier cho đối tượng Retrofit gọi tới OpenRouteService API (Bản đồ chỉ đường đi bộ)
 * — KHÔNG đính kèm Firebase Bearer Token ngân hàng.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface OpenRouteRetrofit {
}
