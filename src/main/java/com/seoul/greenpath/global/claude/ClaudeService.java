package com.seoul.greenpath.global.claude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ClaudeService {

    @Value("${anthropic.api-key:mock-key}")
    private String apiKey;

    @Value("${anthropic.model:claude-3-5-sonnet-20240620}")
    private String model;

    @Value("${anthropic.mock:false}")
    private boolean mockClaude;

    private final RestTemplate restTemplate = new RestTemplate();
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public List<ReRankingResult> reRankCourses(String userPreference, List<CourseInfo> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<>();
        }

        if (mockClaude) {
            log.info("🔥 Claude Mocking activated. Generating dummy re-ranking results.");
            List<ReRankingResult> mockResults = new ArrayList<>();
            for (int i = 0; i < Math.min(3, candidates.size()); i++) {
                mockResults.add(new ReRankingResult(candidates.get(i).getId(), "사용자의 취향에 맞는 조용한 코스입니다. (Mock)"));
            }
            return mockResults;
        }

        String url = "https://api.anthropic.com/v1/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        String prompt = buildPrompt(userPreference, candidates);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", 1024);
        
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        body.put("messages", messages);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            if (response != null && response.containsKey("content")) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
                if (!content.isEmpty()) {
                    String textResponse = (String) content.get(0).get("text");
                    return parseResponse(textResponse);
                }
            }
        } catch (Exception e) {
            log.error("Error calling Claude API: {}", e.getMessage());
        }

        // 실패 시 상위 3개 그대로 반환
        List<ReRankingResult> fallback = new ArrayList<>();
        for (int i = 0; i < Math.min(3, candidates.size()); i++) {
            fallback.add(new ReRankingResult(candidates.get(i).getId(), "사용자에게 추천하는 최적의 코스입니다."));
        }
        return fallback;
    }

    private String buildPrompt(String userPreference, List<CourseInfo> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 서울의 산책 코스를 추천해주는 AI 가이드입니다.\n");
        sb.append("사용자의 선호도와 후보 코스 리스트를 바탕으로 가장 적합한 Top 3 코스를 선정하고 추천 이유를 작성해주세요.\n\n");
        sb.append("사용자 선호도: ").append(userPreference).append("\n\n");
        sb.append("후보 코스 목록:\n");
        for (CourseInfo course : candidates) {
            sb.append("- ID: ").append(course.getId())
              .append(", 제목: ").append(course.getTitle())
              .append(", 설명: ").append(course.getDescription())
              .append("\n");
        }
        sb.append("\n응답은 반드시 아래 JSON 형식으로만 작성하세요. 마크다운 기호 없이 JSON 배열만 출력하세요.\n");
        sb.append("[\n  {\"id\": 1, \"reason\": \"추천 이유\"},\n  {\"id\": 2, \"reason\": \"추천 이유\"}\n]");
        
        return sb.toString();
    }

    private List<ReRankingResult> parseResponse(String textResponse) {
        log.info("Claude Response: {}", textResponse);
        try {
            // 마크다운 블록이 포함된 경우 제거
            String jsonContent = textResponse.replaceAll("```json|```", "").trim();
            return objectMapper.readValue(jsonContent, new com.fasterxml.jackson.core.type.TypeReference<List<ReRankingResult>>() {});
        } catch (Exception e) {
            log.error("JSON Parsing Error: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseInfo {
        private Long id;
        private String title;
        private String description;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReRankingResult {
        private Long id;
        private String reason;
    }
}
