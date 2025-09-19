package com.f12.moitz.application;

import com.f12.moitz.application.port.SubwayMapLoader;
import com.f12.moitz.application.port.dto.RawPathInfo;
import com.f12.moitz.application.port.dto.RawRouteInfo;
import com.f12.moitz.domain.Place;
import com.f12.moitz.domain.Point;
import com.f12.moitz.domain.repository.SubwayStationRepository;
import com.f12.moitz.domain.subway.Edge;
import com.f12.moitz.infrastructure.persistence.EdgeDocument;
import com.f12.moitz.domain.subway.SubwayStation;
import com.f12.moitz.infrastructure.persistence.SubwayStationDocument;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import com.f12.moitz.domain.subway.SubwayLine;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubwayStationService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Point DEFAULT_POINT = new Point(127.027610, 37.498095);
    private final SubwayStationRepository subwayStationRepository;
    private final SubwayMapLoader subwayMapLoader;

    @Transactional
    public void initializeStations() {
        try {
            List<RawRouteInfo> rawRoutes = subwayMapLoader.loadRawRoutes();
            Map<String, SubwayStation> stationDomainMap = assembleSubwayStations(rawRoutes);
            List<SubwayStationDocument> stationDocuments = mapToDocuments(stationDomainMap);

            log.info("MongoDB에 {}개 역 저장 시작", stationDocuments.size());
            subwayStationRepository.deleteAll();
            subwayStationRepository.saveAll(stationDocuments);
            log.info("MongoDB 저장 완료 - 총 {}개 역", stationDocuments.size());
        } catch (Exception e) {
            log.error("MongoDB 저장 실패", e);
            throw new RuntimeException("MongoDB 저장에 실패했습니다.", e);
        }
    }

    private Map<String, SubwayStation> assembleSubwayStations(List<RawRouteInfo> rawRoutes) {
        Map<String, SubwayStation> stationMap = new HashMap<>();
        for (RawRouteInfo rawRoute : rawRoutes) {
            final List<RawPathInfo> paths = rawRoute.paths();
            for (int i = 0; i < paths.size(); i++) {
                final RawPathInfo currentPath = paths.get(i);
                final String fromName = currentPath.departureStation().stationName();
                final String toName = currentPath.arrivalStation().stationName();
                final String fromLine = currentPath.departureStation().lineName();
                final String toLine = currentPath.arrivalStation().lineName();

                SubwayStation fromStation = stationMap.computeIfAbsent(fromName, name -> new SubwayStation(new Place(name, DEFAULT_POINT)));
                SubwayStation toStation = stationMap.computeIfAbsent(toName, name -> new SubwayStation(new Place(name, DEFAULT_POINT)));

                final int distance = currentPath.stationSectionDistance();
                final int travelTimeInSeconds = calculateTravelTime(currentPath, paths, i);

                fromStation.addEdge(new Edge(toStation, travelTimeInSeconds, distance, fromLine));
                if (currentPath.isTransfer()) {
                    fromStation.addEdge(new Edge(toStation, travelTimeInSeconds, distance, toLine));
                } else {
                    toStation.addEdge(new Edge(fromStation, travelTimeInSeconds, distance, fromLine));
                }
            }
        }
        addMissingData(stationMap);
        return stationMap;
    }

    private List<SubwayStationDocument> mapToDocuments(Map<String, SubwayStation> stationDomainMap) {
        List<SubwayStationDocument> documents = new ArrayList<>();
        for (SubwayStation station : stationDomainMap.values()) {
            List<EdgeDocument> edgeDocs = station.getEdges().stream()
                    .map(edge -> new EdgeDocument(
                            edge.getDestination().getName(),
                            edge.getTravelTime().toSeconds(),
                            edge.getDistance(),
                            edge.getSubwayLine()))
                    .collect(Collectors.toList());
            documents.add(new SubwayStationDocument(station.getPlace(), edgeDocs));
        }
        return documents;
    }

    private int calculateTravelTime(final RawPathInfo currentPath, final List<RawPathInfo> allPaths, final int currentIndex) {
        if (currentPath.isTransfer()) {
            return currentPath.waitingSeconds() + currentPath.requiredSeconds();
        }

        if (currentIndex < allPaths.size() - 1) {
            final RawPathInfo nextPath = allPaths.get(currentIndex + 1);
            final String currentDepartureTime = currentPath.trainDepartureTime();
            final String nextDepartureTime = nextPath.trainDepartureTime();

            if (currentDepartureTime != null && nextDepartureTime != null) {
                return calculateTimeDifference(currentDepartureTime, nextDepartureTime);
            }
        }
        return currentPath.requiredSeconds();
    }

    private int calculateTimeDifference(final String currentDepartureTime, final String nextDepartureTime) {
        try {
            final LocalTime current = LocalTime.parse(currentDepartureTime, TIME_FORMATTER);
            LocalTime next = LocalTime.parse(nextDepartureTime, TIME_FORMATTER);
            if (next.isBefore(current)) {
                next = next.plusHours(24);
            }
            return (int) Duration.between(current, next).getSeconds();
        } catch (DateTimeException | ArithmeticException e) {
            throw new IllegalStateException("경로의 출발 시간을 파싱할 수 없습니다.");
        }
    }

    public List<SubwayStation> findAll() {
        List<SubwayStationDocument> documents = subwayStationRepository.findAll();
        if (documents.isEmpty()) {
            return new ArrayList<>();
        }
        return mapDocumentsToDomain(documents);
    }

    public SubwayStation findByName(String name) {
        // 전체 노선도를 알아야 정확한 도메인 객체를 만들 수 있으므로, 결국 모든 역 정보가 필요
        return findAll().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("이름이 일치하는 SubwayStation이 존재하지 않습니다: " + name));
    }

    private List<SubwayStation> mapDocumentsToDomain(List<SubwayStationDocument> documents) {
        Map<String, SubwayStation> stationCache = new HashMap<>();

        // 1. 모든 역에 대한 도메인 객체를 먼저 생성하여 저장
        for (SubwayStationDocument doc : documents) {
            stationCache.computeIfAbsent(doc.getPlace().getName(),
                    n -> new SubwayStation(doc.getPlace()));
        }

        // 2. 도메인 객체를 참조하여 Edge를 조립
        for (SubwayStationDocument doc : documents) {
            SubwayStation station = stationCache.get(doc.getPlace().getName());
            if (doc.getEdges() != null) {
                for (EdgeDocument edgeDoc : doc.getEdges()) {
                    SubwayStation destination = stationCache.get(edgeDoc.getDestinationName());
                    if (destination != null) {
                        long travelTimeInSeconds = edgeDoc.getTravelTime();
                        String subwayLineTitle = edgeDoc.getSubwayLine() != null ? edgeDoc.getSubwayLine().getTitle() : null;
                        Edge newEdge = new Edge(destination, travelTimeInSeconds, edgeDoc.getDistance(), subwayLineTitle);
                        station.addEdge(newEdge);
                    }
                }
            }
        }
        return new ArrayList<>(stationCache.values());
    }

    private void addMissingData(Map<String, SubwayStation> stationMap) {
        log.debug("addMissingData 호출됨. {}개 역에 대한 추가 데이터 처리 시작", stationMap.size());

        // 중랑역
        final SubwayStation joongrang = stationMap.get("중랑역");
        if (joongrang != null) {
            joongrang.addEdge(new Edge(joongrang, 1200, 0, SubwayLine.GYEONGUI_JOUNGANG.getTitle()));
            joongrang.addEdge(new Edge(joongrang, 1200, 0, SubwayLine.GYEONGCHUN.getTitle()));
            log.debug("중랑역 환승 Edge 추가됨: 경의중앙선, 경춘선");
        }

        // 을지로4가역
        final SubwayStation euljiro4ga = stationMap.get("을지로4가역");
        if (euljiro4ga != null) {
            euljiro4ga.addEdge(new Edge(euljiro4ga, 550, 0, SubwayLine.SEOUL_METRO_LINE2.getTitle()));
            euljiro4ga.addEdge(new Edge(euljiro4ga, 550, 0, SubwayLine.SEOUL_METRO_LINE5.getTitle()));
            log.debug("을지로4가역 환승 Edge 추가됨: 2호선, 5호선");
        }

        // 청량리역
        final SubwayStation cheongnyangni = stationMap.get("청량리역");
        if (cheongnyangni != null) {
            cheongnyangni.addEdge(new Edge(cheongnyangni, 1000, 0, SubwayLine.GYEONGCHUN.getTitle()));
            log.debug("청량리역 환승 Edge 추가됨: 경춘선");
        }

        // 오이도역
        final SubwayStation oido = stationMap.get("오이도역");
        if (oido != null) {
            oido.addEdge(new Edge(oido, 500, 0, SubwayLine.SEOUL_METRO_LINE4.getTitle()));
            oido.addEdge(new Edge(oido, 500, 0, SubwayLine.SUIN_BUNDANG.getTitle()));
            log.debug("오이도역 환승 Edge 추가됨: 4호선, 수인분당선");
        }

        // 정왕역
        final SubwayStation jeongwang = stationMap.get("정왕역");
        if (jeongwang != null) {
            jeongwang.addEdge(new Edge(jeongwang, 500, 0, SubwayLine.SEOUL_METRO_LINE4.getTitle()));
            jeongwang.addEdge(new Edge(jeongwang, 500, 0, SubwayLine.SUIN_BUNDANG.getTitle()));
            log.debug("정왕역 환승 Edge 추가됨: 4호선, 수인분당선");
        }

        // 신길온천역
        final SubwayStation singiloncheon = stationMap.get("신길온천역");
        if (singiloncheon != null) {
            singiloncheon.addEdge(new Edge(singiloncheon, 500, 0, SubwayLine.SEOUL_METRO_LINE4.getTitle()));
            singiloncheon.addEdge(new Edge(singiloncheon, 500, 0, SubwayLine.SUIN_BUNDANG.getTitle()));
            log.debug("신길온천역 환승 Edge 추가됨: 4호선, 수인분당선");
        }

        // 안산역
        final SubwayStation ansan = stationMap.get("안산역");
        if (ansan != null) {
            ansan.addEdge(new Edge(ansan, 500, 0, SubwayLine.SEOUL_METRO_LINE4.getTitle()));
            ansan.addEdge(new Edge(ansan, 500, 0, SubwayLine.SUIN_BUNDANG.getTitle()));
            log.debug("안산역 환승 Edge 추가됨: 4호선, 수인분당선");
        }

        // 초지역
        final SubwayStation choji = stationMap.get("초지역");
        if (choji != null) {
            choji.addEdge(new Edge(choji, 500, 0, SubwayLine.SEOUL_METRO_LINE4.getTitle()));
            choji.addEdge(new Edge(choji, 500, 0, SubwayLine.SUIN_BUNDANG.getTitle()));
            log.debug("초지역 환승 Edge 추가됨: 4호선, 수인분당선");
        }

        // 고잔역
        final SubwayStation gojan = stationMap.get("고잔역");
        if (gojan != null) {
            gojan.addEdge(new Edge(gojan, 500, 0, SubwayLine.SEOUL_METRO_LINE4.getTitle()));
            gojan.addEdge(new Edge(gojan, 500, 0, SubwayLine.SUIN_BUNDANG.getTitle()));
            log.debug("고잔역 환승 Edge 추가됨: 4호선, 수인분당선");
        }

        // 중앙역
        final SubwayStation jungang = stationMap.get("중앙역");
        if (jungang != null) {
            jungang.addEdge(new Edge(jungang, 500, 0, SubwayLine.SEOUL_METRO_LINE4.getTitle()));
            jungang.addEdge(new Edge(jungang, 500, 0, SubwayLine.SUIN_BUNDANG.getTitle()));
            log.debug("중앙역 환승 Edge 추가됨: 4호선, 수인분당선");
        }
    }
}
