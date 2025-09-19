package com.f12.moitz.domain.subway;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SubwayEdges {

    private final Map<SubwayStation, List<Edge>> stationMap;

    public SubwayEdges(final Map<SubwayStation, List<Edge>> stationMap) {
        this.stationMap = stationMap;
    }

    public SubwayEdges() {
        this.stationMap = new HashMap<>();
    }

    public void addEdge(final SubwayStation targetStation, final Edge newEdge) {
        final List<Edge> edges = stationMap.get(targetStation);
        final Optional<Edge> existingEdge = edges.stream()
                .filter(edge -> edge.isEqualTo(newEdge))
                .findFirst();

        if (existingEdge.isEmpty()) {
            edges.add(newEdge);
        }
    }

}
