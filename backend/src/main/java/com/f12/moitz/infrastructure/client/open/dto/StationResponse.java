package com.f12.moitz.infrastructure.client.open.dto;

import com.f12.moitz.infrastructure.client.open.dto.utils.StationLineNameConverter;
import com.f12.moitz.infrastructure.client.open.dto.utils.StationNameConverter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StationResponse(
        @JsonProperty("stnNm")
        @JsonDeserialize(using = StationNameConverter.class)
        String stationName,

        @JsonProperty("lineNm")
        @JsonDeserialize(using = StationLineNameConverter.class)
        String lineName
) {

}
