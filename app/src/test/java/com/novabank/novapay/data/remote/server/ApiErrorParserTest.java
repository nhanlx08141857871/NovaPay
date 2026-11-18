package com.novabank.novapay.data.remote.server;

import com.google.gson.Gson;
import com.novabank.novapay.core.common.AppResult;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ApiErrorParserTest {

    private ApiErrorParser parser;

    @Before
    public void setUp() {
        parser = new ApiErrorParser(new Gson());
    }

    @Test
    public void parseError_extractsFlatApiErrorEnvelopeFromHttpException() {
        ResponseBody body = ResponseBody.create(
                "{\"error\":\"INSUFFICIENT_FUNDS\",\"message\":\"Số dư tài khoản không đủ.\"}",
                MediaType.get("application/json")
        );
        HttpException exception = new HttpException(Response.error(400, body));

        AppResult<Object> result = parser.parseError(exception);
        assertTrue(result.isError());
        assertEquals("INSUFFICIENT_FUNDS", result.getErrorCode());
        assertEquals("Số dư tài khoản không đủ.", result.getMessage());
    }

    @Test
    public void parseError_fallsBackToHttpCodeWhenErrorBodyMalformed() {
        ResponseBody body = ResponseBody.create("not-json", MediaType.get("text/plain"));
        HttpException exception = new HttpException(Response.error(502, body));

        AppResult<Object> result = parser.parseError(exception);
        assertTrue(result.isError());
        assertEquals("HTTP_502", result.getErrorCode());
    }

    @Test
    public void parseError_mapsIoExceptionToNetworkError() {
        AppResult<Object> result = parser.parseError(new IOException("timeout"));
        assertTrue(result.isError());
        assertEquals("NETWORK_ERROR", result.getErrorCode());
    }
}
