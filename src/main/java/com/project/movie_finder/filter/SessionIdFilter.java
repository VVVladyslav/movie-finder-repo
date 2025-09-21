package com.project.movie_finder.filter;

import com.project.movie_finder.util.SessionContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 Servlet filter that guarantees an anonymous per-browser session id via the "mf.sid" cookie.
 If absent, generates a UUID and sets HttpOnly, SameSite=Lax, Secure (when HTTPS), Max-Age 7 days.
 The id is exposed to backend code through SessionContext for the duration of the request (then cleared).
 Methods:
 doFilterInternal(...): reads/creates the session id, sets cookie, stores in SessionContext, continues the chain.
 readSessionIdFromCookies(...): extracts "mf.sid" from incoming cookies.
 addSessionCookie(...): writes Set-Cookie with proper attributes.
 */

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SessionIdFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "mf.sid";
    private static final int MAX_AGE_SECONDS = 7 * 24 * 60 * 60;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String sessionId = readSessionIdFromCookies(request);

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
            addSessionCookie(response, request.isSecure(), sessionId);
        }

        SessionContext.set(sessionId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SessionContext.clear();
        }
    }

    private static String readSessionIdFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie c : cookies) {
            if (COOKIE_NAME.equals(c.getName())) {
                String val = c.getValue();
                if (val != null && !val.isBlank()) {
                    return val;
                }
            }
        }
        return null;
    }

    private static void addSessionCookie(HttpServletResponse response, boolean secure, String value) {
        StringBuilder sb = new StringBuilder()
                .append(COOKIE_NAME).append("=").append(value)
                .append("; Path=/")
                .append("; Max-Age=").append(MAX_AGE_SECONDS)
                .append("; HttpOnly")
                .append("; SameSite=Lax");

        if (secure) {
            sb.append("; Secure");
        }

        response.addHeader("Set-Cookie", sb.toString());
    }
}