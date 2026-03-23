package com.blog.server.controller;

import com.blog.server.common.Result;
import com.blog.server.entity.ExperienceTimeline;
import com.blog.server.service.ExperienceTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/timeline")
@RequiredArgsConstructor
public class ExperienceTimelineController {

    private final ExperienceTimelineService timelineService;

    @GetMapping
    public Result<List<ExperienceTimeline>> getAllTimeline() {
        return Result.ok(timelineService.getAllTimeline());
    }

    @PostMapping
    public Result<Long> createTimeline(@RequestBody ExperienceTimeline timeline) {
        return Result.ok(timelineService.createTimeline(timeline));
    }

    @PutMapping("/{id}")
    public Result<Void> updateTimeline(@PathVariable Long id, @RequestBody ExperienceTimeline timeline) {
        timelineService.updateTimeline(id, timeline);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTimeline(@PathVariable Long id) {
        timelineService.deleteTimeline(id);
        return Result.ok(null);
    }

    @PutMapping("/order")
    public Result<Void> updateOrder(@RequestBody List<Long> ids) {
        timelineService.updateOrder(ids);
        return Result.ok(null);
    }
}
