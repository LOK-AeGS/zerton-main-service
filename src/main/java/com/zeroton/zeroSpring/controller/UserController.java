package com.zeroton.zeroSpring.controller;


import com.zeroton.zeroSpring.data.dto.request.ProfileReceive;
import com.zeroton.zeroSpring.service.UserService;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONArray;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    @PostMapping("/profile")
    public ResponseEntity<String> registerUserProfile(@ModelAttribute ProfileReceive data) {
//        String result = userService.registerUserProfile(data);
//        if (result != null) return new ResponseEntity<>(result, HttpStatus.OK);
//        else return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        return userService.registerUserProfile(data);
    }

    @GetMapping("/univ/list")
    public ResponseEntity<List<Object>> getUnivList(
            @RequestParam("query") String query
    ) {
        JSONArray result = userService.getUnivList(query);
        return new ResponseEntity<>(result.toList(), HttpStatus.OK);
    }

    @PostMapping("/univ")
    public Object setUniv(
            @RequestParam("schoolName") String schoolName,
            @RequestParam("userId") String userId
    ) {
        return userService.setUniv(userId, schoolName);
    }

    @PostMapping("/register")
    public ResponseEntity<Document> register(
            @RequestParam("name") String name
    ) {
        return userService.register(name);
    }


    @GetMapping("/login")
    public ResponseEntity<Document> login(
            @RequestParam("userId") String id
    ) {
        return userService.login(id);
    }

    @DeleteMapping("/resign")
    public ResponseEntity<Document> resign(
            @RequestParam("id") String id
    ) {

        return userService.resign(id);
    }

    @PutMapping("/name")
    public ResponseEntity<Document> updateName(
            @RequestParam("name") String name,
            @RequestParam("id") String id
    ) {

        return userService.update(id, name);
    }

}
