package com.example.backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiScopeTest {

    @Test
    @DisplayName("路径映射到所需 scope，路径变量能匹配")
    void routesResolveToScope() {
        assertEquals(ApiScope.ASSIST, ApiScope.routeFor("POST", "/v1/assist/stream").scope());
        assertEquals(ApiScope.FEEDBACK, ApiScope.routeFor("POST", "/v1/assist/feedback").scope());
        assertEquals(ApiScope.KNOWLEDGE_READ, ApiScope.routeFor("GET", "/v1/knowledge/documents").scope());
        assertEquals(ApiScope.KNOWLEDGE_WRITE,
                ApiScope.routeFor("POST", "/v1/knowledge/documents").scope());
        assertEquals(ApiScope.KNOWLEDGE_WRITE,
                ApiScope.routeFor("DELETE", "/v1/knowledge/documents/doc-1").scope());
    }

    @Test
    @DisplayName("能力发现不需要 scope")
    void diseasesNeedsNoScope() {
        assertNull(ApiScope.routeFor("GET", "/v1/diseases").scope());
    }

    @Test
    @DisplayName("没登记的路径或方法不匹配，返回 null，由拦截器默认拒绝")
    void unknownRouteReturnsNull() {
        assertNull(ApiScope.routeFor("GET", "/v1/assist/stream"));
        assertNull(ApiScope.routeFor("POST", "/v1/assist/unknown"));
        assertNull(ApiScope.routeFor("DELETE", "/v1/knowledge/documents"));
    }

    @Test
    @DisplayName("逗号分隔串解析成集合，不认识的跳过")
    void parseSkipsUnknown() {
        assertEquals(EnumSet.of(ApiScope.ASSIST, ApiScope.FEEDBACK),
                ApiScope.parse("assist, feedback,bogus"));
        assertEquals(EnumSet.noneOf(ApiScope.class), ApiScope.parse(null));
        assertEquals("assist,knowledge:write",
                ApiScope.join(EnumSet.of(ApiScope.KNOWLEDGE_WRITE, ApiScope.ASSIST)));
    }
}
