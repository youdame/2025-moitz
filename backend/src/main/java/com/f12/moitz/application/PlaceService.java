package com.f12.moitz.application;

import com.f12.moitz.application.port.PlaceFinder;
import com.f12.moitz.domain.Place;
import com.f12.moitz.domain.repository.PlaceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
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

    public int saveIfAbsent(final List<String> placeNames) {
        final List<Place> places = placeNames.stream()
                .filter(name -> !placeRepository.existsByName(name))
                .map(placeFinder::findPlaceByName)
                .toList();

        placeRepository.saveAll(places);

        return places.size();
    }

    public List<Place> generateCandidatePlace(List<Place> startingPlaces) {
        log.info("진입");
        GeometryFactory factory = new GeometryFactory();

        Coordinate[] coordinateArr = startingPlaces.stream()
                .map(place -> {
                    List<Double> coordinates = place.getPoint().getCoordinates();
                    return new Coordinate(coordinates.get(0), coordinates.get(1));
                })
                .toArray(Coordinate[]::new);

        MultiPoint multiPoint = factory.createMultiPointFromCoords(coordinateArr);

        Point centroid = multiPoint.getCentroid();

        log.info("centroid 생성");
        log.info("centroid : {}", centroid.getX() + " "  + centroid.getY());

        org.springframework.data.geo.Point center =
                new org.springframework.data.geo.Point(centroid.getX(), centroid.getY());

        Distance distance = new Distance(5, Metrics.KILOMETERS);

        List<Place> placeWithRadius = placeRepository.findByPointNear(center, distance);
        for (Place withRadius : placeWithRadius) {
            System.out.println(withRadius.getName());
        }
        return placeWithRadius;
    }
}
