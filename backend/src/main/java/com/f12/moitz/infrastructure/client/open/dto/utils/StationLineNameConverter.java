package com.f12.moitz.infrastructure.client.open.dto.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.Map;

public class StationLineNameConverter extends JsonDeserializer<String> {

    private static final Map<String, String> NAME_EXCEPTIONS = Map.of(
            "경의선", "경의중앙선",
            "경의선 지선", "경의중앙선",
            "우이신설경전철", "우이신설선",
            "김포도시철도", "김포골드라인",
            "용인경전철", "용인에버라인",
            "인천선", "인천1호선"
    );

    @Override
    public String deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        final String value = p.getText();
        return NAME_EXCEPTIONS.getOrDefault(value, value);
    }

}
