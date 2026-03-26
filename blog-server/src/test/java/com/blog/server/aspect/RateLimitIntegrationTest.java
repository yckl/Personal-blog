package com.blog.server.aspect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
public class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        // Clean out any keys affecting our rate limit tracking for exactly this controller mapping
        redisTemplate.keys("rate_limit:*").forEach(redisTemplate::delete);
    }

    @Test
    void testRateLimit_WithConcurrency() throws InterruptedException {
        int totalRequests = 100;
        // The RateLimit annotation on listPublicArticles allows 50 requests.
        // We will assert that exactly 50 status 200s and 50 status 429s are generated.

        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger tooManyRequestsCounter = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalRequests);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready to rush simultaneously
                    int status = mockMvc.perform(get("/api/articles")
                            .header("X-Forwarded-For", "192.168.1.100"))
                            .andReturn().getResponse().getStatus();
                    
                    if (status == 200) {
                        successCounter.incrementAndGet();
                    } else if (status == 429) {
                        tooManyRequestsCounter.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Ignore execution failures for test counting
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Fire all threads!
        startLatch.countDown();
        doneLatch.await();

        assertEquals(50, successCounter.get(), "50 requests should pass through the RateLimiter.");
        assertEquals(50, tooManyRequestsCounter.get(), "50 requests should be bounced by 429 Too Many Requests.");
    }
}
