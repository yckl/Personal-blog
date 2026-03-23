package com.blog.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.server.common.PageResult;
import com.blog.server.dto.request.CommentRequest;
import com.blog.server.dto.response.CommentVO;
import com.blog.server.entity.Article;
import com.blog.server.entity.Comment;
import com.blog.server.exception.BusinessException;
import com.blog.server.mapper.ArticleMapper;
import com.blog.server.mapper.CommentMapper;
import com.blog.server.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;

    // ============ Spam filter: sensitive words (multi-language) ============
    private static final List<String> SPAM_PATTERNS = List.of(
            // Chinese profanity / gaming toxicity
            "傻逼", "你妈", "操你", "去死", "垃圾", "废物", "白痴", "智障", "脑残",
            "滚蛋", "狗日", "贱人", "婊子", "混蛋", "王八蛋", "畜生", "死全家",
            "草泥马", "尼玛", "卧槽", "他妈的", "妈的", "艹", "cnm", "nmsl", "sb",
            // English profanity
            "fuck", "shit", "bitch", "asshole", "bastard", "damn", "crap",
            "dick", "pussy", "whore", "slut", "retard", "nigger", "faggot",
            // Spam patterns
            "casino", "lottery", "viagra", "porn", "xxx", "buy now", "click here",
            "make money", "earn money", "free money", "赌博", "彩票", "色情"
    );

    private static final Pattern SPAM_REGEX = Pattern.compile(
            SPAM_PATTERNS.stream()
                    .map(Pattern::quote)
                    .collect(Collectors.joining("|")),
            Pattern.CASE_INSENSITIVE
    );

    // Rate limiting: max 5 comments per IP per 10 minutes
    private static final int RATE_LIMIT = 5;
    private static final int RATE_WINDOW_MINUTES = 10;

    @Override
    @Transactional
    public Long createComment(CommentRequest request, String ipAddress, String userAgent) {
        Article article = articleMapper.selectById(request.getArticleId());
        if (article == null) {
            throw new BusinessException(404, "Article not found");
        }
        if (!article.getAllowComment()) {
            throw new BusinessException("Comments are disabled for this article");
        }

        // Spam check
        if (containsSpam(request.getContent()) || containsSpam(request.getAuthorName())) {
            throw new BusinessException("Your comment contains inappropriate content. Please revise.");
        }

        // Rate limiting
        if (isRateLimited(ipAddress)) {
            throw new BusinessException("You are commenting too frequently. Please wait a few minutes.");
        }

        Comment comment = new Comment();
        comment.setArticleId(request.getArticleId());
        comment.setParentId(request.getParentId());
        comment.setAuthorName(request.getAuthorName());
        comment.setAuthorEmail(request.getAuthorEmail());
        comment.setAuthorUrl(request.getAuthorUrl());
        comment.setContent(request.getContent());
        comment.setStatus("PENDING");
        comment.setIpAddress(ipAddress);
        comment.setUserAgent(userAgent);
        comment.setLikeCount(0);
        comment.setIsPinned(false);

        // Set root_id for threading
        if (request.getParentId() != null) {
            Comment parent = commentMapper.selectById(request.getParentId());
            if (parent != null) {
                comment.setRootId(parent.getRootId() != null ? parent.getRootId() : parent.getId());
            }
        }

        // Generate Gravatar URL from email
        if (StringUtils.hasText(request.getAuthorEmail())) {
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
                byte[] digest = md.digest(request.getAuthorEmail().trim().toLowerCase().getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) sb.append(String.format("%02x", b));
                comment.setAuthorAvatar("https://gravatar.com/avatar/" + sb + "?d=mp&s=80");
            } catch (Exception ignored) {
            }
        }

        commentMapper.insert(comment);

        // Update article comment count
        articleMapper.update(null,
                new LambdaUpdateWrapper<Article>()
                        .eq(Article::getId, request.getArticleId())
                        .setSql("comment_count = comment_count + 1")
        );

        return comment.getId();
    }

    @Override
    public List<CommentVO> getCommentsByArticleId(Long articleId) {
        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getArticleId, articleId)
                        .eq(Comment::getStatus, "APPROVED")
                        .orderByDesc(Comment::getIsPinned) // pinned first
                        .orderByAsc(Comment::getCreatedAt)
        );
        return buildCommentTree(comments);
    }

    @Override
    public PageResult<CommentVO> listAllComments(Integer page, Integer size, String status, Long articleId) {
        Page<Comment> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 20);

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Comment::getStatus, status);
        }
        if (articleId != null) {
            wrapper.eq(Comment::getArticleId, articleId);
        }
        wrapper.orderByDesc(Comment::getCreatedAt);

        Page<Comment> result = commentMapper.selectPage(pageParam, wrapper);

        List<CommentVO> records = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void approveComment(Long id) {
        commentMapper.update(null,
                new LambdaUpdateWrapper<Comment>()
                        .eq(Comment::getId, id)
                        .set(Comment::getStatus, "APPROVED")
        );
    }

    @Override
    public void rejectComment(Long id) {
        commentMapper.update(null,
                new LambdaUpdateWrapper<Comment>()
                        .eq(Comment::getId, id)
                        .set(Comment::getStatus, "REJECTED")
        );
    }

    @Override
    public void deleteComment(Long id) {
        commentMapper.deleteById(id);
    }

    /**
     * Like a comment (IP-based dedup handled by comment_like unique key at DB level).
     */
    public void likeComment(Long id) {
        commentMapper.update(null,
                new LambdaUpdateWrapper<Comment>()
                        .eq(Comment::getId, id)
                        .setSql("like_count = like_count + 1"));
    }

    /**
     * Toggle pin status.
     */
    public void togglePinComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment != null) {
            commentMapper.update(null,
                    new LambdaUpdateWrapper<Comment>()
                            .eq(Comment::getId, id)
                            .set(Comment::getIsPinned, !Boolean.TRUE.equals(comment.getIsPinned())));
        }
    }

    // ============ Spam Detection ============

    private boolean containsSpam(String text) {
        if (!StringUtils.hasText(text)) return false;
        return SPAM_REGEX.matcher(text).find();
    }

    private boolean isRateLimited(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) return false;
        Long count = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getIpAddress, ipAddress)
                        .ge(Comment::getCreatedAt, LocalDateTime.now().minusMinutes(RATE_WINDOW_MINUTES)));
        return count != null && count >= RATE_LIMIT;
    }

    // ============ Tree Building ============

    private List<CommentVO> buildCommentTree(List<Comment> comments) {
        Map<Long, CommentVO> voMap = comments.stream()
                .collect(Collectors.toMap(Comment::getId, this::convertToVO,
                        (existing, replacement) -> existing, LinkedHashMap::new));

        List<CommentVO> roots = new ArrayList<>();
        for (CommentVO vo : voMap.values()) {
            if (vo.getParentId() == null) {
                roots.add(vo);
            } else {
                CommentVO parent = voMap.get(vo.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(vo);
                } else {
                    // Orphan reply — add as root
                    roots.add(vo);
                }
            }
        }

        // Sort: pinned first, then by date
        roots.sort((a, b) -> {
            if (Boolean.TRUE.equals(a.getIsPinned()) && !Boolean.TRUE.equals(b.getIsPinned())) return -1;
            if (!Boolean.TRUE.equals(a.getIsPinned()) && Boolean.TRUE.equals(b.getIsPinned())) return 1;
            return 0;
        });

        return roots;
    }

    private CommentVO convertToVO(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setArticleId(comment.getArticleId());
        vo.setParentId(comment.getParentId());
        vo.setAuthorName(comment.getAuthorName());
        vo.setAuthorEmail(comment.getAuthorEmail());
        vo.setAuthorUrl(comment.getAuthorUrl());
        vo.setAuthorAvatar(comment.getAuthorAvatar());
        vo.setContent(comment.getContent());
        vo.setStatus(comment.getStatus());
        vo.setLikeCount(comment.getLikeCount());
        vo.setIsPinned(comment.getIsPinned());
        vo.setCreatedAt(comment.getCreatedAt());
        vo.setChildren(new ArrayList<>());
        return vo;
    }
}
