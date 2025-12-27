package com.scholar.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholar.platform.dto.forum.*;
import com.scholar.platform.entity.ForumBoard;
import com.scholar.platform.entity.ForumPost;
import com.scholar.platform.entity.ForumReply;
import com.scholar.platform.entity.User;
import com.scholar.platform.repository.ForumBoardRepository;
import com.scholar.platform.repository.ForumPostRepository;
import com.scholar.platform.repository.ForumReplyRepository;
import com.scholar.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForumService {

    private final ForumPostRepository postRepository;
    private final ForumReplyRepository replyRepository;
    private final ForumBoardRepository boardRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper; // 用于 JSON 转换

    /**
     * 获取帖子列表（带统计数据）
     */
    @Transactional(readOnly = true)
    public List<PostListItemDTO> getPosts(String boardId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // 使用自定义查询获取 [Post, replyCount, lastReplyTime]
        Page<Object[]> results = postRepository.findPostsWithStats(boardId, pageable);

        return results.stream().map(row -> {
            ForumPost post = (ForumPost) row[0];
            Long replyCount = (Long) row[1];
            LocalDateTime lastReplyTime = (LocalDateTime) row[2];

            PostListItemDTO dto = new PostListItemDTO();
            dto.setPostId(post.getId());
            dto.setTitle(post.getTitle());
            // 截取内容预览
            dto.setContentPreview(post.getContent().length() > 100 ? post.getContent().substring(0, 100) + "..." : post.getContent());
            dto.setBoardName(post.getBoard().getName());
            dto.setViewCount(post.getViewCount());
            dto.setCreatedAt(post.getCreatedAt());
            
            // 填充统计数据
            dto.setReplyCount(replyCount == null ? 0 : replyCount);
            dto.setLastReplyTime(lastReplyTime);

            // 填充作者信息
            dto.setAuthor(toUserSummary(post.getAuthor()));
            
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 发布帖子
     */
    @Transactional
    public ForumPost createPost(String email, CreatePostRequest request) {
        User author = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        ForumBoard board = boardRepository.findById(request.getBoardId())
                .orElseGet(() -> {
                    log.info("板块不存在，自动创建: {}", request.getBoardId());
                    ForumBoard newBoard = new ForumBoard();
                    newBoard.setId(request.getBoardId());
                    // 根据ID获取预设的名称，如果未知则默认使用ID
                    newBoard.setName(getPredefinedBoardName(request.getBoardId()));
                    newBoard.setType(0); // 新增：给type字段赋默认值
                    newBoard.setCreatedAt(LocalDateTime.now()); // 如果有createdAt字段
                    newBoard.setDescription(""); // 如果有description字段
                    return boardRepository.save(newBoard);
                });
                //.orElseThrow(() -> new RuntimeException("板块不存在"));

        ForumPost post = new ForumPost();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setBoard(board);
        post.setAuthor(author);
        post.setAttachments(toJson(request.getAttachments()));
        
        return postRepository.save(post);
    }

    private String getPredefinedBoardName(String boardId) {
        switch (boardId) {
            case "1": return "学术交流";
            case "2": return "资源共享";
            case "3": return "校园生活";
            case "4": return "求职招聘";
            default: return "未知板块-" + boardId;
        }
    }

    /**
     * 获取帖子详情
     */
    @Transactional
    public PostDetailDTO getPostDetail(String postId) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));

        // 增加浏览量
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);

        // 获取回复列表
        List<ForumReply> replies = replyRepository.findByPostIdWithAuthor(postId);

        PostDetailDTO dto = new PostDetailDTO();
        dto.setPostId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setBoardId(post.getBoard().getId());
        dto.setBoardName(post.getBoard().getName());
        dto.setAuthor(toUserSummary(post.getAuthor()));
        dto.setViewCount(post.getViewCount());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setAttachments(fromJson(post.getAttachments()));

        // 转换回复列表
        List<ReplyDTO> replyDTOs = replies.stream().map(reply -> {
            ReplyDTO rDto = new ReplyDTO();
            rDto.setId(reply.getId());
            rDto.setContent(reply.getContent());
            rDto.setCreatedAt(reply.getCreatedAt());
            rDto.setAuthor(toUserSummary(reply.getAuthor()));
            rDto.setAttachments(fromJson(reply.getAttachments()));
            return rDto;
        }).collect(Collectors.toList());

        dto.setReplies(replyDTOs);
        return dto;
    }

    /**
     * 回复帖子
     */
    @Transactional
    public ForumReply createReply(String email, String postId, CreateReplyRequest request) {
        User author = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));

        ForumReply reply = new ForumReply();
        reply.setContent(request.getContent());
        reply.setPost(post);
        reply.setAuthor(author);
        reply.setAttachments(toJson(request.getAttachments()));

        // 更新帖子最后更新时间（可选）
        // post.setUpdatedAt(LocalDateTime.now());
        // postRepository.save(post);

        return replyRepository.save(reply);
    }

    private UserSummaryDTO toUserSummary(User user) {
        if (user == null) return null;
        // 假设 User 实体有 getAvatarUrl 方法，如果没有请自行调整
        // return new UserSummaryDTO(user.getId(), user.getUsername(), user.getAvatarUrl());
        return new UserSummaryDTO(user.getId(), user.getUsername(), null); 
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("JSON转换失败", e);
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("JSON解析失败", e);
            return Collections.emptyList();
        }
    }
}