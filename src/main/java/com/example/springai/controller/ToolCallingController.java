package com.example.springai.controller;

import com.example.springai.tools.WeatherTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Function;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/tool")
public class ToolCallingController {

    private static final Logger log = LoggerFactory.getLogger(ToolCallingController.class);

    private final ChatClient chatClient;

    public ToolCallingController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/annotation")
    public String toolByAnnotation(@RequestParam(defaultValue = "北京今天天气怎么样") String message) {
        log.info("[工具调用-@Tool] 收到请求 - 用户消息: {}", message);
        long startTime = System.currentTimeMillis();

        String result = chatClient.prompt()
                .user(message)
                .tools(new WeatherTools())
                .call()
                .content();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[工具调用-@Tool] 请求完成 - 耗时: {}ms, 回复长度: {}字符", elapsed, result != null ? result.length() : 0);
        return result;
    }

    @GetMapping("/callback")
    public String toolByCallback(@RequestParam(defaultValue = "上海和深圳哪个城市更热") String message) {
        log.info("[工具调用-Callback] 收到请求 - 用户消息: {}", message);
        long startTime = System.currentTimeMillis();

        try {
            Function<Map<String, Object>, String> weatherFunction = args -> {
                String city = String.valueOf(args.getOrDefault("city", args.values().iterator().next())).trim();
                String[] cities = {"北京", "上海", "深圳", "成都", "杭州"};
                String[] weathers = {
                        "晴天，温度 18°C",
                        "多云，温度 22°C",
                        "小雨，温度 26°C",
                        "阴天，温度 15°C",
                        "晴天，温度 20°C"
                };
                for (int i = 0; i < cities.length; i++) {
                    if (city.contains(cities[i])) {
                        return cities[i] + "的天气：" + weathers[i];
                    }
                }
                return "暂无" + city + "的天气数据";
            };

            var weatherTool = FunctionToolCallback.builder("getCurrentWeather", weatherFunction)
                    .description("查询指定城市的当前天气信息，输入城市名称返回天气描述")
                    .inputType(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .build();

            String result = chatClient.prompt()
                    .user(message)
                    .toolCallbacks(weatherTool)
                    .call()
                    .content();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[工具调用-Callback] 请求完成 - 耗时: {}ms, 回复长度: {}字符", elapsed, result != null ? result.length() : 0);
            return result;
        } catch (Exception e) {
            log.error("[工具调用-Callback] 请求失败", e);
            return "错误: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }
}
