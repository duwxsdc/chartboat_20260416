package com.ai.assistant.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class CalcSkillTool {

    @Tool(name = "calculate", description = "数学计算")
    public String calculate(double a, double b, String op) {
        return switch (op) {
            case "+" -> String.valueOf(a + b);
            case "-" -> String.valueOf(a - b);
            case "*" -> String.valueOf(a * b);
            case "/" -> String.valueOf(a / b);
            default -> "error";
        };
    }
}
