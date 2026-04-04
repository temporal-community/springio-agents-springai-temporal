// ABOUTME: Defines the tools available to the agent - weather lookup and calculator.
// Tools are registered via Spring AI's @Tool annotation for automatic schema generation.

package io.temporal.ai.workshop.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AgentTools {

    private static final Map<String, String> WEATHER_DATA = Map.of(
            "san francisco", "62F, foggy",
            "new york", "75F, sunny",
            "london", "58F, rainy",
            "tokyo", "80F, humid",
            "paris", "68F, partly cloudy"
    );

    @Tool(description = "Get the current weather for a given city. Returns temperature and conditions.")
    public String getWeather(@ToolParam(description = "The city name to look up weather for") String city) {
        String weather = WEATHER_DATA.get(city.toLowerCase().trim());
        if (weather != null) {
            return "Weather in " + city + ": " + weather;
        }
        return "Weather in " + city + ": 70F, clear skies (default)";
    }

    @Tool(description = "Evaluate a mathematical expression. Supports +, -, *, / and parentheses. Example: '(2 + 3) * 4'")
    public String calculate(@ToolParam(description = "The mathematical expression to evaluate") String expression) {
        try {
            double result = evaluateExpression(expression.trim());
            return expression.trim() + " = " + formatResult(result);
        } catch (Exception e) {
            return "Error evaluating '" + expression + "': " + e.getMessage();
        }
    }

    private String formatResult(double result) {
        if (result == Math.floor(result) && !Double.isInfinite(result)) {
            return String.valueOf((long) result);
        }
        return String.valueOf(result);
    }

    /**
     * Simple recursive descent parser for math expressions.
     * Supports: +, -, *, /, parentheses, and decimal numbers.
     */
    private double evaluateExpression(String expr) {
        return new ExpressionParser(expr).parse();
    }

    private static class ExpressionParser {
        private final String input;
        private int pos = 0;

        ExpressionParser(String input) {
            this.input = input;
        }

        double parse() {
            double result = parseAddSub();
            if (pos < input.length()) {
                throw new IllegalArgumentException("Unexpected character: " + input.charAt(pos));
            }
            return result;
        }

        private double parseAddSub() {
            double left = parseMulDiv();
            while (pos < input.length()) {
                skipSpaces();
                if (pos >= input.length()) break;
                char op = input.charAt(pos);
                if (op != '+' && op != '-') break;
                pos++;
                double right = parseMulDiv();
                left = (op == '+') ? left + right : left - right;
            }
            return left;
        }

        private double parseMulDiv() {
            double left = parseUnary();
            while (pos < input.length()) {
                skipSpaces();
                if (pos >= input.length()) break;
                char op = input.charAt(pos);
                if (op != '*' && op != '/') break;
                pos++;
                double right = parseUnary();
                left = (op == '*') ? left * right : left / right;
            }
            return left;
        }

        private double parseUnary() {
            skipSpaces();
            if (pos < input.length() && input.charAt(pos) == '-') {
                pos++;
                return -parseAtom();
            }
            return parseAtom();
        }

        private double parseAtom() {
            skipSpaces();
            if (pos < input.length() && input.charAt(pos) == '(') {
                pos++;
                double result = parseAddSub();
                skipSpaces();
                if (pos < input.length() && input.charAt(pos) == ')') {
                    pos++;
                } else {
                    throw new IllegalArgumentException("Missing closing parenthesis");
                }
                return result;
            }
            return parseNumber();
        }

        private double parseNumber() {
            skipSpaces();
            int start = pos;
            while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
                pos++;
            }
            if (start == pos) {
                throw new IllegalArgumentException("Expected number at position " + pos);
            }
            return Double.parseDouble(input.substring(start, pos));
        }

        private void skipSpaces() {
            while (pos < input.length() && input.charAt(pos) == ' ') {
                pos++;
            }
        }
    }
}
