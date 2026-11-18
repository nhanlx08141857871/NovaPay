package com.novabank.novapay.data.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.novabank.novapay.data.dto.ChatResponseDto;
import com.novabank.novapay.domain.model.chat.ChatAction;
import com.novabank.novapay.domain.model.chat.ChatReply;

import org.junit.Test;

public class ChatMapperTest {

    @Test
    public void toDomainReply_filtersInvalidDeepLinksAndPreservesValidOnes() {
        String json = "{"
                + "\"reply\":\"Số dư của bạn là 1.000.000 ₫\","
                + "\"actions\":["
                + "{\"label\":\"Chuyển tiền\",\"deepLink\":\"novapay://transfer\"},"
                + "{\"label\":\"Evil\",\"deepLink\":\"https://evil.example.com\"},"
                + "{\"label\":\"\",\"deepLink\":\"novapay://home\"}"
                + "]}";
        ChatResponseDto dto = new Gson().fromJson(json, ChatResponseDto.class);

        ChatReply reply = ChatMapper.toDomainReply(dto);
        assertEquals("Số dư của bạn là 1.000.000 ₫", reply.getReply());
        assertEquals(1, reply.getActions().size());
        assertEquals("novapay://transfer", reply.getActions().get(0).getDeepLink());
    }

    @Test
    public void chatAction_hasValidDeepLinkRequiresSchemeAndNonBlankLabel() {
        assertTrue(new ChatAction("Hóa đơn", "novapay://bills").hasValidDeepLink());
        assertFalse(new ChatAction(" ", "novapay://bills").hasValidDeepLink());
        assertFalse(new ChatAction("Web", "http://novapay.vn").hasValidDeepLink());
    }
}
