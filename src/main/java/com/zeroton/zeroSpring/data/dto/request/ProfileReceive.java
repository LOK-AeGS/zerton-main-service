package com.zeroton.zeroSpring.data.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ProfileReceive {
    private String userId;
    private MultipartFile profile;
}

