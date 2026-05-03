package com.example.springai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置类
 *
 * 本类负责配置 Spring AI 相关的 Bean，包括：
 *   - ChatMemory：聊天记忆存储，用于多轮对话场景
 *   - ChatClient：Spring AI 的高级聊天客户端，支持流式调用、Advisor 等功能
 *
 * ============================================================
 * Spring AI 的两个核心 API 层级：
 * ============================================================
 *
 * 1. ChatModel（低级 API）
 *    - 直接与 AI 模型交互，发送 Prompt，获取 ChatResponse
 *    - 适合简单的单轮对话场景
 *    - 通过自动配置注入，开箱即用
 *
 * 2. ChatClient（高级 API）
 *    - 基于 ChatModel 构建的流畅 API（Fluent API）
 *    - 支持 Advisor 模式（如聊天记忆、日志等）
 *    - 支持流式调用、结构化输出等高级功能
 *    - 推荐在大多数场景下使用
 *
 * ============================================================
 */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    /**
     * 创建聊天记忆 Bean
     *
     * ChatMemory 是 Spring AI 提供的对话记忆抽象接口，
     * 用于存储和检索对话历史消息。它有两个主要方法：
     *   - add(conversationId, messages)：添加消息到指定会话
     *   - get(conversationId, lastN)：获取指定会话最近 N 条消息
     *
     * MessageWindowChatMemory 是 Spring AI 1.0 提供的基于内存的窗口式记忆实现：
     *   - 内部使用 InMemoryChatMemoryRepository 存储消息
     *   - 通过 maxMessages 参数限制保留的最近消息数量（默认 20 条）
     *   - 超出窗口大小的旧消息会被自动丢弃
     *   - 应用重启后记忆会丢失（内存存储）
     *
     * 生产环境中可以使用基于数据库或 Redis 的持久化实现，
     * 例如 JdbcChatMemoryRepository 或自定义的 Redis 实现。
     *
     * @return 聊天记忆实例
     */
    @Bean
    public ChatMemory chatMemory() {
        log.info("[AI配置] 初始化 ChatMemory: MessageWindowChatMemory, maxMessages=20");
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    /**
     * 创建 ChatClient Bean
     *
     * ChatClient 是 Spring AI 的高级 API，采用 Builder 模式构建。
     * 它封装了 ChatModel，提供了更简洁的调用方式。
     *
     * 使用示例：
     *   chatClient.prompt()
     *       .user("你好")                    // 设置用户消息
     *       .system("你是一个助手")           // 设置系统提示词
     *       .advisors(memoryAdvisor)          // 添加 Advisor
     *       .call()                           // 同步调用
     *       .content();                       // 获取文本内容
     *
     * @param chatModel 自动注入的 ChatModel（由 Spring AI Starter 自动配置）
     * @return ChatClient 实例
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        log.info("[AI配置] 初始化 ChatClient, 底层模型: {}", chatModel.getClass().getSimpleName());
        return ChatClient.builder(chatModel).build();
    }
}
