package ru.skypro.homework.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.skypro.homework.controller.AdController;
import ru.skypro.homework.dto.Role;
import ru.skypro.homework.dto.comment.CommentDTO;
import ru.skypro.homework.dto.comment.CommentsDTO;
import ru.skypro.homework.dto.comment.CreateOrUpdateCommentDTO;
import ru.skypro.homework.entity.Ad;
import ru.skypro.homework.entity.Comment;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.repository.CommentRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final AdService adService;
    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(AdController.class);

    /**
     * Получение комментариев объявления
     */
    public CommentsDTO getCommentsByAdId(Long adId) {
        logger.debug("Getting comments for ad id: {}", adId);
        List<Comment> comments = commentRepository.findByAdIdOrderByCreatedAtDesc(adId);
        List<CommentDTO> commentDTOS = comments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        CommentsDTO response = new CommentsDTO();
        response.setCount(commentDTOS.size());
        response.setResults(commentDTOS);
        return response;
    }

    /**
     * Добавление комментария
     */
    public CommentDTO addComment(Long adId, CreateOrUpdateCommentDTO dto, String username) {
        logger.debug("Adding comment to ad {} by user {}", adId, username);
        Ad ad = adService.getAdEntityById(adId);
        User user = userService.findUser(username);

        if (ad == null || user == null) {
            logger.warn("Failed to add comment: ad {} or user {} not found", adId, username);
            return null;
        }

        Comment comment = new Comment();
        comment.setText(dto.getText());
        comment.setUser(user);
        comment.setAd(ad);

        Comment savedComment = commentRepository.save(comment);
        logger.info("Comment {} added successfully to ad {}", savedComment.getId(), adId);
        return convertToDto(savedComment);
    }

    /**
     * Удаление комментария с проверкой прав
     */
    public boolean deleteComment(Long commentId, String username) {
        logger.debug("Deleting comment {} by user {}", commentId, username);
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            logger.warn("Comment {} not found for deletion", commentId);
            return false;
        }

        commentRepository.deleteById(commentId);
        logger.info("Comment {} deleted successfully by user {}", commentId, username);
        return true;
    }

    /**
     * Обновление комментария с проверкой прав
     */
    public CommentDTO updateComment(Long commentId, CreateOrUpdateCommentDTO dto, String username) {
        logger.debug("Updating comment {} by user {}", commentId, username);
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            logger.warn("Comment {} not found for update", commentId);
            return null;
        }

        comment.setText(dto.getText());
        Comment updatedComment = commentRepository.save(comment);
        logger.info("Comment {} updated successfully by user {}", commentId, username);
        return convertToDto(updatedComment);
    }

    /**
     * Проверка прав доступа к комментарию
     * @return true если пользователь является автором комментария или автором объявления
     */
    public boolean hasCommentPermission(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            return false; // или true, в зависимости от логики
        }

        User user = userService.findUser(username);
        if (user == null) {
            return false;
        }

        // Админ имеет полный доступ
        if (user.getRole() == Role.ADMIN) {
            return true;
        }

        // Автор комментария или автор объявления могут управлять комментарием
        boolean isCommentAuthor = comment.getUser().getUsername().equals(username);
        boolean isAdAuthor = comment.getAd().getUser().getUsername().equals(username);

        return isCommentAuthor || isAdAuthor;
    }

    /**
     * Проверка прав доступа для РЕДАКТИРОВАНИЯ комментария
     * @return true только если пользователь является автором комментария
     */
    public boolean hasUpdatePermission(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            return false;
        }

        // Только автор комментария может редактировать свой комментарий
        // Админ НЕ может редактировать чужие комментарии
        boolean isCommentAuthor = comment.getUser().getUsername().equals(username);

        logger.debug("User {} update permission for comment {}: isCommentAuthor={}",
                username, commentId, isCommentAuthor);

        return isCommentAuthor;
    }

    /**
     * Проверка прав доступа для УДАЛЕНИЯ комментария
     * @return true если пользователь является админом, автором комментария или автором объявления
     */
    public boolean hasDeletePermission(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            return false;
        }

        User user = userService.findUser(username);
        if (user == null) {
            return false;
        }

        // Админ может удалять любые комментарии
        if (user.getRole() == Role.ADMIN) {
            logger.debug("User {} is ADMIN, has delete permission for comment {}", username, commentId);
            return true;
        }

        // Автор комментария может удалять свои комментарии
        boolean isCommentAuthor = comment.getUser().getUsername().equals(username);
        // Автор объявления может удалять комментарии под своим объявлением
        boolean isAdAuthor = comment.getAd().getUser().getUsername().equals(username);

        boolean hasPermission = isCommentAuthor || isAdAuthor;

        if (hasPermission) {
            logger.debug("User {} has delete permission for comment {}: isCommentAuthor={}, isAdAuthor={}",
                    username, commentId, isCommentAuthor, isAdAuthor);
        } else {
            logger.debug("User {} has NO delete permission for comment {}: isCommentAuthor={}, isAdAuthor={}",
                    username, commentId, isCommentAuthor, isAdAuthor);
        }

        return hasPermission;
    }

    /**
     * Преобразование сущности в DTO
     */
    private CommentDTO convertToDto(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setText(comment.getText());
        dto.setAuthor(comment.getUser().getId());
        dto.setAuthorFirstName(comment.getUser().getFirstName());
        dto.setCreatedAt(comment.getCreatedAt());

        // Формируем URL для аватара автора
        String avatarUrl = "/users/" + comment.getUser().getId() + "/avatar";
        dto.setAuthorImage(avatarUrl);

        return dto;
    }
}
