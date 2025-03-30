package com.favicon.star;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
class SlackNotifier {
    @Value("${slack.webhook.url}")
    private String slackWebhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendMessage(String message) {
        Map<String, String> request = new HashMap<>();
        request.put("text", message);
        restTemplate.postForEntity(slackWebhookUrl, request, String.class);
    }
}