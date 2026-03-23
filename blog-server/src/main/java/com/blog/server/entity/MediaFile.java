package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("media_file")
public class MediaFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String originalName;
    private String storedName;
    private String filePath;
    private String fileUrl;
    private String thumbnailUrl;
    private String webpUrl;
    private Integer width;
    private Integer height;
    private String altText;
    private Long fileSize;
    private String fileType;   // image, video, audio, document, other
    private String mimeType;
    private Long uploadedBy;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
