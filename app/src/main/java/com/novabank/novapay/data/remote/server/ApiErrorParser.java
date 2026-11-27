package com.novabank.novapay.data.remote.server;

import com.google.gson.Gson;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.dto.ApiErrorDto;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Response;

@Singleton
public class ApiErrorParser {

    private final Gson gson;

    @Inject
    public ApiErrorParser(Gson gson) {
        this.gson = gson;
    }

    public <T> AppResult<T> parseError(Throwable throwable) {
        if (throwable instanceof HttpException httpException) {
            return parseHttpException(httpException);
        }
        if (throwable instanceof IOException) {
            return AppResult.error("NETWORK_ERROR", "Không thể kết nối tới máy chủ NovaPay. Vui lòng kiểm tra mạng.");
        }
        String msg = throwable != null && throwable.getMessage() != null
                ? throwable.getMessage()
                : "Đã xảy ra lỗi không xác định.";
        return AppResult.error("UNKNOWN_ERROR", msg);
    }

    private <T> AppResult<T> parseHttpException(HttpException httpException) {
        int code = httpException.code();
        Response<?> response = httpException.response();
        if (response != null) {
            ResponseBody errorBody = response.errorBody();
            if (errorBody != null) {
                try {
                    String rawJson = errorBody.string();
                    if (rawJson != null && !rawJson.trim().isEmpty()) {
                        ApiErrorDto dto = gson.fromJson(rawJson, ApiErrorDto.class);
                        if (dto != null && dto.getError() != null && !dto.getError().trim().isEmpty()) {
                            String msg = dto.getMessage() != null ? dto.getMessage() : "";
                            return AppResult.error(dto.getError().trim(), msg);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return AppResult.error("HTTP_" + code, "Yêu cầu máy chủ thất bại (HTTP " + code + ").");
    }
}
