package com.springai.chat.tools;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@Service
public class MockLiveService {

    public Response getRoomList(Request request) {
        return new Response(String.format("您的需求为%s, 预算范围是%s，居住地址是%s,现在为您匹配合适的房源", request.roomType, request.money, request.address));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(
            @JsonProperty(required = true, value = "roomType") @JsonPropertyDescription("租房的类型, 比如合租、整租、一居、二居") String roomType,
            @JsonProperty(required = true, value = "money") @JsonPropertyDescription("租房预算范围, 比如3000到4000") String money,
            @JsonProperty(required = true, value = "address") @JsonPropertyDescription("租房的地址, 比如徐汇区漕河泾") String address) {
    }

    public record Response(String description) {

    }
}
