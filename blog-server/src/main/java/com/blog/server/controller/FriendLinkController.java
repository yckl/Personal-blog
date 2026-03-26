package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.common.Result;
import com.blog.server.entity.FriendLink;
import com.blog.server.mapper.FriendLinkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Friend Link Controller — public listing + admin CRUD.
 */
@RestController
@RequiredArgsConstructor
public class FriendLinkController {

    private final FriendLinkMapper friendLinkMapper;

    // ============ Public ============

    @GetMapping("/api/public/links")
    public Result<List<FriendLink>> getPublicLinks() {
        return Result.ok(friendLinkMapper.selectList(
                new LambdaQueryWrapper<FriendLink>()
                        .eq(FriendLink::getStatus, 1)
                        .orderByAsc(FriendLink::getSortOrder)));
    }

    // ============ Admin ============

    @GetMapping("/api/admin/links")
    public Result<List<FriendLink>> listAll() {
        return Result.ok(friendLinkMapper.selectList(
                new LambdaQueryWrapper<FriendLink>()
                        .orderByAsc(FriendLink::getSortOrder)));
    }

    @PostMapping("/api/admin/links")
    public Result<FriendLink> create(@RequestBody FriendLink link) {
        if (link.getSortOrder() == null) link.setSortOrder(0);
        if (link.getStatus() == null) link.setStatus(1);
        friendLinkMapper.insert(link);
        return Result.ok(link);
    }

    @PutMapping("/api/admin/links/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody FriendLink link) {
        link.setId(id);
        friendLinkMapper.updateById(link);
        return Result.ok();
    }

    @DeleteMapping("/api/admin/links/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        friendLinkMapper.deleteById(id);
        return Result.ok();
    }
}
