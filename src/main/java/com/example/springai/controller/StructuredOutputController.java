package com.example.springai.controller;

import com.example.springai.model.MovieInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/structured")
public class StructuredOutputController {

    private static final Logger log = LoggerFactory.getLogger(StructuredOutputController.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StructuredOutputController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/entity")
    public MovieInfo entityOutput(@RequestParam(defaultValue = "推荐一部经典的科幻电影") String message) {
        log.info("[结构化输出-Entity] 收到请求 - 用户消息: {}", message);
        long startTime = System.currentTimeMillis();

        BeanOutputConverter<MovieInfo> converter = new BeanOutputConverter<>(MovieInfo.class);

        String content = chatClient.prompt()
                .user(u -> u.text(message + "。{format}")
                        .param("format", converter.getFormat()))
                .call()
                .content();

        MovieInfo result = converter.convert(content);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[结构化输出-Entity] 请求完成 - 耗时: {}ms, 结果: {}", elapsed, result);
        return result;
    }

    @GetMapping("/list")
    public List<String> listOutput(@RequestParam(defaultValue = "列出5种流行的编程语言") String message) {
        log.info("[结构化输出-List] 收到请求 - 用户消息: {}", message);
        long startTime = System.currentTimeMillis();

        BeanOutputConverter<List<String>> converter = new BeanOutputConverter<>(
                new ParameterizedTypeReference<List<String>>() {});

        String content = chatClient.prompt()
                .user(u -> u.text(message + "。{format}")
                        .param("format", converter.getFormat()))
                .call()
                .content();

        List<String> result = converter.convert(content);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[结构化输出-List] 请求完成 - 耗时: {}ms, 结果数量: {}", elapsed, result != null ? result.size() : 0);
        return result;
    }

    @GetMapping("/map")
    public Object mapOutput(@RequestParam(defaultValue = "描述Python编程语言的特点") String message) {
        log.info("[结构化输出-Map] 收到请求 - 用户消息: {}", message);
        long startTime = System.currentTimeMillis();

        String content = chatClient.prompt()
                .user(message + "。请以JSON对象格式返回，键为特性名称，值为描述。只返回JSON，不要其他文字。")
                .call()
                .content();

        Map<String, Object> result;
        try {
            String json = content;
            if (content.contains("```json")) {
                json = content.substring(content.indexOf("```json") + 7, content.lastIndexOf("```")).trim();
            } else if (content.contains("```")) {
                json = content.substring(content.indexOf("```") + 3, content.lastIndexOf("```")).trim();
            }
            result = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("[结构化输出-Map] JSON解析失败，尝试BeanOutputConverter: {}", e.getMessage());
            BeanOutputConverter<Map<String, Object>> converter = new BeanOutputConverter<>(
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            result = converter.convert(content);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[结构化输出-Map] 请求完成 - 耗时: {}ms, 结果键数量: {}", elapsed, result != null ? result.size() : 0);
        return result;
    }
}
