package com.zeroton.zeroSpring.service;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.mongodb.client.AggregateIterable;
import com.zeroton.zeroSpring.data.dto.request.PostUploadReceive;
import com.zeroton.zeroSpring.data.dto.request.ReturnAuthentication;
import com.zeroton.zeroSpring.data.entity.ChatEntity;
import com.zeroton.zeroSpring.data.entity.PostEntity;
import com.zeroton.zeroSpring.data.entity.RoomEntity;
import com.zeroton.zeroSpring.data.entity.UserEntity;
import com.zeroton.zeroSpring.utils.Constants;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PostService {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private S3Service s3Service;

    public ResponseEntity<Document> getPost(String postId) {
        List<Document> documents = Arrays.asList(
                new Document("$match",
                        new Document(
                                "_id", new ObjectId(postId)
                        )
                ),
                this.borrowerLookupPipeline(),
                this.userLookupPipeline(),
                this.unwindPipeline(),
                this.postProjection()
        );

        Document post = mongoTemplate.getCollection("posts").aggregate(documents).first();

        return ResponseEntity.ok(Objects.requireNonNull(post));
    }
    //state-> 2로 변경, startDate, endDate,borrowerId -> 옮겨주기
    public ResponseEntity<Object> returnLent(ReturnAuthentication returnAuthentication)
            throws IOException {

//        List<String> photoUrlPath = new ArrayList<>(); // 리스트 초기화

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(MediaType.IMAGE_JPEG_VALUE);

//        for(MultipartFile multipartFile : returnAuthentication.getReturnImage()){
            metadata.setContentLength(returnAuthentication.getReturnImage().getSize());

            // 파일명: writerId + 현재시간 + 확장자
            String fileName = returnAuthentication.getBorrowerId()
                    + "_" + System.currentTimeMillis() + ".jpeg";

            // S3 업로드 및 URL 반환
            String urlPath = s3Service.uploadMultipartFile(returnAuthentication.getReturnImage(), fileName);
//            photoUrlPath.add(urlPath);
//        }

        Query query = new Query(Criteria.where("_id").is(new ObjectId(returnAuthentication.getPostId())));


//        Update update = new Update()
//                .set("borrowerInfo.$[elem].returnImage", photoUrlPath)
//                .set("borrowerInfo.$[elem].state", 0); // 예: 반납 상태로 변경
        Update update = new Update();

        update.set("borrowerInfo.$[borrowerInfo].state", 2);
        update.push("borrowerInfo.$[borrowerInfo].returnImage", urlPath);
        update.filterArray("borrowerInfo.userId", new ObjectId(returnAuthentication.getBorrowerId()));
// 실행
        mongoTemplate.updateFirst(query, update, PostEntity.class);

        return new ResponseEntity<>("Return success",HttpStatus.OK);
    }

    public ResponseEntity<Object> allowLent(String borrowerId, String postId){
        //state(0 채팅방,1 빌려준거(startDate,endDate, 2 반납한것(반납 이미지))

        PostEntity post = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(new ObjectId(postId))), PostEntity.class);
        LocalDateTime startDate = LocalDateTime.now().plusHours(9);
        LocalDateTime endDate = startDate; // 기본값

        String period = post.getPriceByPeriod();  // 예: "1일", "2개월", "3주"

