package com.f12.moitz.domain.repository;

import com.f12.moitz.infrastructure.persistence.SubwayStationDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SubwayStationRepository extends MongoRepository<SubwayStationDocument, String> {
    Optional<SubwayStationDocument> findByPlace_Name(String name);
}
