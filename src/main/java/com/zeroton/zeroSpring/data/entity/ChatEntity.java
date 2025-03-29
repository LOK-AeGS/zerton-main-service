package com.zeroton.zeroSpring.data.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import org.bson.types.ObjectId;

@Document(collection = "messages")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatEntity {

    @Id
    private String id; // MongoDB _id

    private ObjectId roomId;
    private ObjectId senderId;
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

    @Builder.Default
    private String type = "chat"; // 기본값 "chat"

    private Image image;
    private String uid;
    private Mention mention;
    private LocalDateTime deletedDate;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Image {
        private String url;
        private Double height;
        private Double width;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Mention {
        private ObjectId messageId;
        private ObjectId userId;
        private String name;
        private String content;
        private String image;
    }
}