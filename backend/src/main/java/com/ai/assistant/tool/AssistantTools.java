package com.ai.assistant.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Component
public class AssistantTools {

    @Tool(description = "Get the current date and time")
    public String getCurrentDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Tool(description = "Calculate a mathematical expression")
    public String calculate(@ToolParam(description = "Mathematical expression like '2+2' or '10*5'") String expression) {
        try {
            expression = expression.replaceAll("[^0-9+\\-*/().\\s]", "");
            javax.script.ScriptEngine engine = new javax.script.ScriptEngineManager()
                    .getEngineByName("JavaScript");
            Object result = engine.eval(expression);
            return "Result: " + result;
        } catch (Exception e) {
            return "Error calculating expression: " + e.getMessage();
        }
    }

    @Tool(description = "Search for information about a topic")
    public String searchInfo(@ToolParam(description = "Topic to search for") String topic) {
        Map<String, String> mockResults = new HashMap<>();
        mockResults.put("weather", "Current weather: Sunny, 25°C, Humidity 60%");
        mockResults.put("news", "Top news: AI technology continues to advance rapidly in 2026");
        mockResults.put("time", "Current time is: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        
        String lowerTopic = topic.toLowerCase();
        for (Map.Entry<String, String> entry : mockResults.entrySet()) {
            if (lowerTopic.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "Information about '" + topic + "': This is a simulated search result. In production, this would connect to a real search API.";
    }

    @Tool(description = "Generate a random number between min and max")
    public String getRandomNumber(
            @ToolParam(description = "Minimum value") int min,
            @ToolParam(description = "Maximum value") int max) {
        Random random = new Random();
        int result = random.nextInt(max - min + 1) + min;
        return "Random number between " + min + " and " + max + ": " + result;
    }

    @Tool(description = "Convert currency amounts")
    public String convertCurrency(
            @ToolParam(description = "Amount to convert") double amount,
            @ToolParam(description = "Source currency code (e.g., USD, CNY)") String from,
            @ToolParam(description = "Target currency code (e.g., EUR, JPY)") String to) {
        Map<String, Double> mockRates = new HashMap<>();
        mockRates.put("USDCNY", 7.25);
        mockRates.put("CNYUSD", 0.138);
        mockRates.put("USDEUR", 0.92);
        mockRates.put("EURUSD", 1.09);
        mockRates.put("USDJPY", 154.50);
        mockRates.put("JPYUSD", 0.0065);
        
        String key = from.toUpperCase() + to.toUpperCase();
        Double rate = mockRates.get(key);
        
        if (rate != null) {
            double result = amount * rate;
            return amount + " " + from + " = " + String.format("%.2f", result) + " " + to + " (Rate: " + rate + ")";
        }
        return "Exchange rate for " + from + " to " + to + " not available in mock data.";
    }

    @Tool(description = "Get today's date")
    public String getTodayDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
