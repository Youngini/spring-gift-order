package gift.service;

import gift.domain.Option;
import gift.dto.OrderResponse;
import gift.exception.ForbiddenException;
import gift.exception.RestTemplateResponseErrorHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class KakaoTokenService {

    @Value("${kakao.app.key}")
    private String appKey;

    private final String tokenUrl = "https://kauth.kakao.com/oauth/token";
    private final RestTemplate restTemplate;
    private final OptionService optionService;

    @Autowired
    public KakaoTokenService(RestTemplateBuilder restTemplateBuilder, RestTemplateResponseErrorHandler errorHandler, OptionService optionService) {
        this.optionService = optionService;
        this.restTemplate = restTemplateBuilder
                .errorHandler(errorHandler) // 사용자 정의 오류 처리기 주입
                .build();
    }

    public String getAccessToken(String authorizationCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", appKey);
        params.add("redirect_uri", "http://localhost:8080/callback");
        params.add("code", authorizationCode);

        HttpEntity<LinkedMultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, String.class);
            return response.getBody(); // Access Token 반환
        } catch (ForbiddenException e) {
            System.err.println("엑세스토큰 처리시, 접근이 거부되었습니다: " + e.getMessage());
            return null;
        } catch (HttpClientErrorException e) {
            System.err.println("엑세스토큰 처리시, 클라이언트 오류가 발생했습니다: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("엑세스토큰 처리시, 문제가 발생했습니다: " + e.getMessage());
            return null;
        }
    }

    public void sendKakaoMessage(OrderResponse orderResponse) {
        String url = "https://kapi.kakao.com/v2/api/talk/memo/default/send"; // 카카오톡 메시지 전송 API URL
        String token = "Bearer {token}"; // 실제 토큰으로 교체 어떤식으로 할 지 생각하기 (어디에 저장할까?)
        Option option = optionService.getOption(orderResponse.getOptionId());
        String messageTemplate = String.format(
                "주문이 완료되었습니다! \n옵션: %s\n수량: %d\n주문 시간: %s\n메시지: %s",
                option.getName(), orderResponse.getQuantity(), orderResponse.getOrderDateTime(), orderResponse.getMessage()
        );

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("template_object", createKakaoTemplate(messageTemplate));

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
        restTemplate.postForEntity(url, requestEntity, String.class);
    }

    private String createKakaoTemplate(String message) {
        return String.format("{\"object_type\":\"text\",\"text\":\"%s\",\"link\":{\"web_url\":\"http://yourwebsite.com\"}}", message);
    }
}

