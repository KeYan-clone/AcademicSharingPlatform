package com.scholar.platform.service;

import com.scholar.platform.dto.PostCreateRequest;
import com.scholar.platform.dto.PostDTO;
import com.scholar.platform.dto.ReplyCreateRequest;
import com.scholar.platform.entity.ForumBoard;
import com.scholar.platform.entity.ForumPost;
import com.scholar.platform.entity.ForumReply;
import com.scholar.platform.entity.User;
import com.scholar.platform.repository.ForumBoardRepository;
import com.scholar.platform.repository.ForumPostRepository;
import com.scholar.platform.repository.ForumReplyRepository;
import com.scholar.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ForumService {

  private final ForumBoardRepository boardRepository;
  private final ForumPostRepository postRepository;
  private final ForumReplyRepository replyRepository;
  private final UserRepository userRepository;

  @Transactional
  public ForumPost createPost(String authorId, PostCreateRequest request) {
    User author = userRepository.findById(authorId)
        .orElseThrow(() -> new RuntimeException("用户不存在"));
    ForumBoard board = boardRepository.findById(request.getBoardId())
        .orElseThrow(() -> new RuntimeException("板块不存在"));

    ForumPost post = new ForumPost();
    post.setBoard(board);
    post.setAuthor(author);
    post.setTitle(request.getTitle());
    post.setContent(request.getContent());
    post.setAttachments(request.getAttachments());

    return postRepository.save(post);
  }

  public Page<PostDTO> getPostsByBoard(String boardId, Pageable pageable) {
    return postRepository.findByBoardId(boardId, pageable)
        .map(this::toDTO);
  }

  public PostDTO getPostById(String postId) {
    ForumPost post = postRepository.findById(postId)
        .orElseThrow(() -> new RuntimeException("帖子不存在"));
    return toDTO(post);
  }

  @Transactional
  public ForumReply createReply(String authorId, ReplyCreateRequest request) {
    User author = userRepository.findById(authorId)
        .orElseThrow(() -> new RuntimeException("用户不存在"));
    ForumPost post = postRepository.findById(request.getPostId())
        .orElseThrow(() -> new RuntimeException("帖子不存在"));

    ForumReply reply = new ForumReply();
    reply.setPost(post);
    reply.setAuthor(author);
    reply.setContent(request.getContent());

    return replyRepository.save(reply);
  }

  public Page<ForumReply> getRepliesByPost(String postId, Pageable pageable) {
    return replyRepository.findByPostId(postId, pageable);
  }

  private PostDTO toDTO(ForumPost post) {
    PostDTO dto = new PostDTO();
    dto.setId(post.getId());
    dto.setBoardId(post.getBoard().getId());
    dto.setBoardName(post.getBoard().getName());
    dto.setAuthorId(post.getAuthor().getId());
    dto.setAuthorUsername(post.getAuthor().getUsername());
    dto.setTitle(post.getTitle());
    dto.setContent(post.getContent());
    dto.setAttachments(post.getAttachments());
    dto.setCreatedAt(post.getCreatedAt());
    return dto;
  }
}
