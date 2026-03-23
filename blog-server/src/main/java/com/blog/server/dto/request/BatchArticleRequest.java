package com.blog.server.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchArticleRequest {

    @NotEmpty(message = "Article IDs are required")
    private List<Long> ids;
}
