package com.example.backend.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
public class DateTimeTools {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Tool("获取当前日期和时间")
    public String getCurrentDateTime() {
        log.info("[Tool] getCurrentDateTime called");
        return "当前日期和时间: " + LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    @Tool("获取当前日期")
    public String getCurrentDate() {
        log.info("[Tool] getCurrentDate called");
        return "当前日期: " + LocalDate.now().format(DATE_FORMATTER);
    }

    @Tool("获取当前时间")
    public String getCurrentTime() {
        log.info("[Tool] getCurrentTime called");
        return "当前时间: " + LocalTime.now().format(TIME_FORMATTER);
    }

    @Tool("计算两个日期之间相差的天数")
    public long daysBetween(
            @P("开始日期，格式为 yyyy-MM-dd") String from,
            @P("结束日期，格式为 yyyy-MM-dd") String to) {
        log.info("[Tool] daysBetween called: from={}, to={}", from, to);
        LocalDate fromDate = LocalDate.parse(from, DATE_FORMATTER);
        LocalDate toDate = LocalDate.parse(to, DATE_FORMATTER);
        return Math.abs(java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate));
    }

    @Tool("计算指定天数后的日期")
    public String addDays(
            @P("起始日期，格式为 yyyy-MM-dd") String date,
            @P("要增加的天数") int days) {
        log.info("[Tool] addDays called: date={}, days={}", date, days);
        LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
        LocalDate result = localDate.plusDays(days);
        return result.format(DATE_FORMATTER);
    }
}
