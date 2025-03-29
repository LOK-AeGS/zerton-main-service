package com.zeroton.zeroSpring.data.dto.request;

import com.zeroton.zeroSpring.data.entity.PostEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

//이전에 Dto를 만들고 class를 조금 나눠서 설계했더라면 좋았을 텐데...
//지금 이 객체는 postEntity와 기능적으로 유사해
//그대로 쓰는데 대신 URL이 들어가는 부분만 사용 X
//사용 X
@Getter
@Setter
public class PostUploadReceive {
    private String writerId;
    private String title;
    private String contents;
    private List<String> tags;
    //지오 인포, name 필요 없슴 user에 있음 다른 곳(userField)에서 가져올 수 있는건 다 빼
    private int price;
    private List<Integer> availableDates;

    private String rentalType; // 무료나눔인지, 빌려주는건지, 장기렌트, 단기렌트
    private String itemType;

    private String priceByPeriod; // 빌릴 수 있는 기간 ? 형이 priceType으로 만들어달라 하신 것

    private List<MultipartFile> productImage;
}
