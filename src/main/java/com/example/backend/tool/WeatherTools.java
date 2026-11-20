package com.example.backend.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

@Slf4j
@Component
public class WeatherTools {

    private static final Random RANDOM = new Random();

    private static final Map<String, String[]> CITY_WEATHER = Map.of(
            "北京", new String[]{"晴朗", "多云", "阴天", "小雨"},
            "上海", new String[]{"多云", "小雨", "阴天", "晴朗"},
            "广州", new String[]{"多云", "雷阵雨", "晴朗", "阴天"},
            "深圳", new String[]{"晴朗", "多云", "阵雨", "多云"},
            "杭州", new String[]{"小雨", "阴天", "多云", "晴朗"},
            "成都", new String[]{"阴天", "小雨", "多云", "阴天"},
            "武汉", new String[]{"多云", "晴朗", "小雨", "阴天"},
            "西安", new String[]{"晴朗", "多云", "阴天", "多云"}
    );

    @Tool("查询指定城市的天气")
    public String getWeather(
            @P("城市名称，例如 北京、上海") String city) {
        log.info("[Tool] getWeather called: city={}", city);
        String cityKey = findCity(city);
        if (cityKey == null) {
            return city + " 的天气数据暂不可用，请尝试查询其他城市。";
        }
        String[] conditions = CITY_WEATHER.get(cityKey);
        String condition = conditions[RANDOM.nextInt(conditions.length)];
        int temp = 22 + RANDOM.nextInt(15);
        int humidity = 40 + RANDOM.nextInt(50);
        return String.format("%s 天气: %s，温度 %d°C，湿度 %d%%",
                cityKey, condition, temp, humidity);
    }

    @Tool("查询指定城市未来几天的天气预报")
    public String getWeatherForecast(
            @P("城市名称") String city,
            @P("预报天数，最多7天") int days) {
        log.info("[Tool] getWeatherForecast called: city={}, days={}", city, days);
        String cityKey = findCity(city);
        if (cityKey == null) {
            return city + " 的天气数据暂不可用，请尝试查询其他城市。";
        }
        if (days < 1) days = 1;
        if (days > 7) days = 7;

        String[] conditions = CITY_WEATHER.get(cityKey);
        StringBuilder sb = new StringBuilder(cityKey + " 未来 " + days + " 天天气预报:\n");
        for (int i = 0; i < days; i++) {
            String condition = conditions[RANDOM.nextInt(conditions.length)];
            int temp = 20 + RANDOM.nextInt(18);
            int humidity = 40 + RANDOM.nextInt(50);
            sb.append(String.format("第%d天: %s，温度 %d°C，湿度 %d%%\n",
                    i + 1, condition, temp, humidity));
        }
        return sb.toString().trim();
    }

    @Tool("比较两个城市的天气")
    public String compareWeather(
            @P("第一个城市") String city1,
            @P("第二个城市") String city2) {
        log.info("[Tool] compareWeather called: city1={}, city2={}", city1, city2);
        return getWeather(city1) + "\n" + getWeather(city2);
    }

    private String findCity(String input) {
        for (String city : CITY_WEATHER.keySet()) {
            if (input.contains(city)) {
                return city;
            }
        }
        return null;
    }
}
