package com.blog.server.service;

import com.blog.server.common.PageResult;
import com.blog.server.entity.MediaFile;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {

    MediaFile uploadFile(MultipartFile file, Long userId);

    PageResult<MediaFile> listFiles(Integer page, Integer size, String fileType, String keyword);

    MediaFile getFile(Long id);

    void updateAltText(Long id, String altText);

    void deleteFile(Long id);
}
