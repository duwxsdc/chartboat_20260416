package com.ai.assistant.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;

@Component
public class BusinessSkillTool {

    // 注入业务录入人员专属的6份支撑文档
    @Value("classpath:skills/business-skill/补录平台基础通用知识.md")
    private Resource platformBasic;

    @Value("classpath:skills/business-skill/补录口业务录入操作规范.md")
    private Resource operationStandard;

    @Value("classpath:skills/business-skill/补录口业务录入操作流程.md")
    private Resource operationProcess;

    @Value("classpath:skills/business-skill/补录口权限说明及申请流程.md")
    private Resource permissionInfo;

    @Value("classpath:skills/business-skill/补录口基础信息查询.md")
    private Resource businessBasicInfo;

    @Value("classpath:skills/business-skill/补录业务FAQ.md")
    private Resource businessFaq;

    /**
     * 工具1：查询平台基础通用知识
     */
    @Tool(name = "query_platform_basic",
            description = "用户询问补录平台定位、业务录入人员定位、平台核心功能、基础使用前提时调用，读取【补录平台基础通用知识.md】")
    public String queryPlatformBasic() {
        return readMd(platformBasic);
    }

    /**
     * 工具2：查询补录操作规范
     */
    @Tool(name = "query_operation_standard",
            description = "用户询问补录数据填写规范、操作行为规范、禁止行为、特殊场景规范时调用，读取【补录口业务录入操作规范.md】")
    public String queryOperationStandard() {
        return readMd(operationStandard);
    }

    /**
     * 工具3：查询补录操作流程
     */
    @Tool(name = "query_operation_process",
            description = "用户询问登录、补录录入、提交审核、待办处理、会计期查询、补录记录查询步骤时调用，读取【补录口业务录入操作流程.md】")
    public String queryOperationProcess() {
        return readMd(operationProcess);
    }

    /**
     * 工具4：查询权限相关信息
     */
    @Tool(name = "query_permission_info",
            description = "用户询问个人权限范围、权限查询方法、权限申请与变更流程时调用，读取【补录口权限说明及申请流程.md】")
    public String queryPermissionInfo() {
        return readMd(permissionInfo);
    }

    /**
     * 工具5：查询补录口基础信息
     */
    @Tool(name = "query_business_basic_info",
            description = "用户询问补录口所属租户、菜单路径、ITBA责任人、补录口状态时调用，读取【补录口基础信息查询.md】")
    public String queryBusinessBasicInfo() {
        return readMd(businessBasicInfo);
    }

    /**
     * 工具6：查询业务FAQ高频问题
     */
    @Tool(name = "query_business_faq",
            description = "用户询问登录报错、操作报错、待办异常、会计期问题、问题升级途径时调用，读取【补录业务FAQ.md】")
    public String queryBusinessFaq() {
        return readMd(businessFaq);
    }

    // ===================== 【你要的新方法】会计期查询 =====================
    /**
     * 根据业务编码/租户编码/补录口编码 查询当前6位年月会计期
     * 随机返回 202401 ~ 202512 之间的合法会计期
     */
    @Tool(
            name = "query_current_accounting_period",
            description = "根据传入的编码（租户编码/补录口编码/业务编码）查询当前6位年月会计期；如果未传入编码或编码为空，必须提示用户先提供编码"
    )
    public String queryCurrentAccountingPeriod(String code) {
        // 1. 如果编码为空 → 强制要求用户提供
        if (!StringUtils.hasText(code)) {
            return "【查询失败】请先提供需要查询的编码（租户编码/补录口编码/业务编码），否则无法查询对应会计期。";
        }

        // 2. 模拟：随机生成 6 位年月会计期（格式：yyyyMM）
        Random random = new Random();
        int year = 2024 + random.nextInt(2);      // 2024 或 2025
        int month = 1 + random.nextInt(12);        // 1~12月
        String yearMonth = String.format("%d%02d", year, month);

        // 3. 返回标准VO结构（易解析、易阅读）
        return """
               {
                 "code": "%s",
                 "currentAccountingPeriod": "%s",
                 "desc": "当前有效会计期"
               }
               """.formatted(code, yearMonth);
    }

    /**
     * 通用读取MD文件方法（按需读取，不缓存全量，用完即销毁）
     */
    private String readMd(Resource resource) {
        try {
            return FileCopyUtils.copyToString( new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return "读取文档失败：" + resource.getDescription();
        }
    }
}
