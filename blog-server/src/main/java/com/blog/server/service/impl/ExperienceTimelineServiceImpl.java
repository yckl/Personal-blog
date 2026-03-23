package com.blog.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.server.entity.ExperienceTimeline;
import com.blog.server.mapper.ExperienceTimelineMapper;
import com.blog.server.service.ExperienceTimelineService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExperienceTimelineServiceImpl extends ServiceImpl<ExperienceTimelineMapper, ExperienceTimeline> implements ExperienceTimelineService {

    @Override
    public List<ExperienceTimeline> getPublicTimeline() {
        return this.list(new LambdaQueryWrapper<ExperienceTimeline>()
                .eq(ExperienceTimeline::getIsVisible, true)
                .orderByDesc(ExperienceTimeline::getYear)
                .orderByAsc(ExperienceTimeline::getSortOrder));
    }

    @Override
    public List<ExperienceTimeline> getAllTimeline() {
        return this.list(new LambdaQueryWrapper<ExperienceTimeline>()
                .orderByDesc(ExperienceTimeline::getYear)
                .orderByAsc(ExperienceTimeline::getSortOrder));
    }

    @Override
    public Long createTimeline(ExperienceTimeline timeline) {
        if (timeline.getIsVisible() == null) timeline.setIsVisible(true);
        if (timeline.getSortOrder() == null) timeline.setSortOrder(0);
        this.save(timeline);
        return timeline.getId();
    }

    @Override
    public void updateTimeline(Long id, ExperienceTimeline timeline) {
        timeline.setId(id);
        this.updateById(timeline);
    }

    @Override
    public void deleteTimeline(Long id) {
        this.removeById(id);
    }

    @Override
    public void updateOrder(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (int i = 0; i < ids.size(); i++) {
            ExperienceTimeline t = new ExperienceTimeline();
            t.setId(ids.get(i));
            t.setSortOrder(i * 10);
            this.updateById(t);
        }
    }
}
