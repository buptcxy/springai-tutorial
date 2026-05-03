package com.example.springai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI 入门教程 - 主启动类
 *
 * 本教程演示了 Spring AI 框架的核心功能，包括：
 *   1. 基础聊天对话（同步调用）
 *   2. 系统提示词（角色设定）
 *   3. 流式响应（Server-Sent Events）
 *   4. 多轮对话（上下文记忆）
 *   5. 参数动态调整
 *
 * 使用的模型：MiniMax M2.7（通过 OpenAI 兼容接口接入）
 *
 * 启动后访问以下接口即可体验：
 *   - GET  http://localhost:8080/api/chat?message=你好
 *   - GET  http://localhost:8080/api/chat/system?message=介绍一下你自己
 *   - GET  http://localhost:8080/api/chat/stream?message=写一首诗
 *   - POST http://localhost:8080/api/chat/multi  (多轮对话，见 Controller 说明)
 */
@SpringBootApplication
public class SpringAiTutorialApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiTutorialApplication.class, args);
    }
}
