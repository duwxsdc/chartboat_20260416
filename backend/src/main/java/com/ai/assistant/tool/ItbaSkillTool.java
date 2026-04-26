package com.ai.assistant.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
public class ItbaSkillTool {

    // 注入各个业务文档
    @Value("classpath:skills/itba-skill/补录平台简介.md")
    private Resource platformIntro;

    @Value("classpath:skills/itba-skill/补录平台ITBA职责.md")
    private Resource itbaDuty;

    @Value("classpath:skills/itba-skill/补录平台ITBA操作手册.md")
    private Resource operationManual;

    @Value("classpath:skills/itba-skill/补录平台ITBA权限.md")
    private Resource permissionInfo;

    @Value("classpath:skills/itba-skill/补录平台ITBA FAQ.md")
    private Resource faqDoc;

    /**
     * 工具1：查询平台简介
     */
    @Tool(name = "query_platform_intro",
            description = "用户询问补录平台介绍、平台定位、核心能力、业务价值、使用对象时调用，读取【补录平台简介.md】")
    public String queryPlatformIntro() {
        return readMd(platformIntro);
    }

    /**
     * 工具2：查询ITBA岗位职责
     */
    @Tool(name = "query_itba_duty",
            description = "用户询问ITBA岗位职责、工作范围、操作红线、禁止行为、岗位约束时调用，读取【补录平台ITBA职责.md】")
    public String queryItbaDuty() {
        return readMd(itbaDuty);
    }

    /**
     * 工具3：查询操作手册与配置指导
     */
    @Tool(name = "query_operation_manual",
            description = "用户询问补录操作流程、平台登录、配置指引、功能使用、操作规范时调用，读取【补录平台ITBA操作手册.md】")
    public String queryOperationManual() {
        return readMd(operationManual);
    }

    /**
     * 工具4：查询权限信息
     */
    @Tool(name = "query_permission_info",
            description = "用户询问ITBA权限范围、权限申请流程、权限限制、账号约束时调用，读取【补录平台ITBA权限.md】")
    public String queryPermissionInfo() {
        return readMd(permissionInfo);
    }

    /**
     * 工具5：查询常见问题FAQ
     */
    @Tool(name = "query_faq",
            description = "用户询问登录报错、菜单缺失、功能限制、异常处理、高频问题时调用，读取【补录平台ITBA FAQ.md】")
    public String queryFaq() {
        return readMd(faqDoc);
    }

    /**
     * 通用读取MD文件方法
     */
    private String readMd(Resource resource) {
        try {
            return FileCopyUtils.copyToString( new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return "读取文档失败：" + resource.getDescription();
        }
    }
}
