package com.blog.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.server.common.PageResult;
import com.blog.server.entity.ContactMessage;
import com.blog.server.mapper.ContactMessageMapper;
import com.blog.server.service.ContactMessageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class ContactMessageServiceImpl extends ServiceImpl<ContactMessageMapper, ContactMessage> implements ContactMessageService {

    @Override
    public Long submitMessage(ContactMessage message) {
        message.setStatus("PENDING");
        this.save(message);
        return message.getId();
    }

    @Override
    public PageResult<ContactMessage> listMessages(Integer page, Integer size, String status, String keyword) {
        Page<ContactMessage> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 10);
        LambdaQueryWrapper<ContactMessage> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(ContactMessage::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ContactMessage::getName, keyword)
                             .or().like(ContactMessage::getEmail, keyword)
                             .or().like(ContactMessage::getMessage, keyword));
        }
        wrapper.orderByDesc(ContactMessage::getCreatedAt);
        
        Page<ContactMessage> result = this.page(pageParam, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void replyMessage(Long id, String replyContent) {
        ContactMessage msg = this.getById(id);
        if (msg != null) {
            msg.setReplyContent(replyContent);
            msg.setRepliedAt(LocalDateTime.now());
            msg.setStatus("REPLIED");
            this.updateById(msg);
            
            // In a real app, send actual email here.
        }
    }

    @Override
    public void deleteMessage(Long id) {
        this.removeById(id);
    }
}
