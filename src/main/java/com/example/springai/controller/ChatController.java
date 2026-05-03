package com.example.springai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 基础聊天控制器 - 演示 Spring AI 的核心聊天功能
 *
 * 本控制器涵盖了 Spring AI 聊天功能的基础用法：
 *   1. 简单对话：使用 ChatClient 进行基本的问答
 *   2. 系统提示词：通过 SystemMessage 设定 AI 的角色和行为
 *   3. 参数调整：动态修改 temperature、maxTokens 等生成参数
 *   4. 底层 API：使用 ChatModel + Prompt 进行更精细的控制
 *
 * ============================================================
 * Spring AI 消息类型说明：
 * ============================================================
 *
 * Spring AI 中有三种核心消息类型，对应聊天 API 中的角色：
 *
 *   - UserMessage（用户消息）：用户发送给 AI 的内容
 *     对应 OpenAI API 中的 "role: user"
 *
 *   - SystemMessage（系统消息）：设定 AI 的行为规则和角色
 *     对应 OpenAI API 中的 "role: system"
 *     系统消息不会显示在对话中，但会影响 AI 的回复方式
 *
 *   - AssistantMessage（助手消息）：AI 的回复内容
 *     对应 OpenAI API 中的 "role: assistant"
 *     在多轮对话中，需要将之前的 AI 回复也传入，以保持上下文连贯
 *
 * ============================================================
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatClient chatClient;
    private final ChatModel chatModel;

    public ChatController(ChatClient chatClient, ChatModel chatModel) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
    }

    /**
     * 示例 1：最简单的聊天接口
     *
     * 使用 ChatClient 的 Fluent API 进行基本对话。
     * ChatClient.prompt() 会创建一个 Prompt 构建器，
     * .user() 设置用户消息，.call() 执行同步调用，
     * .content() 获取 AI 回复的纯文本内容。
     *
     * @param message 用户输入的消息
     * @return AI 的回复文本
     */
    @GetMapping
    public String chat(@RequestParam(defaultValue = "你好") String message) {
        log.info("[基础聊天] 收到请求 - 用户消息: {}", message);
        long startTime = System.currentTimeMillis();

        String result = chatClient.prompt()
                .user(message)
                .call()
                .content();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[基础聊天] 请求完成 - 耗时: {}ms, 回复长度: {}字符", elapsed, result != null ? result.length() : 0);
        log.debug("[基础聊天] AI 回复内容: {}", result);
        return result;
    }

    /**
     * 示例 2：使用系统提示词（System Prompt）
     *
     * 系统提示词是引导 AI 行为的强大工具。通过设定不同的系统提示词，
     * 可以让同一个模型扮演完全不同的角色。
     *
     * 常见用法：
     *   - 角色扮演："你是一位资深的 Java 架构师"
     *   - 输出格式："请以 JSON 格式输出"
     *   - 行为约束："只回答与编程相关的问题"
     *   - 语言设定："请用中文回答"
     *
     * @param message 用户输入的消息
     * @return AI 基于系统提示词设定的角色回复
     */
    @GetMapping("/system")
    public String chatWithSystemPrompt(@RequestParam(defaultValue = "介绍一下你自己") String message) {
        String systemPrompt = "你是一位幽默风趣的编程导师，擅长用生动的比喻解释技术概念。" +
                "回答问题时：1）先用一个通俗的比喻引入；2）再给出技术解释；" +
                "3）最后提供一个代码示例。请用中文回答。";
        log.info("[系统提示词] 收到请求 - 用户消息: {}, 系统提示词长度: {}字符", message, systemPrompt.length());
        long startTime = System.currentTimeMillis();

        String result = chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .call()
                .content();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[系统提示词] 请求完成 - 耗时: {}ms, 回复长度: {}字符", elapsed, result != null ? result.length() : 0);
        return result;
    }

    /**
     * 示例 3：动态调整生成参数
     *
     * Spring AI 允许在每次请求时动态覆盖默认的模型参数。
     * 这在需要不同"创造性"程度的场景中非常有用：
     *
     *   - 低 temperature（0.0~0.3）：适合代码生成、事实性问答等需要确定性输出的场景
     *   - 中 temperature（0.4~0.7）：适合日常对话、翻译等平衡场景
     *   - 高 temperature（0.8~1.5）：适合创意写作、头脑风暴等需要创造性的场景
     *
     * @param message     用户输入的消息
     * @param temperature 温度参数，控制随机性
     * @param maxTokens   最大生成 token 数
     * @return AI 的回复文本
     */
    @GetMapping("/params")
    public String chatWithParams(
            @RequestParam(defaultValue = "用一句话解释什么是人工智能") String message,
            @RequestParam(defaultValue = "0.7") Double temperature,
            @RequestParam(defaultValue = "1024") Integer maxTokens) {

        log.info("[参数调整] 收到请求 - 用户消息: {}, temperature: {}, maxTokens: {}", message, temperature, maxTokens);
        long startTime = System.currentTimeMillis();

        String result = chatClient.prompt()
                .user(message)
                .options(OpenAiChatOptions.builder()
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .build())
                .call()
                .content();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[参数调整] 请求完成 - 耗时: {}ms, 回复长度: {}字符", elapsed, result != null ? result.length() : 0);
        return result;
    }

    /**
     * 示例 4：使用底层 ChatModel API
     *
     * ChatModel 是 Spring AI 的底层 API，提供对模型调用的完整控制。
     * 与 ChatClient 的高级 Fluent API 不同，ChatModel 需要手动构建 Prompt 对象。
     *
     * Prompt 是发送给 AI 模型的完整请求，包含：
     *   - 消息列表（Message List）：对话中的所有消息
     *   - 模型选项（ChatOptions）：生成参数（可覆盖默认配置）
     *
     * ChatResponse 是模型的完整响应，包含：
     *   - content：AI 回复的文本内容
     *   - metadata：元数据（模型名称、token 用量等）
     *   - generations：生成结果列表（支持一次生成多个回复）
     *
     * @param message 用户输入的消息
     * @return 包含回复内容和 token 用量信息的结果
     */
    @GetMapping("/prompt")
    public String chatWithPrompt(@RequestParam(defaultValue = "什么是微服务架构") String message) {
        log.info("[Prompt API] 收到请求 - 用户消息: {}", message);
        long startTime = System.currentTimeMillis();

        Prompt prompt = new Prompt(List.of(
                new SystemMessage("你是一位软件架构专家，回答要简洁专业。"),
                new UserMessage(message)
        ));
        log.debug("[Prompt API] 构建 Prompt 完成 - 消息数量: {}", prompt.getInstructions().size());

        ChatResponse response = chatModel.call(prompt);

        String content = response.getResult().getOutput().getText();
        var usage = response.getMetadata().getUsage();
        long elapsed = System.currentTimeMillis() - startTime;

        log.info("[Prompt API] 请求完成 - 耗时: {}ms, 输入Token: {}, 输出Token: {}, 总Token: {}",
                elapsed, usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());

        return String.format("""
                ========================================
                🤖 AI 回复：
                %s

                📊 Token 用量统计：
                - 输入 Token：%d
                - 输出 Token：%d
                - 总计 Token：%d
                - 响应耗时：%dms
                ========================================
                """,
                content,
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens(),
                elapsed);
    }
}
