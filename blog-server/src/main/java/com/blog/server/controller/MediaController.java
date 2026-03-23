package com.blog.server.controller;

import com.blog.server.common.PageResult;
import com.blog.server.common.Result;
import com.blog.server.entity.MediaFile;
import com.blog.server.entity.SysUser;
import com.blog.server.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public Result<MediaFile> upload(@RequestParam("file") MultipartFile file,
                                    @AuthenticationPrincipal SysUser user) {
        return Result.ok(mediaService.uploadFile(file, user.getId()));
    }

    @GetMapping("/page")
    public Result<PageResult<MediaFile>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String keyword) {
        return Result.ok(mediaService.listFiles(page, size, fileType, keyword));
    }

    @GetMapping("/{id}")
    public Result<MediaFile> getFile(@PathVariable Long id) {
        return Result.ok(mediaService.getFile(id));
    }

    @PutMapping("/{id}/alt")
    public Result<Void> updateAlt(@PathVariable Long id,
                                  @RequestBody Map<String, String> body) {
        mediaService.updateAltText(id, body.get("altText"));
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mediaService.deleteFile(id);
        return Result.ok();
    }
}
