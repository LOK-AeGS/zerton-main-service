package com.zeroton.zeroSpring.data.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class ReturnAuthentication {

    private String postId;
    private String borrowerId;
    private MultipartFile returnImage;

}
