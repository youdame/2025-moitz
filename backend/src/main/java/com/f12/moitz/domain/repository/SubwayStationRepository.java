package com.f12.moitz.domain.repository;

import com.f12.moitz.domain.subway.SubwayStation;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SubwayStationRepository extends MongoRepository<SubwayStation, String> {

    Optional<SubwayStation> findByName(String name);

}
