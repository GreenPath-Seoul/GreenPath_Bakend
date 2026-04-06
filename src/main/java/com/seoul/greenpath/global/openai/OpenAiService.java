package com.seoul.greenpath.global.openai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OpenAiService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.embedding-model}")
    private String embeddingModel;

    private final RestTemplate restTemplate = new RestTemplate();

    public float[] getEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[1536];
        }

        String url = "https://api.openai.com/v1/embeddings";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("input", text);
        body.put("model", embeddingModel);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                if (!data.isEmpty()) {
                    List<Double> embeddingList = (List<Double>) data.get(0).get("embedding");
                    float[] result = new float[embeddingList.size()];
                    for (int i = 0; i < embeddingList.size(); i++) {
                        result[i] = embeddingList.get(i).floatValue();
                    }
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("Error calling OpenAI Embedding API: {}", e.getMessage());
        }

        return new float[1536];
    }
}
