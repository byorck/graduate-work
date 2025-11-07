package ru.skypro.homework.service;

import org.junit.jupiter.api.Test;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    private final Long AD_ID = 1L;
    private final Long COMMENT_ID = 1L;
    private final String USERNAME = "user@example.com";
    private final String TEXT = "Test comment";

    @Test
    void getCommentsByAdId_ShouldReturnComments() {
        // Given
        Comment comment = createTestComment();
        when(commentRepository.findByAdIdOrderByCreatedAtDesc(AD_ID))
                .thenReturn(List.of(comment));

        // When
        CommentsDTO result = commentService.getCommentsByAdId(AD_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getCount());
        assertEquals(1, result.getResults().size());

        CommentDTO commentDTO = result.getResults().get(0);
        assertEquals(COMMENT_ID, commentDTO.getId());
        assertEquals(TEXT, commentDTO.getText());

        verify(commentRepository).findByAdIdOrderByCreatedAtDesc(AD_ID);
    }

    @Test
    void getCommentsByAdId_WhenNoComments_ShouldReturnEmptyList() {
        // Given
        when(commentRepository.findByAdIdOrderByCreatedAtDesc(AD_ID))
                .thenReturn(List.of());

        // When
        CommentsDTO result = commentService.getCommentsByAdId(AD_ID);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCount());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    void addComment_ShouldSuccessfullyAddComment() {
        // Given
        CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
        dto.setText(TEXT);

        Ad ad = createTestAd();
        User user = createTestUser();

        when(adService.getAdEntityById(AD_ID)).thenReturn(ad);
        when(userService.findUser(USERNAME)).thenReturn(user);
        when(commentRepository.countByAdId(AD_ID)).thenReturn(0L);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment savedComment = invocation.getArgument(0);
            savedComment.setId(1L);
            return savedComment;
        });

        // When
        CommentDTO result = commentService.addComment(AD_ID, dto, USERNAME);

        // Then
        assertNotNull(result);
        assertEquals(TEXT, result.getText());
        assertEquals(1L, result.getId());

        verify(commentRepository).save(any(Comment.class));
        verify(commentRepository).countByAdId(AD_ID);
    }

    @Test
    void addComment_WhenAdNotFound_ShouldReturnNull() {
        // Given
        CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
        dto.setText(TEXT);

        when(adService.getAdEntityById(AD_ID)).thenReturn(null);

        // When
        CommentDTO result = commentService.addComment(AD_ID, dto, USERNAME);

        // Then
        assertNull(result);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void addComment_WhenUserNotFound_ShouldReturnNull() {
        // Given
        CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
        dto.setText(TEXT);

        Ad ad = createTestAd();

        when(adService.getAdEntityById(AD_ID)).thenReturn(ad);
        when(userService.findUser(USERNAME)).thenReturn(null);

        // When
        CommentDTO result = commentService.addComment(AD_ID, dto, USERNAME);

        // Then
        assertNull(result);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void deleteCommentWithPermission_AsCommentAuthor_ShouldSuccess() {
        // Given
        Comment comment = createTestComment();
        when(commentRepository.findByAdIdAndCommentNumber(AD_ID, COMMENT_ID))
                .thenReturn(Optional.of(comment));
        when(userService.findUser(USERNAME)).thenReturn(comment.getUser());

        // When
        boolean result = commentService.deleteCommentWithPermission(AD_ID, COMMENT_ID, USERNAME);

        // Then
        assertTrue(result);
        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteCommentWithPermission_AsAdmin_ShouldSuccess() {
        // Given
        Comment comment = createTestComment();
        User adminUser = createTestUser();
        adminUser.setRole(Role.ADMIN);
        adminUser.setUsername("admin@example.com");

        when(commentRepository.findByAdIdAndCommentNumber(AD_ID, COMMENT_ID))
                .thenReturn(Optional.of(comment));
        when(userService.findUser("admin@example.com")).thenReturn(adminUser);

        // When
        boolean result = commentService.deleteCommentWithPermission(AD_ID, COMMENT_ID, "admin@example.com");

        // Then
        assertTrue(result);
        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteCommentWithPermission_AsAdAuthor_ShouldSuccess() {
        // Given
        Comment comment = createTestComment();
        User adAuthor = createTestUser();
        adAuthor.setUsername("adauthor@example.com");
        comment.getAd().getUser().setUsername("adauthor@example.com");
        comment.getUser().setUsername("commentauthor@example.com");

        when(commentRepository.findByAdIdAndCommentNumber(AD_ID, COMMENT_ID))
                .thenReturn(Optional.of(comment));
        when(userService.findUser("adauthor@example.com")).thenReturn(adAuthor);

        // When
        boolean result = commentService.deleteCommentWithPermission(AD_ID, COMMENT_ID, "adauthor@example.com");

        // Then
        assertTrue(result);
        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteCommentWithPermission_WithoutPermission_ShouldFail() {
        // Given
        Comment comment = createTestComment();
        User unauthorizedUser = createTestUser();
        unauthorizedUser.setUsername("unauthorized@example.com");
        unauthorizedUser.setRole(Role.USER);
        comment.getUser().setUsername("different@example.com");
        comment.getAd().getUser().setUsername("different@example.com");

        when(commentRepository.findByAdIdAndCommentNumber(AD_ID, COMMENT_ID))
                .thenReturn(Optional.of(comment));
        when(userService.findUser("unauthorized@example.com")).thenReturn(unauthorizedUser);

        // When
        boolean result = commentService.deleteCommentWithPermission(AD_ID, COMMENT_ID, "unauthorized@example.com");

        // Then
        assertFalse(result);
        verify(commentRepository, never()).delete(comment);
    }

    @Test
    void deleteCommentWithPermission_WhenCommentNotFound_ShouldFail() {
        // Given
        when(commentRepository.findByAdIdAndCommentNumber(AD_ID, COMMENT_ID))
                .thenReturn(Optional.empty());

        // When
        boolean result = commentService.deleteCommentWithPermission(AD_ID, COMMENT_ID, USERNAME);

        // Then
        assertFalse(result);
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void updateCommentWithPermission_AsCommentAuthor_ShouldSuccess() {
        // Given
        Comment comment = createTestComment();
        CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
        dto.setText("Updated text");

        when(commentRepository.findByAdIdAndCommentNumber(AD_ID, COMMENT_ID))
                .thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        CommentDTO result = commentService.updateCommentWithPermission(AD_ID, COMMENT_ID, dto, USERNAME);

        // Then
        assertNotNull(result);
        assertEquals("Updated text", result.getText());
        verify(commentRepository).save(comment);
    }

    @Test
    void updateCommentWithPermission_WithoutPermission_ShouldReturnNull() {
        // Given
        Comment comment = createTestComment();
        comment.getUser().setUsername("different@example.com");
        CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
        dto.setText("Updated text");

        when(commentRepository.findByAdIdAndCommentNumber(AD_ID, COMMENT_ID))
                .thenReturn(Optional.of(comment));

        // When
        CommentDTO result = commentService.updateCommentWithPermission(AD_ID, COMMENT_ID, dto, USERNAME);

        // Then
        assertNull(result);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateCommentWithPermission_WhenCommentNotFound_ShouldReturnNull() {
        // Given
        CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
        dto.setText("Updated text");

        when(commentRepository.findByAdIdAndCommentNumber(AD_ID, COMMENT_ID))
                .thenReturn(Optional.empty());

        // When
        CommentDTO result = commentService.updateCommentWithPermission(AD_ID, COMMENT_ID, dto, USERNAME);

        // Then
        assertNull(result);
        verify(commentRepository, never()).save(any());
    }

    private Comment createTestComment() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setText(TEXT);
        comment.setCommentNumber(COMMENT_ID);
        comment.setCreatedAt(LocalDateTime.now());

        User user = createTestUser();
        comment.setUser(user);

        Ad ad = createTestAd();
        comment.setAd(ad);

        return comment;
    }

    private Ad createTestAd() {
        Ad ad = new Ad();
        ad.setId(AD_ID);

        User adUser = createTestUser();
        adUser.setUsername("adowner@example.com");
        ad.setUser(adUser);

        return ad;
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername(USERNAME);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole(Role.USER);
        return user;
    }
}