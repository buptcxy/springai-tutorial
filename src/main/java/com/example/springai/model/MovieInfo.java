package com.example.springai.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record MovieInfo(
        @JsonPropertyDescription("电影名称")
        String title,
        @JsonPropertyDescription("导演姓名")
        String director,
        @JsonPropertyDescription("上映年份")
        Integer year,
        @JsonPropertyDescription("电影类型，如科幻、喜剧、动作等")
        String genre,
        @JsonPropertyDescription("一句话简介")
        String summary
) {}
