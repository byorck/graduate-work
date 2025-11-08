package ru.skypro.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@DisplayName("Тестирование CommentController")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    private final String USERNAME = "test@example.com";

    private CommentsDTO testCommentsDTO;

    @BeforeEach
    void setUp() {
        testCommentsDTO = new CommentsDTO();
        testCommentsDTO.setCount(2);

        CommentDTO comment1 = createCommentDTO(1L, "First comment");
        CommentDTO comment2 = createCommentDTO(2L, "Second comment");
        testCommentsDTO.setResults(Arrays.asList(comment1, comment2));
    }

    @Nested
    @DisplayName("Тесты получения комментариев")
    class GetCommentsTests {

        @Test
        @WithMockUser
        @DisplayName("Получение комментариев по ID объявления")
        void getComments_ShouldReturnOk() throws Exception {
            // Given
            when(commentService.getCommentsByAdId(1L)).thenReturn(testCommentsDTO);

            // When & Then
            mockMvc.perform(get("/ads/1/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(2))
                    .andExpect(jsonPath("$.results[0].id").value(1))
                    .andExpect(jsonPath("$.results[0].text").value("First comment"));
        }
    }

    @Nested
    @DisplayName("Тесты добавления комментариев")
    class AddCommentTests {

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Успешное добавление комментария")
        void addComment_WhenSuccess_ShouldReturnOk() throws Exception {
            // Given
            CreateOrUpdateCommentDTO requestDTO = new CreateOrUpdateCommentDTO();
            requestDTO.setText("New comment");

            CommentDTO responseDTO = createCommentDTO(1L, "New comment");

            when(commentService.addComment(eq(1L), any(CreateOrUpdateCommentDTO.class), eq(USERNAME)))
                    .thenReturn(responseDTO);

            // When & Then
            mockMvc.perform(post("/ads/1/comments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.text").value("New comment"));
        }

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Добавление комментария к несуществующему объявлению")
        void addComment_WhenAdNotFound_ShouldReturnNotFound() throws Exception {
            // Given
            CreateOrUpdateCommentDTO requestDTO = new CreateOrUpdateCommentDTO();
            requestDTO.setText("New comment");

            when(commentService.addComment(eq(1L), any(CreateOrUpdateCommentDTO.class), eq(USERNAME)))
                    .thenReturn(null);

            // When & Then
            mockMvc.perform(post("/ads/1/comments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Тесты удаления комментариев")
    class DeleteCommentTests {

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Удаление комментария авторизованным пользователем")
        void deleteComment_WhenAuthorized_ShouldReturnNoContent() throws Exception {
            // Given
            when(commentService.deleteCommentWithPermission(1L, 1L, USERNAME))
                    .thenReturn(true);

            // When & Then
            mockMvc.perform(delete("/ads/1/comments/1").with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Удаление комментария неавторизованным пользователем")
        void deleteComment_WhenNotAuthorized_ShouldReturnForbidden() throws Exception {
            // Given
            when(commentService.deleteCommentWithPermission(1L, 1L, USERNAME))
                    .thenReturn(false);

            // When & Then
            mockMvc.perform(delete("/ads/1/comments/1").with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Тесты обновления комментариев")
    class UpdateCommentTests {

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Обновление комментария авторизованным пользователем")
        void updateComment_WhenAuthorized_ShouldReturnOk() throws Exception {
            // Given
            CreateOrUpdateCommentDTO requestDTO = new CreateOrUpdateCommentDTO();
            requestDTO.setText("Updated comment");

            CommentDTO responseDTO = createCommentDTO(1L, "Updated comment");

            when(commentService.updateCommentWithPermission(eq(1L), eq(1L), any(CreateOrUpdateCommentDTO.class), eq(USERNAME)))
                    .thenReturn(responseDTO);

            // When & Then
            mockMvc.perform(patch("/ads/1/comments/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.text").value("Updated comment"));
        }

        @Test
        @WithMockUser(username = USERNAME)
        @DisplayName("Обновление комментария неавторизованным пользователем")
        void updateComment_WhenNotAuthorized_ShouldReturnForbidden() throws Exception {
            // Given
            CreateOrUpdateCommentDTO requestDTO = new CreateOrUpdateCommentDTO();
            requestDTO.setText("Updated comment");

            when(commentService.updateCommentWithPermission(eq(1L), eq(1L), any(CreateOrUpdateCommentDTO.class), eq(USERNAME)))
                    .thenReturn(null);

            // When & Then
            mockMvc.perform(patch("/ads/1/comments/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isForbidden());
        }
    }

    private CommentDTO createCommentDTO(Long id, String text) {
        CommentDTO dto = new CommentDTO();
        dto.setId(id);
        dto.setText(text);
        dto.setAuthor(1L);
        dto.setAuthorFirstName("John");
        dto.setCreatedAt(System.currentTimeMillis());
        dto.setAuthorImage("/users/1/avatar");
        return dto;
    }
}