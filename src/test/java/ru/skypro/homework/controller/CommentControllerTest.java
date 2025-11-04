package ru.skypro.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.skypro.homework.dto.comment.CommentDTO;
import ru.skypro.homework.dto.comment.CommentsDTO;
import ru.skypro.homework.dto.comment.CreateOrUpdateCommentDTO;
import ru.skypro.homework.service.CommentService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тестовый класс для CommentController.
 * Проверяет endpoints для работы с комментариями
 */
@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Тесты получения комментариев")
    class GetCommentsTests {

        @Test
        @WithMockUser
        @DisplayName("Получение комментариев - должен вернуть список комментариев")
        void getComments_ShouldReturnComments() throws Exception {
            // Given
            CommentsDTO commentsDTO = new CommentsDTO();
            commentsDTO.setCount(1);
            commentsDTO.setResults(List.of(new CommentDTO()));

            when(commentService.getCommentsByAdId(1L)).thenReturn(commentsDTO);

            // When & Then
            mockMvc.perform(get("/ads/1/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(1));
        }
    }

    @Nested
    @DisplayName("Тесты управления комментариями")
    class ManageCommentsTests {

        @Test
        @WithMockUser
        @DisplayName("Добавление комментария - когда объявление существует")
        void addComment_WhenAdExists_ShouldReturnComment() throws Exception {
            // Given
            CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
            dto.setText("New comment");

            CommentDTO commentDTO = new CommentDTO();
            commentDTO.setId(1L);
            commentDTO.setText("New comment");

            when(commentService.addComment(eq(1L), any(CreateOrUpdateCommentDTO.class), anyString()))
                    .thenReturn(commentDTO);

            // When & Then
            mockMvc.perform(post("/ads/1/comments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.text").value("New comment"));
        }

        @Test
        @WithMockUser
        @DisplayName("Добавление комментария - когда объявление не существует")
        void addComment_WhenAdNotExists_ShouldReturnNotFound() throws Exception {
            // Given
            CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
            dto.setText("New comment");

            when(commentService.addComment(eq(1L), any(CreateOrUpdateCommentDTO.class), anyString()))
                    .thenReturn(null);

            // When & Then
            mockMvc.perform(post("/ads/1/comments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("Удаление комментария - когда пользователь имеет права")
        void deleteComment_WhenUserHasPermission_ShouldReturnNoContent() throws Exception {
            // Given
            when(commentService.deleteCommentWithPermission(1L, "user"))
                    .thenReturn(true);

            // When & Then
            mockMvc.perform(delete("/ads/1/comments/1").with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser
        @DisplayName("Удаление комментария - когда пользователь не имеет прав")
        void deleteComment_WhenUserNoPermission_ShouldReturnForbidden() throws Exception {
            // Given
            when(commentService.deleteCommentWithPermission(1L, "user"))
                    .thenReturn(false);

            // When & Then
            mockMvc.perform(delete("/ads/1/comments/1").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        @DisplayName("Обновление комментария - когда пользователь имеет права")
        void updateComment_WhenUserHasPermission_ShouldReturnUpdatedComment() throws Exception {
            // Given
            CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
            dto.setText("Updated comment");

            CommentDTO updatedComment = new CommentDTO();
            updatedComment.setId(1L);
            updatedComment.setText("Updated comment");

            when(commentService.updateCommentWithPermission(eq(1L), any(CreateOrUpdateCommentDTO.class), anyString()))
                    .thenReturn(updatedComment);

            // When & Then
            mockMvc.perform(patch("/ads/1/comments/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.text").value("Updated comment"));
        }

        @Test
        @WithMockUser
        @DisplayName("Обновление комментария - когда пользователь не имеет прав")
        void updateComment_WhenUserNoPermission_ShouldReturnForbidden() throws Exception {
            // Given
            CreateOrUpdateCommentDTO dto = new CreateOrUpdateCommentDTO();
            dto.setText("Updated comment");

            when(commentService.updateCommentWithPermission(eq(1L), any(CreateOrUpdateCommentDTO.class), anyString()))
                    .thenReturn(null);

            // When & Then
            mockMvc.perform(patch("/ads/1/comments/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
        }
    }
}