// 숫자 + 단위를 분리
        String numberPart = period.replaceAll("[^0-9]", "");  // 숫자만
        String unitPart = period.replaceAll("[0-9]", "");     // 단위만

        int value = Integer.parseInt(numberPart);

        switch (unitPart) {
            case "일":
                endDate = startDate.plusDays(value);
                break;
            case "주":
                endDate = startDate.plusWeeks(value);
                break;
            case "개월":
            case "달":
                endDate = startDate.plusMonths(value);
                break;
            case "시간":
                endDate = startDate.plusHours(value);
                break;
            default:
                // 예외 처리 또는 기본값 유지
                break;
        }

        Update update = new Update();
        update.set("borrowerInfo.$[borrowerInfo].state", 1);
        update.set("borrowerInfo.$[borrowerInfo].endDate", endDate);
        update.set("borrowerInfo.$[borrowerInfo].startDate", startDate);
        update.filterArray("borrowerInfo.userId", new ObjectId(borrowerId));


        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(new ObjectId(postId))),
                update,
                PostEntity.class
        );


        return new ResponseEntity<Object>(HttpStatus.OK);
    }

    //채팅방 만들기
    public ResponseEntity<Document> makeChatRoom(String postId, String borrowerId){
        //participants List 만들기
        PostEntity post = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(new ObjectId(postId))), PostEntity.class);
        UserEntity borrower = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(new ObjectId(borrowerId))), UserEntity.class);
        LocalDateTime dateTime = LocalDateTime.now().plusHours(9L);
        Update update = new Update();
        ObjectId oldObjectId = new ObjectId();

        update.push("borrowerInfo",
                PostEntity.BorrowerInfo.builder()
                        .state(0)
                        .userId(new ObjectId(borrowerId))
                        .startDate(dateTime)
                        .endDate(dateTime)
                        .createdAt(dateTime)
                        .returnImage(new ArrayList<String>())
                        .build()
        );

        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(new ObjectId(postId))), update, PostEntity.class);
        List<Document> documents = Arrays.asList(
                new Document("$match",
                        new Document(
                                "_id", new ObjectId(post.getId())
                        )
                ),
                this.borrowerLookupPipeline(),
                this.userLookupPipeline(),
                this.unwindPipeline(),
                this.postProjection()
        );
        Document postDoc = mongoTemplate.getCollection("posts").aggregate(documents).first();
        assert postDoc != null;
        String jsonString = postDoc.toJson();

        RoomEntity chatRoom = RoomEntity.builder()
                .description("")
                .content("")
                .createdAt(dateTime)
                .image("")
                .type("A") // 예: private, group 등
                .imageURL("")
                .postInformation(jsonString)
                .brokenRoom(false)
                .participants(List.of(
                        RoomEntity.Participant.builder()
                                .userId(post.getWriterId())
                                .name(post.getName())
                                .recentMessageId(new ObjectId())
                                .enteredDate(dateTime)
                                .notificationStatus(true)
                                .profileImage("")
                                .profileImageURL("")
                                .activated(true)
                                .build(),
                        RoomEntity.Participant.builder()
                                .userId(new ObjectId(borrowerId))
                                .name(borrower.getName())
                                .recentMessageId(new ObjectId())
                                .profileImage("")
                                .profileImageURL("")
                                .enteredDate(dateTime)
                                .notificationStatus(true)
                                .activated(true)
                                .build()
                ))
                .build();

        RoomEntity insertedRoom = mongoTemplate.insert(chatRoom);

        mongoTemplate.insert(ChatEntity.builder()
                        .roomId(new ObjectId(insertedRoom.getId()))
                        .uid(UUID.randomUUID().toString())
                        .senderId(new ObjectId("6600a1234bcf123456789100"))
                        .type("notice")
                        .content("방이 생성 되었어요.")
                        .createdAt(dateTime)
                .build());

        return new ResponseEntity<>(new Document("roomId", insertedRoom.getId()), HttpStatus.OK);
    }

//    private final AmazonS3 s3Client;

