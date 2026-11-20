package com.example.backend.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CalculatorTools {

    @Tool("执行数学表达式计算，支持加减乘除和括号")
    public double calculate(
            @P("数学表达式，例如 2+3*4 或 (10-2)/4") String expression) {
        log.info("[Tool] calculate called: expression={}", expression);
        return evaluateExpression(expression);
    }

    @Tool("计算两个数的加法")
    public double add(
            @P("第一个加数") double a,
            @P("第二个加数") double b) {
        log.info("[Tool] add called: a={}, b={}", a, b);
        return a + b;
    }

    @Tool("计算两个数的减法")
    public double subtract(
            @P("被减数") double a,
            @P("减数") double b) {
        log.info("[Tool] subtract called: a={}, b={}", a, b);
        return a - b;
    }

    @Tool("计算两个数的乘法")
    public double multiply(
            @P("第一个乘数") double a,
            @P("第二个乘数") double b) {
        log.info("[Tool] multiply called: a={}, b={}", a, b);
        return a * b;
    }

    @Tool("计算两个数的除法")
    public double divide(
            @P("被除数") double a,
            @P("除数") double b) {
        log.info("[Tool] divide called: a={}, b={}", a, b);
        if (b == 0) {
            throw new IllegalArgumentException("除数不能为零");
        }
        return a / b;
    }

    @Tool("计算一个数的平方根")
    public double sqrt(
            @P("要计算平方根的数") double x) {
        log.info("[Tool] sqrt called: x={}", x);
        if (x < 0) {
            throw new IllegalArgumentException("不能计算负数的平方根");
        }
        return Math.sqrt(x);
    }

    @Tool("计算一个数的乘方")
    public double power(
            @P("底数") double base,
            @P("指数") double exponent) {
        log.info("[Tool] power called: base={}, exponent={}", base, exponent);
        return Math.pow(base, exponent);
    }

    private double evaluateExpression(String expression) {
        expression = expression.replaceAll("\\s+", "");
        try {
            return parseAddSubtract(expression);
        } catch (Exception e) {
            throw new IllegalArgumentException("无法解析表达式: " + expression, e);
        }
    }

    private double parseAddSubtract(String expr) {
        double result = parseMultiplyDivide(expr, true);
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (c == '+' || c == '-') {
                String rest = expr.substring(i + 1);
                double[] parsed = parseMultiplyDivideWithPos(rest, true);
                if (c == '+') {
                    result += parsed[0];
                } else {
                    result -= parsed[0];
                }
                i += 1 + (int) parsed[1];
            } else {
                i++;
            }
        }
        return result;
    }

    private double parseMultiplyDivide(String expr, boolean first) {
        double[] r = parseMultiplyDivideWithPos(expr, first);
        return r[0];
    }

    private double[] parseMultiplyDivideWithPos(String expr, boolean first) {
        double[] r;
        if (first) {
            r = parseValue(expr);
        } else {
            r = new double[]{0, 0};
        }
        double result = r[0];
        int i = (int) r[1];
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (c == '*' || c == '/') {
                double[] next = parseValue(expr.substring(i + 1));
                if (c == '*') {
                    result *= next[0];
                } else {
                    result /= next[0];
                }
                i += 1 + (int) next[1];
            } else {
                break;
            }
        }
        return new double[]{result, i};
    }

    private double[] parseValue(String expr) {
        if (expr.isEmpty()) {
            throw new IllegalArgumentException("表达式不完整");
        }
        char first = expr.charAt(0);
        if (first == '(') {
            int depth = 1;
            int i = 1;
            while (i < expr.length() && depth > 0) {
                if (expr.charAt(i) == '(') depth++;
                if (expr.charAt(i) == ')') depth--;
                i++;
            }
            String inner = expr.substring(1, i - 1);
            double val = parseAddSubtract(inner);
            return new double[]{val, i};
        }
        if (first == '-') {
            double[] r = parseValue(expr.substring(1));
            return new double[]{-r[0], 1 + (int) r[1]};
        }
        int i = 0;
        while (i < expr.length() &&
                (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
            i++;
        }
        if (i == 0) {
            throw new IllegalArgumentException("无效的表达式: " + expr);
        }
        return new double[]{Double.parseDouble(expr.substring(0, i)), i};
    }
}
