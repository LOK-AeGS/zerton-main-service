package com.zeroton.zeroSpring.controller;


import com.amazonaws.Response;
import com.zeroton.zeroSpring.data.dto.request.PostUploadReceive;
import com.zeroton.zeroSpring.data.dto.request.ReturnAuthentication;
import com.zeroton.zeroSpring.data.entity.PostEntity;
import com.zeroton.zeroSpring.service.PostService;
import com.zeroton.zeroSpring.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/post") //게시물 내 게시물 리스트 보기, 전체 게시물(학교 코드만), 게시글 만들기
public class PostController {
    private final PostService postService;

    //
    /*
  @PutMapping("/modification")
    public ResponseEntity<Object> postModification(
            @RequestBody("po")
    ){
        return ;
    }
*/

    @GetMapping
    public ResponseEntity<Document> getPost(
            @RequestParam("postId") String postId
    ){
        return postService.getPost(postId);
    }
    @PutMapping(value = "/return", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> returnLent(
            @ModelAttribute("returnAuthentication") ReturnAuthentication returnAuthentication
    ) throws IOException {
        System.out.println(returnAuthentication.getReturnImage());
        System.out.println(returnAuthentication.getPostId());
        System.out.println(returnAuthentication.getBorrowerId());
    return postService.returnLent(returnAuthentication);
    }

    @PutMapping("/allow")
    public ResponseEntity<Object> allowLent(
            @RequestParam("borrowerId") String borrowerId,
            @RequestParam("postId") String postId
    ){
        return postService.allowLent(borrowerId,postId);
    }


    @PostMapping("/request")
    public ResponseEntity<Document> makeChatRoom(
            @RequestParam("postId") String postId,
            @RequestParam("borrowerId") String borrowerId
    ){
        return postService.makeChatRoom(postId, borrowerId);
    }


    //조회수 변경 요청
    @PutMapping("/show")
    public ResponseEntity<Object> increaseViewCount(
            @RequestParam("postId") String postId
    ){
        return postService.increaseViewCount(postId);
    }

//좋아요 요청이 올 시
    @PostMapping("/sendPostLikeSwitch")
    public ResponseEntity<Object> sendPostLikeSwitch(
            @RequestParam("userId") String userId,
            @RequestParam("postId") String postId,
            @RequestParam("state")Boolean state
            ){
        return postService.sendPostLikeSwitch(userId,postId,state);
    }

//학교 코드에 맞는 전체 게시물 보여주기
    @GetMapping("/index")
    public List<Document> getAllPost(
            @RequestParam("schoolCode") String schoolCode,
            @RequestParam("query") String query
    ) {
        return postService.getAllPost(schoolCode, query);
    }
//게시물 업로드
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Document> postUpload(
    //        @RequestParam("schoolCode") String schoolCode,
    //        @RequestParam("userId") String userId,
    //        @RequestPart("postEntity") HashMap<String,Object> jsonData,
            @ModelAttribute("post") PostUploadReceive post ) {
        return postService.upload(post);
    }
//유저가 빌려준 것 확인
    @GetMapping("/lent")
    public List<Document> getItemsLentByUser(
            @RequestParam("userId") String userId
    ) {
        return postService.getItemsLentByUser(userId);
    }
//유저가 빌림 받은 것 확인
    @GetMapping("/borrow")
    public List<Document> getItemsBorrowedByUser(
            @RequestParam("userId") String userId
    ) {
        return postService.getItemsBorrowedByUser(userId);
    }
}
