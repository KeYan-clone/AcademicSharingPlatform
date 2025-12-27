package com.scholar.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class TranslationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");
    private static final String TRANSLATION_API_URL = "https://api.mymemory.translated.net/get?q={text}&langpair=zh|en";

    public boolean containsChinese(String text) {
        return StringUtils.hasText(text) && CHINESE_PATTERN.matcher(text).find();
    }

    public String translateToEnglish(String text) {
        if (!containsChinese(text)) {
            return text;
        }

        try {
            String url = TRANSLATION_API_URL.replace("{text}", text);
            String response = restTemplate.getForObject(url, String.class);
            
            JsonNode root = objectMapper.readTree(response);
            String translatedText = root.path("responseData").path("translatedText").asText();
            
            if (StringUtils.hasText(translatedText)) {
                log.info("Translated '{}' to '{}'", text, translatedText);
                return translatedText;
            }
        } catch (Exception e) {
            log.error("Translation failed for text: {}", text, e);
        }
        
        return text;
    }
}
