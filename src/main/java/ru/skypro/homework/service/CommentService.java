package ru.skypro.homework.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.skypro.homework.dto.Role;
import ru.skypro.homework.dto.comment.CommentDTO;
import ru.skypro.homework.dto.comment.CommentsDTO;
import ru.skypro.homework.dto.comment.CreateOrUpdateCommentDTO;
import ru.skypro.homework.entity.Ad;
import ru.skypro.homework.entity.Comment;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.repository.AdRepository;
import ru.skypro.homework.repository.CommentRepository;
import ru.skypro.homework.repository.UserRepository;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final AdRepository adRepository;
    private final UserRepository userRepository;

    /**
     * Получение всех комментариев для объявления
     *
     * @param adId идентификатор объявления
     * @return DTO с количеством и списком комментариев
     */
    public CommentsDTO getCommentsByAdId(Long adId) {
        log.debug("Getting comments for ad id: {}", adId);
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
     * Добавление нового комментария к объявлению
     *
     * @param adId     идентификатор объявления
     * @param dto      DTO с данными комментария
     * @param username имя пользователя-автора
     * @return DTO созданного комментария или null при ошибке
     */
    public CommentDTO addComment(Long adId, CreateOrUpdateCommentDTO dto, String username) {
        log.debug("Adding comment to ad {} by user {}", adId, username);
        Ad ad = adRepository.findById(adId).orElse(null);
        User user = userRepository.findByUsername(username).orElse(null);

        if (ad == null || user == null) {
            log.warn("Failed to add comment: ad {} or user {} not found", adId, username);
            return null;
        }

        Long nextCommentNumber = commentRepository.countByAdId(adId) + 1;

        Comment comment = new Comment();
        comment.setText(dto.getText());
        comment.setUser(user);
        comment.setAd(ad);
        comment.setCommentNumber(nextCommentNumber);

        Comment savedComment = commentRepository.save(comment);
        log.info("Comment {} added successfully to ad {}", savedComment.getCommentNumber(), adId);
        return convertToDto(savedComment);
    }

    /**
     * Удаление комментария с проверкой прав доступа
     *
     * @param adId идентификатор объявления
     * @param commentNumber номер комментария в рамках объявления
     * @param username имя пользователя, выполняющего операцию
     * @return true если удаление успешно, false если нет прав
     */
    public boolean deleteCommentWithPermission(Long adId, Long commentNumber, String username) {
        log.debug("Deleting comment {} from ad {} by user {}", commentNumber, adId, username);
        Comment comment = commentRepository.findByAdIdAndCommentNumber(adId, commentNumber).orElse(null);
        if (comment == null) {
            log.warn("Comment {} not found for deletion in ad {}", commentNumber, adId);
            return false;
        }

        if (!hasDeletePermission(comment, username)) {
            log.warn("User {} attempted to delete comment {} from ad {} without permission", username, commentNumber, adId);
            return false;
        }

        commentRepository.delete(comment);
        log.info("Comment {} from ad {} deleted successfully by user {}", commentNumber, adId, username);
        return true;
    }

    /**
     * Обновление комментария с проверкой прав доступа
     *
     * @param adId идентификатор объявления
     * @param commentNumber номер комментария в рамках объявления
     * @param dto DTO с обновленными данными
     * @param username имя пользователя, выполняющего операцию
     * @return DTO обновленного комментария или null если нет прав
     */
    public CommentDTO updateCommentWithPermission(Long adId, Long commentNumber, CreateOrUpdateCommentDTO dto, String username) {
        log.debug("Updating comment {} from ad {} by user {}", commentNumber, adId, username);
        Comment comment = commentRepository.findByAdIdAndCommentNumber(adId, commentNumber).orElse(null);
        if (comment == null) {
            log.warn("Comment {} not found for update in ad {}", commentNumber, adId);
            return null;
        }

        if (!hasUpdatePermission(comment, username)) {
            log.warn("User {} attempted to update comment {} from ad {} without permission", username, commentNumber, adId);
            return null;
        }

        comment.setText(dto.getText());
        Comment updatedComment = commentRepository.save(comment);
        log.info("Comment {} from ad {} updated successfully by user {}", commentNumber, adId, username);
        return convertToDto(updatedComment);
    }

    /**
     * Проверка прав доступа для редактирования комментария
     *
     * @param comment комментарий для проверки
     * @param username имя пользователя
     * @return true только если пользователь является автором комментария
     */
    private boolean hasUpdatePermission(Comment comment, String username) {
        return comment.getUser().getUsername().equals(username);
    }

    /**
     * Проверка прав доступа для удаления комментария
     *
     * @param comment комментарий для проверки
     * @param username имя пользователя
     * @return true если пользователь является админом, автором комментария или автором объявления
     */
    private boolean hasDeletePermission(Comment comment, String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;
        if (user.getRole() == Role.ADMIN) return true;

        boolean isCommentAuthor = comment.getUser().getUsername().equals(username);
        boolean isAdAuthor = comment.getAd().getUser().getUsername().equals(username);
        return isCommentAuthor || isAdAuthor;
    }

    /**
     * Преобразование сущности комментария в DTO
     *
     * @param comment сущность комментария
     * @return DTO комментария
     */
    private CommentDTO convertToDto(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getCommentNumber());
        dto.setText(comment.getText());
        dto.setAuthor(comment.getUser().getId());
        dto.setAuthorFirstName(comment.getUser().getFirstName());
        dto.setCreatedAt(comment.getCreatedAt().toEpochSecond(ZoneOffset.UTC) * 1000);

        String avatarUrl = "/users/" + comment.getUser().getId() + "/avatar";
        dto.setAuthorImage(avatarUrl);

        return dto;
    }
}
