package com.ai.assistant.service;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * ReactAgent 智能体服务
 * 使用 Spring AI Alibaba Graph 框架的 ReactAgent 实现
 * 支持推理-行动-观察（ReAct）循环，自动调度工具调用
 * 支持流式输出和非流式输出两种模式
 * <p>
 * 核心工作流程：
 * 1. 接收用户输入和会话上下文
 * 2. 模型分析是否需要调用工具
 * 3. 如需调用，执行工具并获取结果
 * 4. 将工具结果加入上下文，继续推理
 * 5. 循环执行直到得出最终答案
 *
 * @author ReactAgent Chat System
 * @since 2026-04-22
 */
@Slf4j
@Service
public class ReactAgentService {

    @Autowired
    private ReactAgent reactAgent;

    @Value("classpath:skills/introduction-business.md")
    private Resource introduction;
    @Value("classpath:skills/systemPrompt-business.md")
    private Resource systemPrompt;
    /**
     * 构建 RunnableConfig（包含会话配置）
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return RunnableConfig
     */
    private RunnableConfig buildRunnableConfig(String sessionId, String userId) {
        return RunnableConfig.builder()
                .threadId(sessionId)
                .addMetadata("user_id", userId)
                .build();
    }

    /**
     * 非流式聊天（一次性返回完整响应）
     *
     * @param userMessage  用户消息
     * @param sessionId    会话ID
     * @param userId       用户ID
     * @return AI 回复内容
     */
    public String chat( String userMessage, String sessionId, String userId) {
        log.info("ReactAgent 非流式聊天开始: sessionId={}", sessionId);

        long startTime = System.currentTimeMillis();

        // 创建 ReactAgent
        ReactAgent agent = selectReactAgent( userId);

        // 构建运行配置
        RunnableConfig config = buildRunnableConfig(sessionId, userId);

        log.debug("发送给模型的消息: {}", userMessage);

        try {
            // 执行聊天
            AssistantMessage response = agent.call(userMessage, config);
            String content = response.getText();

            long duration = System.currentTimeMillis() - startTime;
            log.info("ReactAgent 非流式聊天完成，耗时: {}ms, 响应长度: {}", duration, content.length());

            return content;
        } catch (GraphRunnerException e) {
            log.error("ReactAgent 调用失败", e);
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式聊天（SSE 实时推送）
     * 返回 Flux 流，前端可实时接收打字机效果
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @param sessionId    会话ID
     * @param userId       用户ID
     * @return Flux<String> 流式响应
     */
//    public Flux<String> chatStream(String systemPrompt, String userMessage, String sessionId, String userId) {
//        log.info("ReactAgent 流式聊天开始: sessionId={}", sessionId);
//
//        long startTime = System.currentTimeMillis();
//
//        // 创建 ReactAgent
//        ReactAgent agent = selectReactAgent(systemPrompt, sessionId);
//        try {
//            agent.setInstruction(FileCopyUtils.copyToString( new InputStreamReader(introduction.getInputStream(), StandardCharsets.UTF_8)));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        // 构建运行配置
//        RunnableConfig config = buildRunnableConfig(sessionId, userId);
//
//        log.debug("发送给模型的消息: {}", userMessage);
//
//        // 执行流式聊天
//        try {
//            return agent.stream(userMessage, config)
//                    .map(nodeOutput -> {
//                        // 从 NodeOutput 中提取文本内容
//                        try {
//                            if (nodeOutput != null) {
//                                if (nodeOutput instanceof StreamingOutput) {
//                                    StreamingOutput streamingOutput = (StreamingOutput) nodeOutput;
//                                    OutputType outputType = streamingOutput.getOutputType();
//                                    Message message = streamingOutput.message();
//
//                                    // 只处理模型流式输出，确保文本顺序
//                                    if (!Objects.isNull(outputType) && OutputType.AGENT_MODEL_STREAMING.equals(outputType)) {
//                                        log.debug("流式输出 AGENT_MODEL_STREAMING: {}", message.getText());
//                                        return Objects.isNull(message) || StringUtils.isEmpty(message.getText()) ? "" : message.getText();
//                                    }
//
//                                    // 其他类型输出仅记录日志，不返回给前端
//                                    if (!Objects.isNull(outputType) && OutputType.AGENT_TOOL_STREAMING.equals(outputType)) {
//                                        log.debug("跳过工具流式输出: {}", nodeOutput);
//                                    }
//                                    if (!Objects.isNull(outputType) && OutputType.AGENT_TOOL_FINISHED.equals(outputType)) {
//                                        log.debug("跳过工具完成输出: {}", nodeOutput);
//                                    }
//                                    if (!Objects.isNull(outputType) && OutputType.AGENT_HOOK_STREAMING.equals(outputType)) {
//                                        log.debug("跳过Hook流式输出: {}", nodeOutput);
//                                    }
//                                    if (!Objects.isNull(outputType) && OutputType.AGENT_HOOK_FINISHED.equals(outputType)) {
//                                        log.debug("跳过Hook完成输出: {}", nodeOutput);
//                                    }
//                                    if (!Objects.isNull(outputType) && OutputType.GRAPH_NODE_STREAMING.equals(outputType)) {
//                                        log.debug("跳过图节点流式输出: {}", nodeOutput);
//                                    }
//                                    if (!Objects.isNull(outputType) && OutputType.GRAPH_NODE_FINISHED.equals(outputType)) {
//                                        log.debug("跳过图节点完成输出: {}", nodeOutput);
//                                    }
//                                    if (!Objects.isNull(outputType) && OutputType.AGENT_MODEL_FINISHED.equals(outputType)) {
//                                        log.debug("模型完成输出: {}", nodeOutput);
//                                    }
//
//                                    // 其他类型不输出
//                                    return "";
//                                }
//                                // 尝试获取消息内容
//                                log.debug("流式输出 outputType not StreamingOutput: {}", nodeOutput);
//                                return "";
//                            }
//                        } catch (Exception e) {
//                            log.debug("提取节点输出失败", e);
//                        }
//                        log.debug("流式输出 nodeOutput null or error: {}", nodeOutput);
//                        return "";
//                    })
//                    .filter(content -> content != null && !content.isEmpty())
//                    .doOnNext(chunk -> log.trace("流式输出 chunk: {}", chunk))
//                    .doOnComplete(() -> log.info("ReactAgent 流式聊天完成，耗时: {}ms",
//                            System.currentTimeMillis() - startTime))
//                    .doOnError(error -> log.error("ReactAgent 流式聊天异常", error));
//        } catch (GraphRunnerException e) {
//            log.error("ReactAgent 流式调用失败", e);
//            return Flux.error(new RuntimeException("AI 流式调用失败: " + e.getMessage(), e));
//        }
//    }

    public Flux<String> chatStream(String userMessage, String sessionId, String userId) {
        log.info("ReactAgent 流式聊天开始: sessionId={}", sessionId);

        long startTime = System.currentTimeMillis();

        // 创建 ReactAgent
        ReactAgent agent = selectReactAgent(sessionId);

        // 构建运行配置
        RunnableConfig config = buildRunnableConfig(sessionId, userId);

        log.debug("发送给模型的消息: {}", userMessage);

        // 执行流式聊天
        try {
            return agent.stream(userMessage, config)
                    .publishOn(Schedulers.boundedElastic())
                    .concatMap(nodeOutput -> {
                        try {
                            if (nodeOutput != null && nodeOutput instanceof StreamingOutput) {
                                StreamingOutput streamingOutput = (StreamingOutput) nodeOutput;
                                OutputType outputType = streamingOutput.getOutputType();
                                Message message = streamingOutput.message();

                                if (!Objects.isNull(outputType) && OutputType.AGENT_MODEL_STREAMING.equals(outputType)) {
                                    String text = Objects.isNull(message) || StringUtils.isEmpty(message.getText()) ? "" : message.getText();
                                    if (!text.isEmpty()) {
                                        log.debug("流式输出 AGENT_MODEL_STREAMING: {}", text);
                                        return Flux.just(text);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.debug("提取节点输出失败", e);
                        }
                        return Flux.empty();
                    })
                    .doOnNext(chunk -> log.trace("流式输出 chunk: {}", chunk))
                    .doOnComplete(() -> log.info("ReactAgent 流式聊天完成，耗时: {}ms",
                            System.currentTimeMillis() - startTime))
                    .doOnError(error -> log.error("ReactAgent 流式聊天异常", error));
        } catch (GraphRunnerException e) {
            log.error("ReactAgent 流式调用失败", e);
            return Flux.error(new RuntimeException("AI 流式调用失败: " + e.getMessage(), e));
        }
    }

    private ReactAgent selectReactAgent(String userId) {
        try {
            reactAgent.setSystemPrompt(FileCopyUtils.copyToString( new InputStreamReader(systemPrompt.getInputStream(), StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            reactAgent.setInstruction(FileCopyUtils.copyToString( new InputStreamReader(introduction.getInputStream(), StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return reactAgent;
    }
}
