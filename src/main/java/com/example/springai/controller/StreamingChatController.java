package com.example.springai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 流式聊天控制器 - 演示 Spring AI 的流式响应功能
 *
 * ============================================================
 * 为什么需要流式响应？
 * ============================================================
 *
 * 传统同步调用的问题：
 *   - AI 模型生成回复可能需要数秒甚至数十秒
 *   - 用户在等待期间看不到任何输出，体验很差
 *   - 长文本生成时，用户可能以为系统卡死了
 *
 * 流式响应（Streaming）的优势：
 *   - AI 模型逐 token 生成回复，每生成一部分就立即推送给客户端
 *   - 用户可以看到"打字机"效果，体验更自然
 *   - 首个 token 的延迟大幅降低，用户几乎立即看到输出
 *
 * ============================================================
 * Spring AI 流式调用的技术实现：
 * ============================================================
 *
 * Spring AI 的流式调用基于 Reactor 的 Flux 实现：
 *   - ChatClient.prompt().stream() 返回 Flux<String>
 *   - 底层使用 Server-Sent Events (SSE) 协议传输数据
 *   - 每个 Flux 元素是一个生成的文本片段（通常是 1~2 个 token）
 *   - 流结束时 Flux 自动完成
 *
 * ============================================================
 * SSE（Server-Sent Events）说明：
 * ============================================================
 *
 * SSE 是一种基于 HTTP 的单向实时推送协议：
 *   - 服务端可以持续向客户端推送数据
 *   - 数据格式：data: 文本内容\n\n
 *   - 浏览器原生支持 EventSource API 接收 SSE
 *   - 比 WebSocket 更简单，适合服务端推送场景
 *
 * 本控制器使用 MediaType.TEXT_EVENT_STREAM 作为 Content-Type，
 * Spring Boot 会自动将 Flux<String> 转换为 SSE 格式。
 *
 * ============================================================
 */
@RestController
@RequestMapping("/api/chat/stream")
public class StreamingChatController {

    private static final Logger log = LoggerFactory.getLogger(StreamingChatController.class);

    private final ChatClient chatClient;

    public StreamingChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 示例 5：流式聊天接口
     *
     * 使用 ChatClient 的 stream() 方法进行流式调用。
     * 返回类型是 Flux<String>，Spring WebFlux 会将其转换为 SSE 流。
     *
     * @param message 用户输入的消息
     * @return Flux<String> 流式响应，每个元素是一个文本片段
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam(defaultValue = "写一首关于春天的诗") String message) {
        log.info("[流式聊天] 收到请求 - 用户消息: {}", message);
        long startTime = System.currentTimeMillis();

        return chatClient.prompt()
                .system("你是一位诗人，擅长创作优美的中文诗歌。")
                .user(message)
                .stream()
                .content()
                .doOnComplete(() -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    log.info("[流式聊天] 流式响应完成 - 耗时: {}ms", elapsed);
                })
                .doOnError(e -> log.error("[流式聊天] 流式响应出错: {}", e.getMessage()));
    }

    /**
     * 示例 6：带系统提示词的流式聊天
     *
     * 演示在流式模式下同时使用系统提示词。
     * 系统提示词在流式调用中的作用与同步调用完全一致。
     *
     * @param message 用户输入的消息
     * @return Flux<String> 流式响应
     */
    @GetMapping(value = "/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamCodeChat(@RequestParam(defaultValue = "解释什么是递归") String message) {
        log.info("[流式代码] 收到请求 - 用户消息: {}", message);
        long startTime = System.currentTimeMillis();

        return chatClient.prompt()
                .system("你是一位编程导师，回答要包含代码示例。" +
                        "使用 Markdown 代码块格式，并标注语言类型。")
                .user(message)
                .stream()
                .content()
                .doOnComplete(() -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    log.info("[流式代码] 流式响应完成 - 耗时: {}ms", elapsed);
                })
                .doOnError(e -> log.error("[流式代码] 流式响应出错: {}", e.getMessage()));
    }
}
