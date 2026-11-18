package com.novabank.novapay.data.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.novabank.novapay.core.common.AppResult;

import org.junit.Test;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Response;

public class ApiErrorParserTest {

    private final ApiErrorParser parser = new ApiErrorParser(new Gson());

    @Test
    public void toErrorResult_parsesServerErrorEnvelopeFromHttpException() {
        ResponseBody body = ResponseBody.create(
                "{\"error\":\"EMAIL_ALREADY_EXISTS\",\"message\":\"Email đã tồn tại.\"}",
                MediaType.get("application/json")
        );
        HttpException ex = new HttpException(Response.error(409, body));

        AppResult<String> result = parser.toErrorResult(ex);
        assertTrue(result.isError());
        assertEquals("EMAIL_ALREADY_EXISTS", result.getErrorCode());
        assertEquals("Email đã tồn tại.", result.getMessage());
    }

    @Test
    public void toErrorResult_handlesMalformedErrorBodyGracefully() {
        ResponseBody body = ResponseBody.create("not-json", MediaType.get("text/plain"));
        HttpException ex = new HttpException(Response.error(500, body));

        AppResult<String> result = parser.toErrorResult(ex);
        assertTrue(result.isError());
        assertEquals("HTTP_500", result.getErrorCode());
    }

    @Test
    public void toErrorResult_mapsIoExceptionToNetworkError() {
        AppResult<String> result = parser.toErrorResult(new IOException("timeout"));
        assertTrue(result.isError());
        assertEquals("NETWORK_ERROR", result.getErrorCode());
    }
}
