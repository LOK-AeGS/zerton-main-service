package com.zeroton.zeroSpring.service;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.mongodb.client.AggregateIterable;
import com.zeroton.zeroSpring.data.dto.UserDto;
import com.zeroton.zeroSpring.data.dto.request.ProfileReceive;
import com.zeroton.zeroSpring.data.entity.UserEntity;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserService {
    @Autowired
    private MongoTemplate mongoTemplate;

//    private String bucketName = "zeroton-rentree";
    @Autowired
    private S3Service s3Service;

    @Value("${api.career}")
    private String key;
    public Object setUniv(String userId,String schoolName) {

        Query query = new Query();
        query.addCriteria(
                Criteria.where("_id").is(new ObjectId(userId))
        );
        Update update = new Update();
        update.set("schoolCode", schoolName);

        mongoTemplate.updateFirst(query,update, UserEntity.class);

        return ResponseEntity.ok();
    }
    public JSONArray getUnivList(String input) {

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "application/x-www-form-urlencoded");

        HttpEntity<?> entity = new HttpEntity<>(httpHeaders);
        URI uri = URI.create("https://www.career.go.kr/cnet/openapi/getOpenApi?apiKey=" + key + "&svcType=api&svcCode=SCHOOL&contentType=json&gubun=univ_list&searchSchulNm=" + URLEncoder.encode(input, StandardCharsets.UTF_8));
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

        JSONObject dataSearch = new JSONObject(Objects.requireNonNull(response.getBody())).getJSONObject("dataSearch");
        JSONArray array = dataSearch.getJSONArray("content");
        JSONArray result = new JSONArray();

        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            JSONObject newItem = new JSONObject();
            newItem.put("schoolName", item.getString("schoolName"));
            newItem.put("address", item.getString("adres")); // 'adres'를 'address'로 변환
            result.put(newItem);
        }

        return result;

    }
    public ResponseEntity<Document> register(String name) {
        LocalDateTime ld = LocalDateTime.now().plusHours(9L);
        UserEntity user = UserEntity.builder()
                .name(name)
                .createdAt(ld)
                .build();
        UserEntity insertedUser = mongoTemplate.insert(user);
        return login(insertedUser.getId());
    }


    // 실제 환경에서는 JWT 든 뭐든 써서 토큰으로 로그인함
    public ResponseEntity<Document> login(String id) {
        List<Document> documents = Arrays.asList(new Document("$match",
                        new Document(
                                "_id", new ObjectId(id)
                        )
                ),
                new Document("$project",
                        new Document("_id", 0L)
                                .append("id",
                                        new Document("$toString", "$_id"))
                                .append("name", 1L)
                                .append("schoolCode", 1L)
                                .append("age", 1L)
                                .append("geoInfo", 1L)
                                .append("coordinates", 1L)
                                .append("monthTransactionCount", 1L)
                                .append("monthTransactionValue", 1L)
                                .append("mannerValue", 1L)
                                .append("profileImage", 1L)
                )
        );

        AggregateIterable<Document> iterable = mongoTemplate.getCollection("users").aggregate(documents);
        return new ResponseEntity<>(Objects.requireNonNull(iterable.first()), HttpStatus.OK);
    }

    public ResponseEntity<Document> resign(String id) {
        Query query = new Query();
        query.addCriteria(
                Criteria.where("_id").is(new ObjectId(id))
        );
        mongoTemplate.remove(
                query, UserEntity.class
        );
        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity<Document> update(String id, String newName) {
        Query query = new Query();
        query.addCriteria(
                Criteria.where("_id").is(new ObjectId(id))
        );
        Update update = new Update();
        update.set("name", newName);
        mongoTemplate.updateFirst(query, update, UserEntity.class);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity<String> registerUserProfile(ProfileReceive data) {
        String result;
        if (data.getProfile() != null) {
            MultipartFile file = data.getProfile();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(MediaType.IMAGE_JPEG_VALUE);
            metadata.setContentLength(file.getSize());
            String fileName = data.getUserId() + System.currentTimeMillis() + ".jpeg";

            try {
                //URL 반환
                result = s3Service.uploadMultipartFile(file, fileName);

                Update update = new Update();
                update.set("profileImage", result);
                mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(new ObjectId(data.getUserId()))), update, UserEntity.class);
                return ResponseEntity.ok(result);
            } catch (Exception ignored) {
                return ResponseEntity.badRequest().body("");
            }
        }
        return ResponseEntity.badRequest().body("");
    }
}
