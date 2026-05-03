package com.example.springai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多轮对话控制器 - 演示 Spring AI 的对话记忆功能
 *
 * ============================================================
 * 为什么需要对话记忆？
 * ============================================================
 *
 * 大语言模型本身是无状态的——每次调用都是独立的，模型不会"记住"
 * 之前的对话内容。但在实际应用中，我们经常需要多轮对话，例如：
 *
 *   用户：我叫小明
 *   AI：你好小明！
 *   用户：我叫什么名字？    ← 如果没有记忆，AI 不知道你叫小明
 *   AI：你叫小明！          ← 有了记忆，AI 能正确回答
 *
 * ============================================================
 * Spring AI 的对话记忆实现原理：
 * ============================================================
 *
 * Spring AI 通过 Advisor 模式实现对话记忆，提供了两种 Advisor：
 *
 *   1. MessageChatMemoryAdvisor（消息式记忆）
 *      - 将历史消息作为独立的消息添加到 Prompt 中
 *      - 适合支持多轮消息格式的模型（如 OpenAI 原生 API）
 *      - 注意：某些模型（如 MiniMax M2.7）对 system 消息的位置有限制
 *
 *   2. PromptChatMemoryAdvisor（提示词式记忆）
 *      - 将历史消息合并到 system 提示词中
 *      - 兼容性更好，适合对消息格式有严格限制的模型
 *      - 本教程使用此方式，因为 MiniMax M2.7 对 system 角色位置敏感
 *
 * ============================================================
 * conversationId 的作用：
 * ============================================================
 *
 * conversationId 用于区分不同的对话会话。
 * 每个会话有独立的记忆空间，互不干扰。
 *
 * 这在实际应用中非常重要：
 *   - 不同用户有不同的 conversationId
 *   - 同一用户的不同话题可以有不同的 conversationId
 *
 * ============================================================
 */
@RestController
@RequestMapping("/api/chat/multi")
public class MultiTurnChatController {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public MultiTurnChatController(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    /**
     * 示例 7：多轮对话接口（使用 PromptChatMemoryAdvisor）
     *
     * 通过 conversationId 参数区分不同的对话会话。
     * 同一个 conversationId 下的消息会共享上下文记忆。
     *
     * 这里使用 PromptChatMemoryAdvisor 而非 MessageChatMemoryAdvisor，
     * 原因是 MiniMax M2.7 的 OpenAI 兼容接口对消息格式有特殊要求：
     *   - system 消息只能出现在消息列表的开头
     *   - MessageChatMemoryAdvisor 会将历史消息（含 system）插入到消息列表中间，
     *     导致 MiniMax API 返回 "invalid message role: system" 错误
     *   - PromptChatMemoryAdvisor 将历史消息合并到 system 提示词中，
     *     避免了消息角色冲突的问题
     *
     * 测试步骤（在终端中依次执行）：
     *
     * 第一步 - 告诉 AI 你的名字：
     *   curl --get "http://localhost:8081/api/chat/multi" --data-urlencode "conversationId=session1" --data-urlencode "message=我叫小明，我是一名Java开发者"
     *
     * 第二步 - 问 AI 你的名字（AI 能记住）：
     *   curl --get "http://localhost:8081/api/chat/multi" --data-urlencode "conversationId=session1" --data-urlencode "message=我叫什么名字？我做什么工作？"
     *
     * 第三步 - 换一个会话，AI 不会知道你的名字：
     *   curl --get "http://localhost:8081/api/chat/multi" --data-urlencode "conversationId=session2" --data-urlencode "message=我叫什么名字？"
     *
     * @param message        用户输入的消息
     * @param conversationId 会话 ID，用于区分不同的对话
     * @return AI 的回复文本
     */
    @GetMapping
    public String multiTurnChat(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String conversationId) {

        return chatClient.prompt()
                .system("你是一个友好的对话助手，能够记住用户之前说过的话。" +
                        "请用中文回答，回答要简洁自然。")
                .user(message)
                .advisors(PromptChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(conversationId)
                        .build())
                .call()
                .content();
    }

    /**
     * 示例 8：带记忆长度限制的多轮对话
     *
     * 在实际应用中，对话历史不能无限增长，因为：
     *   1. 模型的上下文窗口有限（MiniMax-M2.7 为 204800 token）
     *   2. 过长的历史会降低回复质量（模型注意力分散）
     *   3. 过长的历史会增加 API 调用成本（按 token 计费）
     *
     * Spring AI 提供了 MessageWindowChatMemory 来限制记忆窗口大小，
     * 只保留最近 N 条消息作为上下文，更早的消息会被自动丢弃。
     * 这是一种简单但有效的滑动窗口策略。
     *
     * 测试命令：
     *   curl --get "http://localhost:8081/api/chat/multi/limited" --data-urlencode "conversationId=session3" --data-urlencode "message=你好" --data-urlencode "maxMessages=5"
     *
     * @param message      用户输入的消息
     * @param conversationId 会话 ID
     * @param maxMessages  保留最近 N 条消息作为上下文
     * @return AI 的回复文本
     */
    @GetMapping("/limited")
    public String multiTurnChatWithLimit(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String conversationId,
            @RequestParam(defaultValue = "10") int maxMessages) {

        ChatMemory limitedMemory = MessageWindowChatMemory.builder()
                .maxMessages(maxMessages)
                .build();

        return chatClient.prompt()
                .system("你是一个友好的对话助手。请用中文回答。")
                .user(message)
                .advisors(PromptChatMemoryAdvisor.builder(limitedMemory)
                        .conversationId(conversationId)
                        .build())
                .call()
                .content();
    }

    /**
     * 示例 9：清除指定会话的对话记忆
     *
     * 当用户想开始一个全新的对话时，可以清除之前的记忆。
     * 清除后，该会话 ID 下之前的对话历史将不再被 AI 看到。
     *
     * 测试命令：
     *   curl --get "http://localhost:8081/api/chat/multi/clear" --data-urlencode "conversationId=session1"
     *
     * @param conversationId 要清除记忆的会话 ID
     * @return 操作结果
     */
    @GetMapping("/clear")
    public String clearMemory(@RequestParam String conversationId) {
        chatMemory.clear(conversationId);
        return "会话 " + conversationId + " 的记忆已清除！";
    }
}
