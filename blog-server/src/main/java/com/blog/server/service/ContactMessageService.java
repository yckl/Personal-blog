package com.blog.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.server.entity.ContactMessage;
import com.blog.server.common.PageResult;

public interface ContactMessageService extends IService<ContactMessage> {
    Long submitMessage(ContactMessage message);
    PageResult<ContactMessage> listMessages(Integer page, Integer size, String status, String keyword);
    void replyMessage(Long id, String replyContent);
    void deleteMessage(Long id);
}
