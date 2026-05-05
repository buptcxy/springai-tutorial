package com.example.springai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WeatherTools {

    private static final Map<String, String> WEATHER_DATA = new ConcurrentHashMap<>();

    static {
        WEATHER_DATA.put("北京", "晴天，温度 18°C，空气质量良好");
        WEATHER_DATA.put("上海", "多云，温度 22°C，湿度 65%");
        WEATHER_DATA.put("深圳", "小雨，温度 26°C，湿度 80%");
        WEATHER_DATA.put("成都", "阴天，温度 15°C，湿度 70%");
        WEATHER_DATA.put("杭州", "晴天，温度 20°C，空气质量优");
    }

    @Tool(description = "查询指定城市的当前天气信息，包括温度、天气状况和湿度等")
    public String getCurrentWeather(@ToolParam(description = "城市名称，如北京、上海、深圳") String city) {
        String weather = WEATHER_DATA.getOrDefault(city, "暂无该城市的天气数据，可查询：北京、上海、深圳、成都、杭州");
        return city + "的天气：" + weather;
    }

    @Tool(description = "获取当前的日期和时间")
    public String getCurrentDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
