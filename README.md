# Spring AI 入门教程

基于 **Spring AI 1.1.5** + **MiniMax M2.7** 的 Spring AI 入门教程，包含 9 个渐进式示例，涵盖聊天、流式响应、多轮对话等核心功能。

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.4.5 | 基础框架 |
| Spring AI | 1.1.5 | AI 集成框架（最新稳定版） |
| MiniMax-M2.7 | - | 大语言模型（20万 token 上下文） |
| Java | 17+ | 运行环境 |

## 项目结构

```
springai/
├── pom.xml                                              # Maven 依赖配置
├── README.md                                            # 项目说明
├── src/main/resources/
│   ├── application.yml                                  # Spring AI + MiniMax 配置
│   └── static/
│       └── index.html                                   # 浏览器测试页面
└── src/main/java/com/example/springai/
    ├── SpringAiTutorialApplication.java                 # 启动类
    ├── config/
    │   └── AiConfig.java                                # ChatClient & ChatMemory 配置
    └── controller/
        ├── ChatController.java                          # 示例 1~4：基础聊天
        ├── StreamingChatController.java                 # 示例 5~6：流式响应
        └── MultiTurnChatController.java                 # 示例 7~9：多轮对话
```

## 快速开始

### 1. 环境要求

- JDK 17 或以上
- Maven 3.6+

### 2. 配置 API Key

编辑 `src/main/resources/application.yml`，修改 API Key：

```yaml
spring:
  ai:
    openai:
      api-key: 你的MiniMax-API-Key
```

> 也可以通过环境变量注入：`export MINIMAX_API_KEY=你的Key`，然后将配置改为 `api-key: ${MINIMAX_API_KEY}`

### 3. 启动项目

```bash
cd springai
mvn spring-boot:run
```

启动成功后，控制台会显示：

```
Started SpringAiTutorialApplication in x.xxx seconds
```

### 4. 打开测试页面

浏览器访问：**http://localhost:8081**

这是内置的测试控制台，可以直接在浏览器中测试所有 9 个示例，无需使用 curl。

## 9 个教程示例

### 示例 1：基础聊天

最简单的 Spring AI 调用，使用 ChatClient Fluent API。

```
GET /api/chat?message=你好
```

### 示例 2：系统提示词

通过 System Prompt 设定 AI 角色（编程导师），让回复更有针对性。

```
GET /api/chat/system?message=什么是多线程
```

### 示例 3：动态参数调整

运行时修改 temperature（创造性）和 maxTokens（回复长度）。

```
GET /api/chat/params?message=写一首诗&temperature=1.2&maxTokens=1024
```

### 示例 4：底层 Prompt API

使用 ChatModel + Prompt 手动构建请求，获取 Token 用量统计。

```
GET /api/chat/prompt?message=什么是微服务架构
```

### 示例 5：流式响应

SSE 流式输出，AI 回复像打字机一样逐字显示。

```
GET /api/chat/stream?message=写一首关于春天的诗
```

### 示例 6：流式 + 系统提示词

流式模式下同时使用系统提示词，让 AI 输出代码示例。

```
GET /api/chat/stream/code?message=解释什么是递归
```

### 示例 7：多轮对话记忆

通过 conversationId 区分会话，AI 能记住同一会话中的上下文。

```
GET /api/chat/multi?message=我叫小明&conversationId=session1
GET /api/chat/multi?message=我叫什么名字&conversationId=session1
```

### 示例 8：限制记忆窗口

限制保留的最近消息数量，控制上下文长度。

```
GET /api/chat/multi/limited?message=你好&conversationId=s1&maxMessages=5
```

### 示例 9：清除对话记忆

清除指定会话的历史消息。

```
GET /api/chat/multi/clear?conversationId=session1
```

## 日志说明

项目已配置分层日志，方便调试：

| 日志前缀 | 说明 | 示例输出 |
|----------|------|---------|
| `[基础聊天]` | 示例 1 的请求/响应 | `收到请求 - 用户消息: 你好` |
| `[系统提示词]` | 示例 2 的请求/响应 | `收到请求 - 用户消息: 什么是多线程` |
| `[参数调整]` | 示例 3 的参数和响应 | `temperature: 1.2, maxTokens: 1024` |
| `[Prompt API]` | 示例 4 的 Token 统计 | `输入Token: 25, 输出Token: 180` |
| `[流式聊天]` | 示例 5~6 的流式耗时 | `流式响应完成 - 耗时: 3200ms` |
| `[多轮对话]` | 示例 7~9 的会话信息 | `会话ID: session1, 用户消息: ...` |

### 调整日志级别

编辑 `application.yml` 中的日志配置：

```yaml
logging:
  level:
    # 查看详细请求参数和完整回复内容
    com.example.springai: DEBUG
    # 查看 Spring AI 内部调用过程
    org.springframework.ai: DEBUG
    # 查看 HTTP 请求/响应细节（排查 API 问题）
    org.springframework.ai.openai: DEBUG
```

## 核心知识点

### MiniMax M2.7 接入方式

MiniMax M2.7 通过 OpenAI 兼容 API 提供，使用 `spring-ai-starter-model-openai` + 自定义 `base-url` 接入：

```yaml
spring:
  ai:
    openai:
      base-url: https://api.minimaxi.com    # MiniMax 的 OpenAI 兼容端点
      api-key: 你的API-Key
      chat:
        options:
          model: MiniMax-M2.7               # 模型名称
```

### 多轮对话 Advisor 选择

MiniMax M2.7 对 `system` 消息位置敏感，需使用 `PromptChatMemoryAdvisor`（将历史合并到 system 提示词）而非 `MessageChatMemoryAdvisor`（将历史作为独立消息插入），否则会报 `invalid message role: system` 错误。

### ChatModel vs ChatClient

| 特性 | ChatModel（底层） | ChatClient（高级） |
|------|-------------------|-------------------|
| 调用方式 | 手动构建 Prompt | Fluent API |
| 流式支持 | 需自行处理 | `.stream().content()` |
| Advisor | 不支持 | 支持记忆、日志等 |
| Token 统计 | 直接获取 | 需额外调用 |
| 推荐场景 | 需要精细控制 | 日常开发 |
