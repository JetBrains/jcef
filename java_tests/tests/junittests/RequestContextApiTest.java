package tests.junittests;

import org.cef.browser.CefRequestContext;
import org.cef.browser.CefRequestContextSettings;
import org.cef.network.CefCookie;
import org.cef.network.CefCookieManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// Checks CefRequestContext API exposed for isolated/shared browsing profiles
// (settings, shared storage, cookie manager, http-cache purge). Runs in both JNI and remote modes.
@ExtendWith(TestSetupExtension.class)
public class RequestContextApiTest {
    private static final long TIMEOUT_MS = 10_000;

    @Test
    public void defaultContextIsInMemory() {
        CefRequestContext ctx = CefRequestContext.createContext(null);
        assertNotNull(ctx);
        try {
            assertFalse(ctx.isGlobal());
            assertEquals("", ctx.getCachePath());
            assertTrue(ctx.isSame(ctx));
            assertFalse(ctx.isSame(CefRequestContext.getGlobalContext()));
            assertFalse(ctx.isSharingWith(CefRequestContext.getGlobalContext()));
        } finally {
            ctx.dispose();
        }
    }

    @Test
    public void contextWithCachePath() {
        String root = CefRequestContext.getGlobalContext().getCachePath();
        assertNotNull(root);
        if (root.isEmpty()) {
            // Global context is in-memory: cache_path of a custom context must be under root_cache_path,
            // which is unknown here, so just check that an invalid path falls back to in-memory storage.
            CefRequestContextSettings settings = new CefRequestContextSettings();
            settings.cache_path = "relative/invalid/path";
            CefRequestContext ctx = CefRequestContext.createContext(settings, null);
            assertNotNull(ctx);
            try {
                assertEquals("", ctx.getCachePath());
            } finally {
                ctx.dispose();
            }
            return;
        }

        String path = Path.of(root).resolve("RequestContextApiTest_" + System.nanoTime()).toString();
        CefRequestContextSettings settings = new CefRequestContextSettings();
        settings.cache_path = path;

        CefRequestContext ctx1 = CefRequestContext.createContext(settings, null);
        CefRequestContext ctx2 = CefRequestContext.createContext(settings, null);
        assertNotNull(ctx1);
        assertNotNull(ctx2);
        try {
            assertEquals(path, ctx1.getCachePath());
            assertEquals(path, ctx2.getCachePath());
            assertFalse(ctx1.isGlobal());
            assertFalse(ctx1.isSame(ctx2));
            // Two contexts with the same cache_path share the same storage.
            assertTrue(ctx1.isSharingWith(ctx2));
            assertTrue(ctx2.isSharingWith(ctx1));
            assertFalse(ctx1.isSharingWith(CefRequestContext.getGlobalContext()));
        } finally {
            ctx1.dispose();
            ctx2.dispose();
        }
    }

    @Test
    public void sharedContext() {
        CefRequestContext anchor = CefRequestContext.createContext(null);
        assertNotNull(anchor);
        CefRequestContext shared = CefRequestContext.createContext(anchor, null);
        assertNotNull(shared);
        try {
            assertFalse(shared.isGlobal());
            assertFalse(shared.isSame(anchor));
            assertTrue(shared.isSharingWith(anchor));
            assertTrue(anchor.isSharingWith(shared));
            assertEquals(anchor.getCachePath(), shared.getCachePath());
            assertFalse(shared.isSharingWith(CefRequestContext.getGlobalContext()));
        } finally {
            shared.dispose();
            anchor.dispose();
        }

        assertNull(CefRequestContext.createContext((CefRequestContext) null, null));
    }

    @Test
    public void cookieManagerIsIsolated() throws InterruptedException {
        CefRequestContext ctx = CefRequestContext.createContext(null);
        assertNotNull(ctx);
        try {
            CefCookieManager manager = ctx.getCookieManager();
            assertNotNull(manager);

            final String name = "RequestContextApiTest_" + System.nanoTime();
            final String url = "https://request-context-api-test.example.com/";
            Date now = new Date();
            CefCookie cookie = new CefCookie(name, "value", "request-context-api-test.example.com", "/",
                    true, false, now, now, false, null);
            assertTrue(manager.setCookie(url, cookie));

            // The cookie must become visible in the isolated context...
            assertTrue(waitForCookie(manager, name, true), "cookie wasn't set in the isolated context");
            // ...and must not leak into the global one.
            assertFalse(waitForCookie(CefCookieManager.getGlobalManager(), name, false), "cookie leaked into the global context");

            // A context sharing storage with |ctx| sees the same cookies.
            CefRequestContext shared = CefRequestContext.createContext(ctx, null);
            assertNotNull(shared);
            try {
                assertTrue(waitForCookie(shared.getCookieManager(), name, true), "cookie isn't visible in the shared context");
            } finally {
                shared.dispose();
            }

            assertTrue(manager.deleteCookies("", ""));
            assertFalse(waitForCookie(manager, name, false), "cookie wasn't deleted");
        } finally {
            ctx.dispose();
        }
    }

    @Test
    public void clearHttpCacheAndAuthCredentials() throws InterruptedException {
        CefRequestContext ctx = CefRequestContext.createContext(null);
        assertNotNull(ctx);
        try {
            CountDownLatch latch = new CountDownLatch(2);
            ctx.clearHttpCache(latch::countDown);
            ctx.clearHttpAuthCredentials(latch::countDown);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "completion callbacks weren't invoked");
        } finally {
            ctx.dispose();
        }
    }

    // Polls the cookie manager until a cookie with the given name is (expectPresent=true) or is not
    // (expectPresent=false) found. Returns the final presence state.
    private static boolean waitForCookie(CefCookieManager manager, String name, boolean expectPresent) throws InterruptedException {
        long deadline = System.currentTimeMillis() + (expectPresent ? TIMEOUT_MS : 2_000);
        boolean present = !expectPresent;
        while (System.currentTimeMillis() < deadline) {
            present = collectCookieNames(manager).contains(name);
            if (present == expectPresent)
                return present;
            Thread.sleep(100);
        }
        return present;
    }

    private static Set<String> collectCookieNames(CefCookieManager manager) throws InterruptedException {
        Set<String> names = Collections.synchronizedSet(new HashSet<>());
        CountDownLatch done = new CountDownLatch(1);
        boolean started = manager.visitAllCookies((cookie, count, total, delete) -> {
            names.add(cookie.name);
            if (count + 1 >= total)
                done.countDown();
            return true;
        });
        if (!started)
            return names;
        // The visitor is never called when there are no cookies, so just wait a bit.
        done.await(500, TimeUnit.MILLISECONDS);
        return names;
    }
}
