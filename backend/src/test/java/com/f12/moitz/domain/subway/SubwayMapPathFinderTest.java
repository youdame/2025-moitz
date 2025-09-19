package com.f12.moitz.domain.subway;

import com.f12.moitz.domain.Place;
import com.f12.moitz.domain.Point;
import com.f12.moitz.domain.subway.service.SubwayMapPathFinder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SubwayMapPathFinderTest {

    private SubwayMapPathFinder pathFinder;
    private static final Point DEFAULT_POINT = new Point(127.0, 37.0);
    private final Map<String, SubwayStation> map = new HashMap<>();

    @BeforeEach
    void setUp() {
        SubwayStation station1 = new SubwayStation(new Place("청량리", DEFAULT_POINT));
        map.put("청량리", station1);
        SubwayStation station2 = new SubwayStation(new Place("회기", DEFAULT_POINT));
        map.put("회기", station2);
        SubwayStation station3 = new SubwayStation(new Place("중랑", DEFAULT_POINT));
        map.put("중랑", station3);
        SubwayStation station4 = new SubwayStation(new Place("상봉", DEFAULT_POINT));
        map.put("상봉", station4);

        station1.addEdge(new Edge(station2, 180, 0, "1호선"));
        station1.addEdge(new Edge(station2, 240, 0, "경의중앙선"));
        station1.addEdge(new Edge(station2, 210, 0, "경춘선"));

        station2.addEdge(new Edge(station1, 180, 0, "1호선"));
        station2.addEdge(new Edge(station1, 240, 0, "경의중앙선"));
        station2.addEdge(new Edge(station1, 210, 0, "경춘선"));
        station2.addEdge(new Edge(station3, 180, 0, "경의중앙선"));
        station2.addEdge(new Edge(station3, 150, 0, "경춘선"));
        station2.addEdge(new Edge(station2, 1560, 0, "1호선"));
        station2.addEdge(new Edge(station2, 1380, 0, "경의중앙선"));
        station2.addEdge(new Edge(station2, 1560, 0, "경춘선"));

        station3.addEdge(new Edge(station2, 180, 0, "경의중앙선"));
        station3.addEdge(new Edge(station2, 150, 0, "경춘선"));
        station3.addEdge(new Edge(station4, 120, 0, "경의중앙선"));
        station3.addEdge(new Edge(station4, 150, 0, "경춘선"));

        station4.addEdge(new Edge(station3, 120, 0, "경의중앙선"));
        station4.addEdge(new Edge(station3, 150, 0, "경춘선"));

        pathFinder = new SubwayMapPathFinder(new ArrayList<>(map.values()));
    }

    @DisplayName("청량리-회기 최단경로를 찾는다.")
    @Test
    void findShortest() {
        // Given
        SubwayStation cheongnyangni = map.get("청량리");
        SubwayStation hoegi = map.get("회기");

        // When
        List<SubwayPath> paths = pathFinder.findShortestTimePath(cheongnyangni, hoegi);

        // Then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(paths).hasSize(1);
            softly.assertThat(paths.getFirst().totalTime()).isEqualTo(180);
            softly.assertThat(paths.getFirst().line()).isEqualTo(SubwayLine.SEOUL_METRO_LINE1);
        });
    }

}
