package com.f12.moitz.application;

import com.f12.moitz.application.port.PlaceFinder;
import com.f12.moitz.domain.Place;
import com.f12.moitz.domain.repository.PlaceRepository;
import com.f12.moitz.domain.subway.SubwayStation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final PlaceFinder placeFinder;

    public Place findByName(final String name) {
        return placeRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("이름이 일치하는 Place가 존재하지 않습니다."));
    }

    public List<Place> findByNames(final List<String> names) {
        return names.stream()
                .map(this::findByName)
                .toList();
    }

    public int saveIfAbsent(final List<SubwayStation> places) {
        final List<Place> newPlaces = places.stream()
                // TODO: 이름으로만 확인하지 말고 edge, 좌표, 라인도 확인해야 할지 고민하기
                .filter(station -> !placeRepository.existsByName(station.getName()))
                .map(station -> placeFinder.findPlaceByName(station.getName()))
                .toList();

        placeRepository.saveAll(newPlaces);

        return places.size();
    }

}
