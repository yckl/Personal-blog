package com.blog.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.server.common.PageResult;
import com.blog.server.entity.MediaFile;
import com.blog.server.exception.BusinessException;
import com.blog.server.mapper.MediaFileMapper;
import com.blog.server.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MediaFileMapper mediaFileMapper;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Value("${upload.url-prefix:/files}")
    private String urlPrefix;

    @Value("${upload.max-size:52428800}")
    private long maxFileSize; // 50MB default

    /** Allowed MIME types */
    private static final Set<String> ALLOWED_MIME = Set.of(
            // Images
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml", "image/bmp",
            // Video
            "video/mp4", "video/webm", "video/quicktime",
            // Audio
            "audio/mpeg", "audio/wav", "audio/ogg", "audio/flac",
            // Documents
            "application/pdf", "application/zip", "application/x-zip-compressed",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final int THUMBNAIL_SIZE = 400;

    @Override
    @SuppressWarnings("null")
    public MediaFile uploadFile(MultipartFile file, Long userId) {
        // ---- Validation ----
        if (file.isEmpty()) {
            throw new BusinessException("File is empty");
        }
        if (file.getSize() > maxFileSize) {
            throw new BusinessException("File exceeds maximum size of " + (maxFileSize / 1024 / 1024) + "MB");
        }
        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME.contains(mimeType)) {
            throw new BusinessException("File type not allowed: " + mimeType);
        }

        // ---- Storage ----
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String baseName = UUID.randomUUID().toString().replace("-", "");
        String storedName = baseName + extension;
        String relativePath = datePath + "/" + storedName;

        Path dirPath = Paths.get(uploadPath, datePath);
        Path filePath = Paths.get(uploadPath, relativePath);

        try {
            Files.createDirectories(dirPath);
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            log.error("Failed to upload file", e);
            throw new BusinessException("File upload failed");
        }

        // ---- Build entity ----
        MediaFile media = new MediaFile();
        media.setOriginalName(originalName);
        media.setStoredName(storedName);
        media.setFilePath(relativePath);
        media.setFileUrl(urlPrefix + "/" + relativePath);
        media.setFileSize(file.getSize());
        media.setMimeType(mimeType);
        media.setFileType(getFileType(mimeType));
        media.setUploadedBy(userId);

        // ---- Image-specific processing ----
        if ("image".equals(media.getFileType()) && !mimeType.contains("svg")) {
            try {
                BufferedImage original = ImageIO.read(filePath.toFile());
                if (original != null) {
                    media.setWidth(original.getWidth());
                    media.setHeight(original.getHeight());

                    // Generate thumbnail
                    String thumbName = baseName + "_thumb" + extension;
                    String thumbRelPath = datePath + "/" + thumbName;
                    Path thumbPath = Paths.get(uploadPath, thumbRelPath);
                    generateThumbnail(original, thumbPath, extension);
                    media.setThumbnailUrl(urlPrefix + "/" + thumbRelPath);

                    // Generate WebP version (except for GIFs)
                    if (!"image/gif".equals(mimeType)) {
                        String webpName = baseName + ".webp";
                        String webpRelPath = datePath + "/" + webpName;
                        Path webpPath = Paths.get(uploadPath, webpRelPath);
                        boolean webpWritten = ImageIO.write(original, "webp", webpPath.toFile());
                        if (webpWritten) {
                            media.setWebpUrl(urlPrefix + "/" + webpRelPath);
                        }
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to process image: {}", originalName, e);
                // Non-fatal — the original file is still uploaded
            }
        }

        mediaFileMapper.insert(media);
        return media;
    }

    @Override
    public PageResult<MediaFile> listFiles(Integer page, Integer size, String fileType, String keyword) {
        Page<MediaFile> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 20);

        LambdaQueryWrapper<MediaFile> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(fileType)) {
            wrapper.eq(MediaFile::getFileType, fileType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(MediaFile::getOriginalName, keyword);
        }
        wrapper.orderByDesc(MediaFile::getCreatedAt);

        Page<MediaFile> result = mediaFileMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public MediaFile getFile(Long id) {
        MediaFile media = mediaFileMapper.selectById(id);
        if (media == null) {
            throw new BusinessException(404, "Media file not found");
        }
        return media;
    }

    @Override
    public void updateAltText(Long id, String altText) {
        MediaFile media = mediaFileMapper.selectById(id);
        if (media == null) {
            throw new BusinessException(404, "Media file not found");
        }
        media.setAltText(altText);
        mediaFileMapper.updateById(media);
    }

    @Override
    public void deleteFile(Long id) {
        MediaFile media = mediaFileMapper.selectById(id);
        if (media != null) {
            // Delete physical files
            deletePath(media.getFilePath());
            if (media.getThumbnailUrl() != null) {
                deletePath(media.getThumbnailUrl().replace(urlPrefix + "/", ""));
            }
            if (media.getWebpUrl() != null) {
                deletePath(media.getWebpUrl().replace(urlPrefix + "/", ""));
            }
            mediaFileMapper.deleteById(id);
        }
    }

    // ---- Private helpers ----

    private void generateThumbnail(BufferedImage original, Path output, String ext) throws IOException {
        int w = original.getWidth();
        int h = original.getHeight();
        double ratio = Math.min((double) THUMBNAIL_SIZE / w, (double) THUMBNAIL_SIZE / h);
        if (ratio >= 1.0) {
            // Original is already small enough — just copy
            ImageIO.write(original, getFormatName(ext), output.toFile());
            return;
        }
        int newW = (int) (w * ratio);
        int newH = (int) (h * ratio);

        BufferedImage thumb = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newW, newH, null);
        g.dispose();

        ImageIO.write(thumb, getFormatName(ext), output.toFile());
    }

    private void deletePath(String relativePath) {
        try {
            Path p = Paths.get(uploadPath, relativePath);
            Files.deleteIfExists(p);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", relativePath);
        }
    }

    private String getFileType(String mimeType) {
        if (mimeType == null) return "other";
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        if (mimeType.startsWith("audio/")) return "audio";
        if (mimeType.contains("pdf") || mimeType.contains("word") || mimeType.contains("zip")) return "document";
        return "other";
    }

    private String getFormatName(String ext) {
        if (ext == null) return "jpg";
        ext = ext.toLowerCase().replace(".", "");
        return switch (ext) {
            case "png" -> "png";
            case "gif" -> "gif";
            case "webp" -> "webp";
            case "bmp" -> "bmp";
            default -> "jpg";
        };
    }
}
