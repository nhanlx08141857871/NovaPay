package com.novabank.novapay.data.mapper;

import com.google.gson.Gson;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.dto.ApiErrorDto;

import okhttp3.ResponseBody;
import retrofit2.HttpException;

/**
 * Ánh xạ lỗi Retrofit HttpException / Exception sang AppResult.error với mã lỗi chuẩn của Server.
 */
public final class HttpErrorMapper {

    private static final Gson GSON = new Gson();

    private HttpErrorMapper() {
    }

    public static <T> AppResult<T> toAppResultError(Throwable throwable, String fallbackMessage) {
        if (throwable instanceof HttpException) {
            HttpException httpEx = (HttpException) throwable;
            try (ResponseBody errorBody = httpEx.response() != null ? httpEx.response().errorBody() : null) {
                if (errorBody != null) {
                    String raw = errorBody.string();
                    ApiErrorDto dto = GSON.fromJson(raw, ApiErrorDto.class);
                    if (dto != null && dto.getError() != null && !dto.getError().trim().isEmpty()) {
                        String msg = (dto.getMessage() != null && !dto.getMessage().trim().isEmpty())
                                ? dto.getMessage()
                                : fallbackMessage;
                        return AppResult.error(dto.getError().trim(), msg);
                    }
                }
            } catch (Exception ignored) {
                // Fallback bên dưới
            }
            return AppResult.error("HTTP_" + httpEx.code(), fallbackMessage);
        }
        String msg = (throwable != null && throwable.getMessage() != null && !throwable.getMessage().trim().isEmpty())
                ? throwable.getMessage()
                : fallbackMessage;
        return AppResult.error("NETWORK_ERROR", msg);
    }
}
