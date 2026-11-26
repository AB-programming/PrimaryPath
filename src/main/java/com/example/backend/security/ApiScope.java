package com.example.backend.security;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 对外接口的 scope：key 的能力清单，平铺无层级。
 * 新增 /v1 接口时在 ROUTES 登记：scope 为 null 表示任何有效 key 都可调；没登记的路径默认 403。
 */
public enum ApiScope {

    ASSIST("assist"),
    FEEDBACK("feedback"),
    KNOWLEDGE_READ("knowledge:read"),
    KNOWLEDGE_WRITE("knowledge:write");

    /** 一条路由：方法、路径模式、所需 scope */
    public record Route(String method, String pattern, ApiScope scope, PathPattern compiled) {
    }

    private static final PathPatternParser PARSER = new PathPatternParser();

    private static final List<Route> ROUTES = List.of(
            route("POST", "/v1/assist/stream", ASSIST),
            route("POST", "/v1/assist/feedback", FEEDBACK),
            route("GET", "/v1/knowledge/documents", KNOWLEDGE_READ),
            route("POST", "/v1/knowledge/documents", KNOWLEDGE_WRITE),
            route("DELETE", "/v1/knowledge/documents/*", KNOWLEDGE_WRITE),
            route("GET", "/v1/diseases", null));

    private final String value;

    ApiScope(String value) {
        this.value = value;
    }

    /** 库里存的值 */
    public String value() {
        return value;
    }

    private static Route route(String method, String pattern, ApiScope scope) {
        return new Route(method, pattern, scope, PARSER.parse(pattern));
    }

    /** 按方法和路径找路由，没登记返回 null */
    public static Route routeFor(String method, String path) {
        PathContainer container = PathContainer.parsePath(path);
        for (Route r : ROUTES) {
            if (r.method().equals(method) && r.compiled().matches(container)) {
                return r;
            }
        }
        return null;
    }

    public static Optional<ApiScope> fromValue(String value) {
        for (ApiScope scope : values()) {
            if (scope.value.equals(value)) {
                return Optional.of(scope);
            }
        }
        return Optional.empty();
    }

    /** 解析逗号分隔的 scope 串。不认识的值跳过，兼容旧版本写入的数据 */
    public static Set<ApiScope> parse(String csv) {
        Set<ApiScope> result = EnumSet.noneOf(ApiScope.class);
        if (csv == null) {
            return result;
        }
        for (String part : csv.split(",")) {
            fromValue(part.trim()).ifPresent(result::add);
        }
        return result;
    }

    public static String join(Set<ApiScope> scopes) {
        return scopes.stream().map(ApiScope::value).collect(Collectors.joining(","));
    }
}
