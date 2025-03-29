package com.zeroton.zeroSpring.data.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Document(collection = "schools")
public class SchoolEntity {
    @Id
    private String id;
    private String shcoolCode;
    private String schoolName;
    private GeoJsonPoint geoInfo;
}
