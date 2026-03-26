package com.blog.server.service;

import com.blog.server.dto.request.CommentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class CommentServiceConcurrencyTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate.keys("comment_lock:*").forEach(redisTemplate::delete);
    }

    @Test
    void testCommentCreationIdempotency() throws InterruptedException {
        int threads = 10;
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger throwCounter = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        CommentRequest request = new CommentRequest();
        request.setArticleId(9999L);
        request.setAuthorName("Testing Bot");
        request.setContent("This is an idempotent comment");

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    commentService.createComment(request, "192.168.1.101", "Concurrency-Test-Agent");
                    successCounter.incrementAndGet();
                } catch (Exception e) {
                    if (e.getMessage().contains("Please do not submit duplicate messages")) {
                        throwCounter.incrementAndGet();
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();

        assertEquals(1, successCounter.get(), "Only 1 comment should succeed due to SETNX idempotent locking.");
        assertEquals(9, throwCounter.get(), "9 duplicate requests must throw duplicate constraint errors.");
    }
}
