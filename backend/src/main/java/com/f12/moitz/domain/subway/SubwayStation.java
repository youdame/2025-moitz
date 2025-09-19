package com.f12.moitz.domain.subway;

import com.f12.moitz.domain.Place;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;

@Getter
public class SubwayStation {

    private final Place place;
    private final List<Edge> edges = new ArrayList<>();

    public SubwayStation(final Place place) {
        this.place = place;
    }

    public String getName() {
        return this.place.getName();
    }

    public void addEdge(final Edge newEdge) {
        final Optional<Edge> existingEdge = edges.stream()
                .filter(edge -> edge.isEqualTo(newEdge))
                .findFirst();

        if (existingEdge.isEmpty()) {
            edges.add(newEdge);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubwayStation that = (SubwayStation) o;
        return Objects.equals(place.getName(), that.place.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(place.getName());
    }
}
