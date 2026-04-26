package com.ai.assistant.config;

import com.ai.assistant.memory.TieredChatMemory;
import com.ai.assistant.tool.*;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class AiConfig {

    @Bean
    public ChatMemory chatMemory(TieredChatMemory tieredChatMemory) {
        return tieredChatMemory;
    }


    /**
     * 注入 DashScope ChatModel
     * Spring AI Alibaba Starter 会自动配置，这里显式声明以便扩展
     *
     * @param chatModel 自动注入的 ChatModel
     * @return ChatModel Bean
     */
    @Bean
    public ChatModel chatModel(ChatModel chatModel) {
        return chatModel;
    }

    /**
     * 创建 ReactAgent 实例
     *
     * @param systemPrompt 系统提示词
     * @param sessionId    会话ID（用于记忆）
     * @return ReactAgent 实例
     */
    @Bean
    public ReactAgent createReactAgent(ChatModel chatModel,ItbaSkillTool itbaSkillTool,BusinessSkillTool businessSkillTool) {
        // 构建 ReactAgent
        return ReactAgent.builder()
                .name("react_chat_agent")
                .model(chatModel)
                .systemPrompt("你是一个有用的AI助手")
                .tools(getToolCallbacks())
                .hooks(skillsAgentHook(itbaSkillTool,businessSkillTool))
                .interceptors(ToolSelectionInterceptor.builder().selectionModel(chatModel).maxTools(5).build())
                .maxParallelTools(3)
                .returnReasoningContents(true)
                .saver(new MemorySaver())
                .enableLogging(true)
                .build();
    }

    /**
     * 获取所有工具的 ToolCallback
     *
     * @return ToolCallback 数组
     */
    private ToolCallback[] getToolCallbacks() {
        // 使用 MethodToolCallbackProvider 从工具类中提取所有 @Tool 方法
        MethodToolCallbackProvider weatherProvider = MethodToolCallbackProvider.builder()
                .toolObjects(new AssistantTools())
                .build();

        // 合并所有工具
        List<ToolCallback> allTools = new java.util.ArrayList<>();
        allTools.addAll(List.of(weatherProvider.getToolCallbacks()));

        return allTools.toArray(new ToolCallback[0]);
    }

    @Bean
    public SkillsAgentHook skillsAgentHook(ItbaSkillTool itbaSkillTool,BusinessSkillTool businessSkillTool) {
        // 工具注册
        MethodToolCallbackProvider tool1 = MethodToolCallbackProvider.builder()
                .toolObjects(new TimeSkillTool()).build();
        MethodToolCallbackProvider tool2 = MethodToolCallbackProvider.builder()
                .toolObjects(new CalcSkillTool()).build();
        MethodToolCallbackProvider tool3 = MethodToolCallbackProvider.builder()
                .toolObjects(itbaSkillTool).build();
        MethodToolCallbackProvider tool = MethodToolCallbackProvider.builder()
                .toolObjects(businessSkillTool).build();
        // ========================
        // groupedTools：技能名 ↔ 工具绑定
        // ========================
        Map<String, List<ToolCallback>> groupedTools = Map.of(
                "time-skill", List.of(tool1.getToolCallbacks()),
                "calc-skill", List.of(tool2.getToolCallbacks()),
                "itba-skill", List.of(tool3.getToolCallbacks()),
                "business-skill",List.of(tool.getToolCallbacks())
        );

        return SkillsAgentHook.builder()
                .skillRegistry(ClasspathSkillRegistry.builder().classpathPath("skills").build())
                .groupedTools(groupedTools)  // ← 你问的核心！
                .build();
    }
}
