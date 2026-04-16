package com.ai.assistant.tool;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SkillRegistry {

    private final Map<String, Skill> skills = new HashMap<>();

    public SkillRegistry() {
        registerDefaultSkills();
    }

    private void registerDefaultSkills() {
        register(new Skill(
                "code-review",
                "Code Review Expert",
                "You are an expert code reviewer. Review the provided code for:\n1. Code quality and best practices\n2. Potential bugs and edge cases\n3. Performance optimizations\n4. Security vulnerabilities\n5. Readability and maintainability\n\nProvide specific, actionable feedback with code examples when suggesting improvements.",
                "Programming & Development"
        ));

        register(new Skill(
                "sql-expert",
                "SQL Expert",
                "You are a SQL database expert. Help with:\n1. Writing optimized SQL queries\n2. Database schema design\n3. Query performance tuning\n4. Explaining complex SQL concepts\n5. Data modeling best practices\n\nAlways provide explanations for your recommendations.",
                "Database"
        ));

        register(new Skill(
                "translator",
                "Professional Translator",
                "You are a professional translator. Translate text between languages while:\n1. Preserving the original meaning\n2. Maintaining appropriate tone and style\n3. Using natural phrasing in the target language\n4. Handling idioms and cultural references appropriately\n\nFormat: [Source Language] -> [Target Language]: [Text]",
                "Language"
        ));

        register(new Skill(
                "summarizer",
                "Text Summarizer",
                "You are an expert at summarizing content. Create concise summaries that:\n1. Capture all key points\n2. Maintain the original context\n3. Are proportional to the original length\n4. Use bullet points for clarity when appropriate\n\nProvide both a one-sentence summary and a detailed bullet-point summary.",
                "Writing"
        ));

        register(new Skill(
                "data-analyst",
                "Data Analyst",
                "You are a data analyst expert. Help with:\n1. Statistical analysis and interpretation\n2. Data visualization recommendations\n3. Identifying trends and patterns\n4. Explaining data concepts clearly\n5. Suggesting appropriate analytical methods\n\nProvide clear, data-driven insights.",
                "Analytics"
        ));

        register(new Skill(
                "creative-writer",
                "Creative Writer",
                "You are a creative writing assistant. Help with:\n1. Story and narrative development\n2. Character creation and development\n3. Writing prompts and inspiration\n4. Poetry and prose crafting\n5. Editing and refining written work\n\nBe imaginative and provide rich, detailed responses.",
                "Creative"
        ));
    }

    public void register(Skill skill) {
        skills.put(skill.id(), skill);
    }

    public Skill getSkill(String skillId) {
        return skills.get(skillId);
    }

    public Map<String, Skill> getAllSkills() {
        return new HashMap<>(skills);
    }

    public boolean hasSkill(String skillId) {
        return skills.containsKey(skillId);
    }

    public record Skill(String id, String name, String systemPrompt, String category) {}
}
