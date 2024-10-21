package com.exed1ons.localnewsdeliverer.service.impl;

import com.exed1ons.localnewsdeliverer.service.LLMRequestCityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LLMRequestCityServiceImpl implements LLMRequestCityService {

    private static final Logger logger = LoggerFactory.getLogger(LLMRequestCityServiceImpl.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${llm.api.url}")
    private String apiUrl;
    @Value("${llm.system.prompt}")
    private String systemPrompt;
    @Value("${llm.model}")
    private String model;

    @Value("#{'${llm.api.keys}'.split(',')}")
    private List<String> apiKeys;

    private final AtomicInteger apiKeyIndex = new AtomicInteger(0);

    @Override
    public List<String> requestCity(String description) {
        logger.info("Sending message to LLM API with description: {}", description);
        String response = sendMessageToLLM(description);
        logger.info("Received response from LLM API: {}", response);
        if (!response.equalsIgnoreCase("global")) {
            return Stream.of(response.split(";"))
                    .map(String::trim)
                    .toList();
        }
        return null;
    }

    public String sendMessageToLLM(String description) {
        logger.info("Creating request entity to send to LLM API.");
        HttpEntity<String> request = createRequestEntity(description);

        logger.info("Sending request to LLM API at {}", apiUrl);
        return processApiResponse(sendApiRequest(request));
    }

    private HttpEntity<String> createRequestEntity(String description) {
        logger.info("Building the JSON payload for the request.");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json; charset=utf-8");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", description));

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", messages,
                "max_tokens", 100
        );

        try {
            logger.debug("Serialized request body: {}", objectMapper.writeValueAsString(requestBody));
            return new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize request body", e);
            throw new RuntimeException("Error creating JSON request body", e);
        }
    }

    private ResponseEntity<String> sendApiRequest(HttpEntity<String> originalRequest) {
        int attempts = 0;
        int retryAfterAllKeysExhausted = 60_000;
        int maxRetriesPerKey = 3;

        while (true) {
            try {
                String currentApiKey = apiKeys.get(apiKeyIndex.get());

                HttpHeaders updatedHeaders = new HttpHeaders();
                updatedHeaders.addAll(originalRequest.getHeaders());
                updatedHeaders.set("Authorization", "Bearer " + currentApiKey);

                HttpEntity<String> updatedRequest = new HttpEntity<>(originalRequest.getBody(), updatedHeaders);

                logger.info("Executing API request to LLM using API key index {}", apiKeyIndex.get());
                return restTemplate.exchange(apiUrl, HttpMethod.POST, updatedRequest, String.class);
            } catch (HttpClientErrorException.TooManyRequests e) {
                logger.warn("Rate limit reached for API key at index {}, attempt {}. Switching to next key...", apiKeyIndex.get(), attempts + 1);
                apiKeyIndex.set((apiKeyIndex.get() + 1) % apiKeys.size());
                attempts++;

                if (attempts >= maxRetriesPerKey * apiKeys.size()) {
                    logger.warn("All API keys exhausted. Waiting for {} seconds before retrying...", retryAfterAllKeysExhausted / 1000);
                    try {
                        Thread.sleep(retryAfterAllKeysExhausted);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Thread interrupted while waiting", ie);
                    }
                    attempts = 0;
                }
            } catch (HttpClientErrorException.BadRequest e) {
                logger.error("Bad request sent to API: {}. Payload: {}", e.getResponseBodyAsString(), e.getResponseBodyAsString());
                throw new RuntimeException("Bad request sent to API", e);
            } catch (Exception e) {
                logger.error("Exception occurred during API request: {}", e.getMessage(), e);
                throw new RuntimeException("API request failed", e);
            }
        }
    }

    private String processApiResponse(ResponseEntity<String> response) {
        logger.info("Processing the API response.");
        try {
            Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);
            String assistantMessage = extractMessageFromResponse(responseBody);
            logger.info("Successfully extracted message from API response.");
            return assistantMessage;
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse API response", e);
            throw new RuntimeException("Failed to process API response", e);
        }
    }

    private String extractMessageFromResponse(Map<String, Object> responseBody) {
        logger.debug("Extracting message content from the response body.");
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");

        if (choices == null || choices.isEmpty()) {
            logger.error("No choices found in the API response.");
            throw new RuntimeException("No choices found in response");
        }

        Map<String, Object> choice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) choice.get("message");

        if (message == null || !message.containsKey("content")) {
            logger.error("Message content missing in the response.");
            throw new RuntimeException("Message content is missing");
        }

        return (String) message.get("content");
    }
}
