package com.f12.moitz.domain.subway;

import java.time.Duration;
import lombok.Getter;

@Getter
public class Edge {

    private final SubwayStation destination;
    private final Duration travelTime;
    private final int distance;
    private final SubwayLine subwayLine;

    public Edge(
            final SubwayStation destination,
            final int timeInSeconds,
            final int distance,
            final String subwayLine
    ) {
        this.destination = destination;
        this.travelTime = Duration.ofSeconds(timeInSeconds);
        this.distance = distance;
        this.subwayLine = subwayLine != null ? SubwayLine.fromTitle(subwayLine) : null;
    }

    public Edge(
            final SubwayStation destination,
            final long timeInSeconds,
            final int distance,
            final String subwayLine
    ) {
        this.destination = destination;
        this.travelTime = Duration.ofSeconds(timeInSeconds);
        this.distance = distance;
        this.subwayLine = subwayLine != null ? SubwayLine.fromTitle(subwayLine) : null;
    }

    public boolean isEqualTo(final Edge edge) {
        return this.destination.getName().equals(edge.getDestination().getName())
                && this.subwayLine.equals(edge.getSubwayLine());
    }

    public int getTimeInSeconds() {
        return (int) travelTime.getSeconds();
    }

    public boolean isSameLine(final SubwayLine subwayLine) {
        return this.subwayLine == subwayLine;
    }

    public boolean isTowards(final SubwayStation destination) {
        return this.destination.equals(destination);
    }
}
