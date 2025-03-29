package com.zeroton.zeroSpring.data.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Document(collection = "room")
@Getter
@Setter
@Builder
@AllArgsConstructor
public class RoomEntity {
    private String id; // _id

    private List<Participant> participants;
    private String content;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

    private String image;
    private String type;
    private String imageURL;
    private String postInformation;
    private boolean brokenRoom;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Builder
    public static class Participant {
        private ObjectId userId; // userId.$oid
        private ObjectId recentMessageId; // null 일 수 있음
        private String name;
        private LocalDateTime enteredDate;
        private boolean notificationStatus;
        private String profileImage;
        private String profileImageURL;
        private boolean activated;
    }
}