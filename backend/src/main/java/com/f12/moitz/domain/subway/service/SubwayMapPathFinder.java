package com.f12.moitz.domain.subway.service;

import com.f12.moitz.domain.TravelMethod;
import com.f12.moitz.domain.subway.Edge;
import com.f12.moitz.domain.subway.SubwayLine;
import com.f12.moitz.domain.subway.SubwayPath;
import com.f12.moitz.domain.subway.SubwayStation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SubwayMapPathFinder {

    private final List<SubwayStation> stations;

    public List<SubwayPath> findShortestTimePath(final SubwayStation start, final SubwayStation end) {
        if (!stations.contains(start) || !stations.contains(end)) {
            log.error("노선도에 존재하지 않는 역입니다. 출발역: {}, 도착역: {}", start.getName(), end.getName());
            throw new IllegalStateException("출발역 또는 도착역이 노선도에 존재하지 않아 경로를 찾을 수 없습니다.");
        }
        if (start.equals(end)) {
            throw new IllegalStateException("출발역과 도착역은 동일할 수 없습니다.");
        }

        final Map<SubwayStation, Integer> times = new HashMap<>();
        final Map<SubwayStation, SubwayStation> prev = new HashMap<>();
        final Map<SubwayStation, SubwayLine> edgeLines = new HashMap<>(); // 각 역에 도달할 때 사용한 호선 저장
        final PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.time));
        final Set<SubwayStation> visited = new HashSet<>();

        // 초기화
        for (SubwayStation station : stations) {
            times.put(station, Integer.MAX_VALUE);
        }
        times.put(start, 0);
        pq.add(new Node(start, 0));

        while (!pq.isEmpty()) {
            final Node current = pq.poll();
            if (!visited.add(current.station)) {
                continue;
            }

            if (current.station.equals(end)) {
                break;
            }

            // 현재 역 정보 가져오기
            final SubwayStation currentStation = current.station;
            if (!stations.contains(currentStation)) {
                continue;
            }

            for (Edge edge : currentStation.getEdges()) {
                final SubwayStation neighbor = edge.getDestination();
                if (visited.contains(neighbor)) {
                    continue;
                }

                if (times.get(current.station) == null || times.get(neighbor) == null) {
                    throw new IllegalStateException("출발역 혹은 도착역에 도달하는 시간이 초기화되지 않았습니다.");
                }

                int newTime = times.get(current.station) + edge.getTimeInSeconds();

                // 환승 시간 추가
                if (!start.equals(current.station) && !edgeLines.get(current.station).equals(edge.getSubwayLine())) {
                    Edge transferEdge = null;
                    for (Edge currentEdge : currentStation.getEdges()) {
                        if (currentEdge.isTowards(current.station) && currentEdge.isSameLine(edge.getSubwayLine())) {
                            transferEdge = currentEdge;
                            break;
                        }
                    }
                    if (transferEdge == null) {
                        log.error("현재역: {}, 다음역: {}, 환승호선: {}", current.station.getName(), neighbor.getName(), edge.getSubwayLine().getTitle());
                        throw new IllegalStateException("환승 시간을 계산할 환승 Edge가 존재하지 않습니다.");
                    }

                    newTime += transferEdge.getTimeInSeconds();
                }

                if (newTime < times.get(neighbor)) {
                    times.put(neighbor, newTime);
                    prev.put(neighbor, current.station);
                    edgeLines.put(neighbor, edge.getSubwayLine()); // 이 역에 도달한 호선 저장
                    pq.add(new Node(neighbor, newTime));
                }
            }
        }

        final List<StationWithEdge> fullPath = reconstructPaths(prev, edgeLines, start, end);

        return groupByLine(fullPath);
    }

    private List<StationWithEdge> reconstructPaths(
            final Map<SubwayStation, SubwayStation> prev,
            final Map<SubwayStation, SubwayLine> edgeLines,
            final SubwayStation start,
            final SubwayStation end
    ) {
        final List<StationWithEdge> fullPath = new ArrayList<>();
        SubwayStation current = end;

        fullPath.addFirst(new StationWithEdge(current, null));

        SubwayStation previous = prev.get(current);
        while (previous != null) {
            SubwayStation next = current;
            current = previous;

            final SubwayStation currentStation = current;
            if (!stations.contains(currentStation)) {
                throw new IllegalStateException("찾으려는 이름과 일치하는 역이 노선도에 존재하지 않습니다.");
            }

            // edgeLines에서 다음 역(nextName)에 도달할 때 사용한 호선 정보를 가져옴
            final SubwayLine targetLine = edgeLines.get(next);

            // 현재 역에서 다음 역으로 가는 Edge 중에서 호선명이 일치하는 Edge 찾기
            Edge movementEdge = null;
            for (Edge edge : currentStation.getEdges()) {
                if (edge.getDestination().equals(next) && edge.getSubwayLine().equals(targetLine)) {
                    movementEdge = edge;
                    break;
                }
            }
            if (movementEdge == null) {
                log.error("현재역: {}, 다음역: {}, 환승호선: {}", current.getName(), next.getName(), targetLine.getTitle());
                throw new IllegalStateException("다음 역으로 가는 Edge가 존재하지 않습니다.");
            }

            // StationWithEdge 생성하여 경로에 추가
            fullPath.addFirst(new StationWithEdge(current, movementEdge));

            if (start.equals(current)) {
                break;
            }

            final SubwayLine currentLine = edgeLines.get(current);
            if (!currentLine.equals(targetLine)) {
                Edge transferEdge = null;
                for (Edge edge : currentStation.getEdges()) {
                    if (edge.getDestination().equals(current) && edge.getSubwayLine().equals(targetLine)) {
                        transferEdge = edge;
                        break;
                    }
                }
                if (transferEdge == null) {
                    log.error("현재역: {}, 다음역: {}, 환승호선: {}", current.getName(), next.getName(), targetLine.getTitle());
                    throw new IllegalStateException("환승역이지만 환승 Edge가 존재하지 않습니다.");
                }
                fullPath.addFirst(new StationWithEdge(current, transferEdge));
            }

            previous = prev.get(current);
        }
        if (!start.equals(current)) {
            throw new IllegalStateException("경로가 출발역까지 이어지지 않습니다.");
        }

        return fullPath;
    }

    private List<SubwayPath> groupByLine(final List<StationWithEdge> fullPath) {
        final List<SubwayPath> paths = new ArrayList<>();

        if (fullPath.isEmpty()) {
            throw new IllegalStateException("경로가 존재하지 않습니다.");
        }

        SubwayLine currentLine = fullPath.getFirst().getLine();
        SubwayStation startStation = fullPath.getFirst().station;
        int totalTime = 0;

        for (int i = 1; i < fullPath.size(); i++) {
            final StationWithEdge current = fullPath.get(i);

            // 이전 구간의 실제 이동 시간 계산 (Edge에서 직접 가져오기)
            final StationWithEdge previous = fullPath.get(i - 1);
            final int segmentTime = previous.edge != null ? previous.getTimeInSeconds() : 120;
            totalTime += segmentTime;

            // 환승 경로이거나 마지막 역인 경우
            if (current.isTransfer() || fullPath.getLast().equals(current)) {
                final SubwayStation endStation = current.station;

                final SubwayPath path = new SubwayPath(
                        startStation,
                        endStation,
                        TravelMethod.SUBWAY,
                        totalTime,
                        currentLine
                );
                paths.add(path);

                // 환승 처리: 같은 역에서 다른 호선으로 환승 (마지막 역이 아닌 경우에만)
                if (current.isTransfer() && i < fullPath.size() - 1) {
                    // 환승 시간 계산: 환승 Edge의 시간 직접 활용
                    final int transferTime = current.getTimeInSeconds();

                    final SubwayPath transferPath = new SubwayPath(
                            endStation,
                            endStation,
                            TravelMethod.TRANSFER,
                            transferTime,
                            null
                    );
                    paths.add(transferPath);

                    // 다음 구간 초기화
                    i++;
                    final StationWithEdge next = fullPath.get(i);
                    currentLine = next.getLine();
                    startStation = current.station;
                    totalTime = 0;
                }
            }
        }

        return paths;
    }

    private static class StationWithEdge {
        final SubwayStation station;
        final Edge edge;

        StationWithEdge(final SubwayStation station, final Edge edge) {
            this.station = station;
            this.edge = edge;
        }

        public boolean isTransfer() {
            if (edge == null) {
                return false;
            }
            return station.equals(edge.getDestination());
        }

        int getTimeInSeconds() {
            return edge.getTimeInSeconds();
        }

        SubwayLine getLine() {
            if (edge == null) {
                return null;
            }
            return edge.getSubwayLine();
        }
    }

    private static class Node {
        SubwayStation station;
        int time;

        Node(final SubwayStation station, final int time) {
            this.station = station;
            this.time = time;
        }
    }

}
