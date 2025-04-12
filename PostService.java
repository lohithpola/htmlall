package com.socialmedia.service;

import com.socialmedia.entity.Post;
import com.socialmedia.entity.User;
import com.socialmedia.repository.PostRepository;
import com.socialmedia.repository.UserRepository;
import com.socialmedia.controller.WebSocketController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PostService {
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private WebSocketController webSocketController;

    @Transactional
    public Post createPost(Long userId, String caption, MultipartFile mediaFile) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post post = new Post();
        post.setCaption(caption);
        post.setUser(user);
        
        if (mediaFile != null && !mediaFile.isEmpty()) {
            post.setMediaContent(mediaFile.getBytes());
            post.setMediaType(mediaFile.getContentType().startsWith("image") ? "IMAGE" : "VIDEO");
        }

        post = postRepository.save(post);
        webSocketController.notifyFeedUpdate(post);
        return post;
    }

    public Page<Post> getFeedPosts(Long userId, Pageable pageable) {
        return postRepository.findFeedPosts(userId, pageable);
    }

    public Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    @Transactional
    public void likePost(Long userId, Long postId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Post post = getPost(postId);

        if (!post.getLikedBy().contains(user)) {
            post.getLikedBy().add(user);
            post = postRepository.save(post);
            notificationService.createLikeNotification(user, post);
            webSocketController.notifyFeedUpdate(post);
        }
    }

    @Transactional
    public void unlikePost(Long userId, Long postId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Post post = getPost(postId);

        post.getLikedBy().remove(user);
        post = postRepository.save(post);
        webSocketController.notifyFeedUpdate(post);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = getPost(postId);
        if (!post.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this post");
        }
        postRepository.delete(post);
        webSocketController.notifyFeedUpdate(post);
    }
} 