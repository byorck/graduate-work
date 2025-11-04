package ru.skypro.homework.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.skypro.homework.dto.Role;
import ru.skypro.homework.dto.comment.CommentDTO;
import ru.skypro.homework.dto.comment.CommentsDTO;
import ru.skypro.homework.dto.comment.CreateOrUpdateCommentDTO;
import ru.skypro.homework.entity.Ad;
import ru.skypro.homework.entity.Comment;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.repository.CommentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для CommentService.
 * Проверяет бизнес-логику работы с комментариями
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private AdService adService;

    @Mock
    private UserService userService;

    @InjectMocks
    private CommentService commentService;

    // Вспомогательные методы для создания тестовых данных
    private User createTestUser(String username, Role role) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPhone("+123456789");
        user.setRole(role);
        return user;
    }

    private Ad createTestAd(User author) {
        Ad ad = new Ad();
        ad.setId(1L);
        ad.setTitle("Test Ad");
        ad.setDescription("Test Description");
        ad.setPrice(100);
        ad.setUser(author);
        return ad;
    }

    private Comment createTestComment(User author, Ad ad, String text) {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setText(text);
        comment.setUser(author);
        comment.setAd(ad);
        comment.setCreatedAt(LocalDateTime.now());
        return comment;
    }

    @Nested
    @DisplayName("Тесты получения комментариев")
    class GetCommentsTests {

        @Test
        @DisplayName("Получение комментариев по ID объявления - когда комментарии существуют")
        void getCommentsByAdId_WhenCommentsExist_ReturnsComments() {
            // Given
            Long adId = 1L;
            User user = createTestUser("test@example.com", Role.USER);
            Ad ad = createTestAd(user);
            Comment comment = createTestComment(user, ad, "Test comment");

            when(commentRepository.findByAdIdOrderByCreatedAtDesc(adId)).thenReturn(List.of(comment));

            // When
            CommentsDTO result = commentService.getCommentsByAdId(adId);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getCount());
            assertEquals(1, result.getResults().size());
            assertEquals("Test comment", result.getResults().get(0).getText());
        }
    }

    @Nested
    @DisplayName("Тесты создания комментариев")
    class CreateCommentsTests {

        @Test
        @DisplayName("Добавление комментария - когда объявление и пользователь существуют")
        void addComment_WhenAdAndUserExist_CreatesComment() {
            // Given
            Long adId = 1L;
            String username = "test@example.com";

            CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
            dto.setText("New comment");

            User user = createTestUser(username, Role.USER);
            Ad ad = createTestAd(user);

            when(adService.getAdEntityById(adId)).thenReturn(ad);
            when(userService.findUser(username)).thenReturn(user);
            when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
                Comment savedComment = invocation.getArgument(0);
                savedComment.setId(1L);
                savedComment.setCreatedAt(LocalDateTime.now());
                return savedComment;
            });

            // When
            CommentDTO result = commentService.addComment(adId, dto, username);

            // Then
            assertNotNull(result);
            assertEquals("New comment", result.getText());
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("Добавление комментария - когда объявление не найдено")
        void addComment_WhenAdNotFound_ReturnsNull() {
            // Given
            Long adId = 1L;
            String username = "test@example.com";

            CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
            dto.setText("New comment");

            when(adService.getAdEntityById(adId)).thenReturn(null);
            when(userService.findUser(username)).thenReturn(createTestUser(username, Role.USER));

            // When
            CommentDTO result = commentService.addComment(adId, dto, username);

            // Then
            assertNull(result);
            verify(commentRepository, never()).save(any(Comment.class));
        }
    }

    @Nested
    @DisplayName("Тесты удаления комментариев")
    class DeleteCommentsTests {

        @Test
        @DisplayName("Удаление комментария с проверкой прав - когда пользователь администратор")
        void deleteCommentWithPermission_WhenUserIsAdmin_DeletesComment() {
            // Given
            Long commentId = 1L;
            String username = "admin@example.com";

            User admin = createTestUser(username, Role.ADMIN);
            User commentAuthor = createTestUser("author@example.com", Role.USER);
            Ad ad = createTestAd(createTestUser("adowner@example.com", Role.USER));
            Comment comment = createTestComment(commentAuthor, ad, "Test comment");

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(userService.findUser(username)).thenReturn(admin);

            // When
            boolean result = commentService.deleteCommentWithPermission(commentId, username);

            // Then
            assertTrue(result);
            verify(commentRepository).deleteById(commentId);
        }

        @Test
        @DisplayName("Удаление комментария с проверкой прав - когда пользователь автор комментария")
        void deleteCommentWithPermission_WhenUserIsCommentAuthor_DeletesComment() {
            // Given
            Long commentId = 1L;
            String username = "author@example.com";

            User author = createTestUser(username, Role.USER);
            Ad ad = createTestAd(createTestUser("adowner@example.com", Role.USER));
            Comment comment = createTestComment(author, ad, "Test comment");

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(userService.findUser(username)).thenReturn(author);

            // When
            boolean result = commentService.deleteCommentWithPermission(commentId, username);

            // Then
            assertTrue(result);
            verify(commentRepository).deleteById(commentId);
        }

        @Test
        @DisplayName("Удаление комментария - когда комментарий существует")
        void deleteComment_WhenCommentExists_DeletesComment() {
            // Given
            Long commentId = 1L;
            String username = "user@example.com";

            User author = createTestUser("author@example.com", Role.USER);
            Ad ad = createTestAd(createTestUser("adowner@example.com", Role.USER));
            Comment comment = createTestComment(author, ad, "Test comment");

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            // When
            boolean result = commentService.deleteComment(commentId, username);

            // Then
            assertTrue(result);
            verify(commentRepository).deleteById(commentId);
        }

        @Test
        @DisplayName("Удаление комментария - когда комментарий не существует")
        void deleteComment_WhenCommentNotExists_ReturnsFalse() {
            // Given
            Long commentId = 1L;
            String username = "user@example.com";

            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            // When
            boolean result = commentService.deleteComment(commentId, username);

            // Then
            assertFalse(result);
            verify(commentRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Тесты обновления комментариев")
    class UpdateCommentsTests {

        @Test
        @DisplayName("Обновление комментария с проверкой прав - когда пользователь автор комментария")
        void updateCommentWithPermission_WhenUserIsCommentAuthor_UpdatesComment() {
            // Given
            Long commentId = 1L;
            String username = "author@example.com";

            CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
            dto.setText("Updated comment");

            User author = createTestUser(username, Role.USER);
            Ad ad = createTestAd(createTestUser("adowner@example.com", Role.USER));
            Comment comment = createTestComment(author, ad, "Original comment");

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.<Comment>getArgument(0));

            // When
            CommentDTO result = commentService.updateCommentWithPermission(commentId, dto, username);

            // Then
            assertNotNull(result);
            assertEquals("Updated comment", result.getText());
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("Обновление комментария с проверкой прав - когда пользователь не имеет прав")
        void updateCommentWithPermission_WhenUserNoPermission_ReturnsNull() {
            // Given
            Long commentId = 1L;
            String username = "other@example.com";

            CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
            dto.setText("Updated comment");

            User author = createTestUser("author@example.com", Role.USER);
            Ad ad = createTestAd(createTestUser("adowner@example.com", Role.USER));
            Comment comment = createTestComment(author, ad, "Original comment");

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            // When
            CommentDTO result = commentService.updateCommentWithPermission(commentId, dto, username);

            // Then
            assertNull(result);
            verify(commentRepository, never()).save(any(Comment.class));
        }
    }

    @Nested
    @DisplayName("Тесты проверки прав доступа")
    class PermissionTests {

        @Test
        @DisplayName("Проверка прав на обновление - когда пользователь автор комментария")
        void hasUpdatePermission_WhenUserIsCommentAuthor_ReturnsTrue() {
            // Given
            Long commentId = 1L;
            String username = "author@example.com";

            User author = createTestUser(username, Role.USER);
            Ad ad = createTestAd(createTestUser("adowner@example.com", Role.USER));
            Comment comment = createTestComment(author, ad, "Test comment");

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            // When
            boolean result = commentService.hasUpdatePermission(commentId, username);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Проверка прав на удаление - когда пользователь администратор")
        void hasDeletePermission_WhenUserIsAdmin_ReturnsTrue() {
            // Given
            Long commentId = 1L;
            String username = "admin@example.com";

            User admin = createTestUser(username, Role.ADMIN);
            User commentAuthor = createTestUser("author@example.com", Role.USER);
            Ad ad = createTestAd(createTestUser("adowner@example.com", Role.USER));
            Comment comment = createTestComment(commentAuthor, ad, "Test comment");

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(userService.findUser(username)).thenReturn(admin);

            // When
            boolean result = commentService.hasDeletePermission(commentId, username);

            // Then
            assertTrue(result);
        }
    }
}