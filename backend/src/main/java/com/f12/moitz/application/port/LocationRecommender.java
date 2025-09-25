package com.f12.moitz.application.port;

import com.f12.moitz.application.dto.RecommendedLocationsResponse;
import java.util.List;

public interface LocationRecommender {

    RecommendedLocationsResponse recommendLocations(final List<String> startingPlaces,final List<String> candidatePlace, final String requirement);
}
