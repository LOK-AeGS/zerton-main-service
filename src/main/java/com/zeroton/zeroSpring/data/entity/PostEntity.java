package com.zeroton.zeroSpring.data.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@Document(collection = "posts")
public class PostEntity {
    @Id
    private String id;

    private ObjectId writerId;
    private String name;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

    private String title;
    private String content;
    private String schoolCode;

    private List<String> photos;

    private List<String> likes;
    private List<String> tags;

    private GeoJsonPoint geoInfo;

    private int price;

    private List<Integer> availableDates;
//    private int state; //0,1,2,3

//    @DBRef
//    private UserEntity borrowerInfo;

    //조회수
    private int viewCount;

    private List<BorrowerInfo> borrowerInfo;

    private String rentalType; // 무료나눔인지, 빌려주는건지, 장기렌트, 단기렌트
    private String itemType;

    private String priceByPeriod; // 빌릴 수 있는 기간 ? 형이 priceType으로 만들어달라 하신 것

    @Getter
    @Setter
    @Builder
    public static class BorrowerInfo {
        private ObjectId userId;
        private int state; // 0: 대기, 1: 빌려줌, 2: 반납
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private LocalDateTime createdAt;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private LocalDateTime startDate;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private LocalDateTime endDate;

        private List<String> returnImage;
    }
}