package com.f12.moitz.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Document(collection = "place")
@EqualsAndHashCode
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Place {

    @Id
    private String id;
    private String name;
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint point;

    public Place(final String name, final GeoJsonPoint point) {
        validate(name, point);
        this.name = name;
        this.point = point;
    }

    private void validate(final String name, final GeoJsonPoint point) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("이름은 비어있거나 null일 수 없습니다.");
        }
        if (point == null) {
            throw new IllegalArgumentException("좌표는 필수입니다.");
        }
    }

}
