package com.novabank.novapay.data.mapper;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.dto.ApiErrorDto;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Response;

@Singleton
public final class ApiErrorParser {

    private final Gson gson;

    @Inject
    public ApiErrorParser(Gson gson) {
        this.gson = gson;
    }

    public <T> AppResult<T> toErrorResult(Throwable throwable) {
        if (throwable instanceof HttpException httpException) {
            Response<?> response = httpException.response();
            if (response != null) {
                try (ResponseBody errorBody = response.errorBody()) {
                    if (errorBody != null) {
                        String raw = errorBody.string();
                        if (!raw.trim().isEmpty()) {
                            ApiErrorDto dto = gson.fromJson(raw, ApiErrorDto.class);
                            if (dto != null && (dto.getError() != null || dto.getMessage() != null)) {
                                String code = dto.getError() != null ? dto.getError() : "HTTP_" + httpException.code();
                                String msg = dto.getMessage() != null && !dto.getMessage().isEmpty()
                                        ? dto.getMessage()
                                        : "Yêu cầu máy chủ thất bại (" + httpException.code() + ").";
                                return AppResult.error(code, msg);
                            }
                        }
                    }
                } catch (IOException | JsonSyntaxException ignored) {
                }
            }
            return AppResult.error(
                    "HTTP_" + httpException.code(),
                    "Máy chủ trả về lỗi (" + httpException.code() + ")."
            );
        }

        if (throwable instanceof IOException) {
            return AppResult.error(
                    "NETWORK_ERROR",
                    "Không thể kết nối tới máy chủ. Vui lòng kiểm tra mạng."
            );
        }

        String msg = throwable != null && throwable.getMessage() != null
                ? throwable.getMessage()
                : "Đã xảy ra lỗi không xác định.";
        return AppResult.error("UNKNOWN_ERROR", msg);
    }
}
