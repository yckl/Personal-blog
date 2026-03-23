package com.blog.server.controller;

import com.blog.server.common.PageResult;
import com.blog.server.common.Result;
import com.blog.server.entity.ContactMessage;
import com.blog.server.service.ContactMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/messages")
@RequiredArgsConstructor
public class ContactMessageController {

    private final ContactMessageService messageService;

    @GetMapping
    public Result<PageResult<ContactMessage>> listMessages(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(messageService.listMessages(page, size, status, keyword));
    }

    @PostMapping("/{id}/reply")
    public Result<Void> replyMessage(@PathVariable Long id, @RequestBody Map<String, String> body) {
        messageService.replyMessage(id, body.get("replyContent"));
        return Result.ok(null);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ContactMessage msg = new ContactMessage();
        msg.setId(id);
        msg.setStatus(body.get("status"));
        messageService.updateById(msg);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteMessage(@PathVariable Long id) {
        messageService.deleteMessage(id);
        return Result.ok(null);
    }
}
