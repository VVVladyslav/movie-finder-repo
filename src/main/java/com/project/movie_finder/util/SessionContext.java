package com.project.movie_finder.util;

/**
 Thread-local holder of the current session id set by SessionIdFilter.
 Methods: set(id) stores for the current request thread; get() reads; clear() removes to prevent leaks.
 Utility class (final, private ctor) — not intended to be instantiated.
 */

public final class SessionContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private SessionContext() {
    }

    public static void set(String sessionId) {
        CURRENT.set(sessionId);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
