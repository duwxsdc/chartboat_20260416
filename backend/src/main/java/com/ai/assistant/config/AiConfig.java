package com.ai.assistant.config;

import com.ai.assistant.hook.CapabilityBoundaryHook;
import com.ai.assistant.hook.ContentFilterHook;
import com.ai.assistant.memory.TieredChatMemory;
import com.ai.assistant.tool.*;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class AiConfig {

    @Autowired
    private CapabilityBoundaryHook capabilityBoundaryHook;

    @Autowired
    private ContentFilterHook contentFilterHook;

    @Bean
    public ChatMemory chatMemory(TieredChatMemory tieredChatMemory) {
        return tieredChatMemory;
    }

    @Bean
    public ChatModel chatModel(ChatModel chatModel) {
        return chatModel;
    }

    @Bean
    public ReactAgent createReactAgent(ChatModel chatModel,ItbaSkillTool itbaSkillTool,BusinessSkillTool businessSkillTool) {
        return ReactAgent.builder()
                .name("react_chat_agent")
                .model(chatModel)
                .systemPrompt("你是一个有用的AI助手")
                .tools(getToolCallbacks())
                .hooks(contentFilterHook, skillsAgentHook(itbaSkillTool,businessSkillTool), capabilityBoundaryHook)
                .interceptors(ToolSelectionInterceptor.builder().selectionModel(chatModel).maxTools(5).build())
                .maxParallelTools(3)
                .returnReasoningContents(true)
                .saver(new MemorySaver())
                .enableLogging(true)
                .build();
    }

    private ToolCallback[] getToolCallbacks() {
        MethodToolCallbackProvider weatherProvider = MethodToolCallbackProvider.builder()
                .toolObjects(new AssistantTools())
                .build();

        List<ToolCallback> allTools = new java.util.ArrayList<>();
        allTools.addAll(List.of(weatherProvider.getToolCallbacks()));

        return allTools.toArray(new ToolCallback[0]);
    }

    @Bean
    public SkillsAgentHook skillsAgentHook(ItbaSkillTool itbaSkillTool,BusinessSkillTool businessSkillTool) {
        MethodToolCallbackProvider tool1 = MethodToolCallbackProvider.builder()
                .toolObjects(new TimeSkillTool()).build();
        MethodToolCallbackProvider tool2 = MethodToolCallbackProvider.builder()
                .toolObjects(new CalcSkillTool()).build();
        MethodToolCallbackProvider tool3 = MethodToolCallbackProvider.builder()
                .toolObjects(itbaSkillTool).build();
        MethodToolCallbackProvider tool = MethodToolCallbackProvider.builder()
                .toolObjects(businessSkillTool).build();

        Map<String, List<ToolCallback>> groupedTools = Map.of(
                "time-skill", List.of(tool1.getToolCallbacks()),
                "calc-skill", List.of(tool2.getToolCallbacks()),
                "itba-skill", List.of(tool3.getToolCallbacks()),
                "business-skill",List.of(tool.getToolCallbacks())
        );

        return SkillsAgentHook.builder()
                .skillRegistry(ClasspathSkillRegistry.builder().classpathPath("skills").build())
                .groupedTools(groupedTools)
                .build();
    }
}