package com.springai.chat.tools;

import java.util.function.Function;

public class MockWeatherService implements
        Function<com.springai.chat.tools.MockWeatherService.Request, com.springai.chat.tools.MockWeatherService.Response> {
    public enum Unit {
        C, F
    }

    public record Request(String location, Unit unit) {
    }

    public record Response(double temp, Unit unit) {
    }

    public Response apply(Request request) {
        return new Response(30.0, Unit.C);
    }
}