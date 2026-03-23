package com.blog.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.server.entity.ExperienceTimeline;

import java.util.List;

public interface ExperienceTimelineService extends IService<ExperienceTimeline> {
    List<ExperienceTimeline> getPublicTimeline();
    List<ExperienceTimeline> getAllTimeline();
    Long createTimeline(ExperienceTimeline timeline);
    void updateTimeline(Long id, ExperienceTimeline timeline);
    void deleteTimeline(Long id);
    void updateOrder(List<Long> ids);
}
