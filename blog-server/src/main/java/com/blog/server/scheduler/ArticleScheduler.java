package com.blog.server.scheduler;

import com.blog.server.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleScheduler {

    private final ArticleService articleService;

    /**
     * Run every minute to check for scheduled articles that should be published.
     */
    @Scheduled(fixedRate = 60000)
    public void publishScheduledArticles() {
        try {
            articleService.publishScheduledArticles();
        } catch (Exception e) {
            log.error("Error publishing scheduled articles", e);
        }
    }
}