//    @Value("${cloud.aws.s3.bucket}")
//    private String bucketName;

    // viewCount를 올려주는 함수
    public ResponseEntity<Object> increaseViewCount(String postId){

        Query query = new Query(Criteria.where("_id").is(new ObjectId(postId)));
        Update update = new Update().inc("viewCount", 1); // viewCount를 1 증가
        mongoTemplate.updateFirst(query, update, PostEntity.class);

        return new ResponseEntity<>("View count increased", HttpStatus.OK);
    }

    /*
    만약 게시물에 좋아요를 누르면 누른 사람 id가 DB에 들어가도록
    */
    public ResponseEntity<Object> sendPostLikeSwitch(String userId, String postId, boolean state) {
        Query query = new Query(Criteria.where("_id").is(new ObjectId(postId)));

        Update update = new Update();
        if (state) update.pull("likes", userId);  // 제거
        else update.addToSet("likes").value(userId); // 추가
        mongoTemplate.updateFirst(query, update, PostEntity.class);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /*
    학교 코드에 따라 전체 게시물을 보여주는 함수
    viewCount 추가
    priceByPeriod 추가
    */
    private Document postProjection() {
        return new Document("$project",
                new Document("_id", 0L)
                        // 기본 필드 변환
                        .append("id", new Document("$toString", "$_id"))
                        .append("writerId", new Document("$toString", "$writerId"))
                        .append("createdAt", new Document("$dateToString",
                                new Document("format", Constants.DATE_FORMAT)
                                        .append("date", "$createdAt")))
                        .append("title", 1L)
                        .append("name", "$user.name")
                        .append("mannerValue", "$user.mannerValue")
                        .append("content", 1L)
                        .append("schoolCode", 1L)
                        .append("photos", 1L)
                        .append("likes", 1L)
                        .append("tags", 1L)
                        .append("geoInfo", 1L)
                        .append("price", 1L)
                        .append("availableDates", 1L)
                        // borrowerInfo 배열 내부 문서 처리: 기존 변환 + lookup으로 가져온 사용자 정보 병합
                        .append("borrowerInfo",
                                new Document("$map",
                                        new Document("input", "$borrowerInfo")
                                                .append("as", "bi")
                                                .append("in", new Document("$mergeObjects", Arrays.asList(
                                                        // 기존 borrowerInfo 필드 변환
                                                        new Document("userId", new Document("$toString", "$$bi.userId"))
                                                                .append("state", "$$bi.state")
                                                                .append("createdAt", new Document("$dateToString",
                                                                        new Document("format", Constants.DATE_FORMAT)
                                                                                .append("date", "$$bi.createdAt")))
                                                                .append("startDate", new Document("$dateToString",
                                                                        new Document("format", Constants.DATE_FORMAT)
                                                                                .append("date", "$$bi.startDate")))
                                                                .append("endDate", new Document("$dateToString",
                                                                        new Document("format", Constants.DATE_FORMAT)
                                                                                .append("date", "$$bi.endDate")))
                                                                .append("returnImage", "$$bi.returnImage"),
                                                        // lookup으로 가져온 사용자 정보 병합
                                                        new Document("$arrayElemAt", Arrays.asList(
                                                                new Document("$filter",
                                                                        new Document("input", "$borrowers")
                                                                                .append("as", "user")
                                                                                .append("cond", new Document("$eq",
                                                                                        Arrays.asList("$$user._id", "$$bi.userId"))
                                                                                )
                                                                ),
                                                                0
                                                        ))
                                                )))
                                )
                        )
                        .append("rentalType", 1L)
                        .append("profileImage", "$user.profileImage")
                        .append("itemType", 1L)
                        .append("viewCount", 1L)
                        .append("priceByPeriod", 1L)
//                        .append("borrowers", 0L)
        );
    }

    public Document borrowerLookupPipeline() {
        return new Document("$lookup", new Document("from", "users")
                .append("let", new Document("borrowerIds", "$borrowerInfo.userId"))
                .append("pipeline", Arrays.asList(
                        new Document("$match", new Document("$expr",
                                new Document("$in", Arrays.asList("$_id", "$$borrowerIds"))
                        )),
                        new Document("$project",
                                new Document("name", 1L)
                                .append("profileImage", 1L)
                                .append("mannerValue", 1L)
                                .append("schoolCode", 1L))
                ))
                .append("as", "borrowers")
        );
    }

    public Document userLookupPipeline() {
        return new Document("$lookup", new Document("from", "users")
                .append("localField", "writerId")
                .append("foreignField", "_id")
                .append("as", "user")
        );
    }
    public Document unwindPipeline() {
        return new Document("$unwind", "$user");
    }

    public List<Document> getAllPost(String schoolCode, String query) {
        List<Document> documents = new ArrayList<>();
        if (!query.equals("")) {
            documents = Arrays.asList(
                    new Document("$search",
                            new Document("index", "search")
                                    .append("text", new Document(
                                                    "query", query
                                            )
                                                    .append("path", new Document("wildcard", "*"))

                                    )
                    ),
                    this.borrowerLookupPipeline(),
                    this.userLookupPipeline(),
                    this.unwindPipeline(),
                    this.postProjection()
            );
        }
        else {
            documents = Arrays.asList(
                    new Document("$match",
                            new Document(
                                    "schoolCode", schoolCode
                            )
                    ),
                    this.borrowerLookupPipeline(),
                    this.userLookupPipeline(),
                    this.unwindPipeline(),
                    this.postProjection()
            );
        }
        AggregateIterable<Document> iterable = mongoTemplate.getCollection("posts").aggregate(documents);

        List<Document> results = new ArrayList<>();
        for (Document document : iterable) {
            results.add(document);
        }
        return results;
    }

    /*
    게시물을 업로드해주는 함수
    viewCount는 upload시 기본 0 으로 초기화 시켜줌
    priceByPeriod 추가
    이미지 업로드 기능 추가 - 새로운 Request 객체를 만들지 않고 그냥 구현
     */
    public ResponseEntity<Document> upload(PostUploadReceive post) {
        try {
            UserEntity userEntity = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(new ObjectId(post.getWriterId()))), UserEntity.class);

            List<String> photoUrlPath = new ArrayList<>(); // 리스트 초기화

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(MediaType.IMAGE_JPEG_VALUE);

            for (MultipartFile photo : post.getProductImage()) {
                metadata.setContentLength(photo.getSize());

                // 파일명: writerId + 현재시간 + 확장자
                String fileName = post.getWriterId()
                        + "_" + System.currentTimeMillis() + ".jpeg";

                // S3 업로드 및 URL 반환
                String urlPath = s3Service.uploadMultipartFile(photo, fileName); // 메서드 시그니처에 따라 다름
                photoUrlPath.add(urlPath);
            }

            PostEntity postEntity = PostEntity.builder()
                    .writerId(new ObjectId(post.getWriterId()))
                    .title(post.getTitle())
                    .content(post.getContents())
                    .schoolCode(userEntity.getSchoolCode())//찾아야함
                    .photos(photoUrlPath)
                    .likes(new ArrayList<String>())//Null
                    .tags(post.getTags())
                    .viewCount(0) //viewCount 추가
                    .geoInfo(userEntity.getGeoInfo())  // ← 위에서 수동 변환한 값
                    .price(post.getPrice())
                    .availableDates(post.getAvailableDates())
                    .borrowerInfo(new ArrayList<PostEntity.BorrowerInfo>())  // ← 만들어줘야함
                    .rentalType(post.getRentalType())
                    .itemType(post.getItemType())
                    .createdAt(LocalDateTime.now().plusHours(9))
                    .priceByPeriod(post.getPriceByPeriod())
                    .name(userEntity.getName())
                    .rentalType(post.getRentalType())
                    .build();

            mongoTemplate.insert(postEntity);
            System.out.println(">>> inserting post: " + post.getTitle());

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /*
    유저가 빌려준 것 확인
     */
    public List<Document> getItemsLentByUser(String userId) {

        List<Document> documents = Arrays.asList(
                new Document("$match",
                        new Document(
                                "writerId", new ObjectId(userId)
                        )
                ),
                this.borrowerLookupPipeline(),
                this.userLookupPipeline(),
                this.unwindPipeline(),
                this.postProjection()
        );

        AggregateIterable<Document> iterable = mongoTemplate.getCollection("posts").aggregate(documents);

        List<Document> results = new ArrayList<>();
        for (Document document : iterable) {
            results.add(document);
        }
        return results;

    }
/*
유저가 빌린 것 확인
 */

    public List<Document> getItemsBorrowedByUser(String userId) {
        List<Document> documents = Arrays.asList(new Document("$match",
                        new Document(
                                "borrowerInfo.userId", new ObjectId(userId)
                        )
                ),
                this.borrowerLookupPipeline(),
                this.userLookupPipeline(),
                this.unwindPipeline(),
                this.postProjection()
        );

        AggregateIterable<Document> iterable = mongoTemplate.getCollection("posts").aggregate(documents);

        List<Document> results = new ArrayList<>();
        for (Document document : iterable) {
            results.add(document);
        }
        return results;
    }

}


