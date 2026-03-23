package com.blog.server.service;

import java.util.List;
import com.blog.server.entity.ArticleVersion;

public interface VersionService {

    List<ArticleVersion> listVersions(Long articleId);

    ArticleVersion getVersion(Long versionId);

    void rollbackToVersion(Long articleId, Long versionId);
}
