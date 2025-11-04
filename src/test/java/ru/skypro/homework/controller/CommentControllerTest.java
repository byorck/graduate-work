package ru.skypro.homework.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.skypro.homework.config.TestConfig;
import ru.skypro.homework.config.WebSecurityConfig;
import ru.skypro.homework.dto.comment.CommentDTO;
import ru.skypro.homework.dto.comment.CommentsDTO;
import ru.skypro.homework.dto.comment.CreateOrUpdateCommentDTO;
import ru.skypro.homework.service.CommentService;
import ru.skypro.homework.service.CustomUserDetailsService;
import ru.skypro.homework.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@Import({WebSecurityConfig.class, TestConfig.class})
@WithMockUser
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private UserService userService;

    private CommentDTO commentDto;
    private CommentsDTO commentsDto;

    @BeforeEach
    void setUp() {
        commentDto = new CommentDTO();
        commentDto.setId(1L);
        commentDto.setText("Test comment");
        commentDto.setAuthor(1L);
        commentDto.setAuthorFirstName("John");
        commentDto.setCreatedAt(LocalDateTime.now());

        commentsDto = new CommentsDTO();
        commentsDto.setCount(1);
        commentsDto.setResults(List.of(commentDto));
    }

    @Test
    void getComments_ShouldReturnComments() throws Exception {
        when(commentService.getCommentsByAdId(1L)).thenReturn(commentsDto);

        mockMvc.perform(get("/ads/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.results[0].id").value(1L))
                .andExpect(jsonPath("$.results[0].text").value("Test comment"));
    }

    @Test
    @WithMockUser(username = "test@mail.com")
    void addComment_ShouldReturnComment() throws Exception {
        when(commentService.addComment(eq(1L), any(CreateOrUpdateCommentDTO.class), eq("test@mail.com")))
                .thenReturn(commentDto);

        String jsonContent = "{\"text\": \"New comment\"}";

        mockMvc.perform(post("/ads/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.text").value("Test comment"));
    }

    @Test
    @WithMockUser(username = "test@mail.com")
    void addComment_WhenInvalidData_ShouldReturnBadRequest() throws Exception {
        String jsonContent = "{\"text\": \"\"}";

        mockMvc.perform(post("/ads/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "author@mail.com", roles = "USER")
    public void deleteComment_WhenUserHasPermission_ShouldReturnNoContent() throws Exception {
        // Исправляем на hasDeletePermission
        given(commentService.hasDeletePermission(1L, "author@mail.com")).willReturn(true);
        given(commentService.deleteComment(1L, "author@mail.com")).willReturn(true);

        mockMvc.perform(delete("/ads/1/comments/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(1L, "author@mail.com");
    }

    @Test
    @WithMockUser(username = "other@mail.com")
    void deleteComment_WhenUserNoPermission_ShouldReturnForbidden() throws Exception {
        // Исправляем на hasDeletePermission
        when(commentService.hasDeletePermission(1L, "other@mail.com")).thenReturn(false);

        mockMvc.perform(delete("/ads/1/comments/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "author@mail.com")
    void updateComment_WhenUserHasPermission_ShouldReturnUpdatedComment() throws Exception {
        CommentDTO updatedComment = new CommentDTO();
        updatedComment.setId(1L);
        updatedComment.setText("Updated comment");

        // Исправляем на hasUpdatePermission
        when(commentService.hasUpdatePermission(1L, "author@mail.com")).thenReturn(true);
        when(commentService.updateComment(eq(1L), any(CreateOrUpdateCommentDTO.class), eq("author@mail.com")))
                .thenReturn(updatedComment);

        String jsonContent = "{\"text\": \"Updated comment\"}";

        mockMvc.perform(patch("/ads/1/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Updated comment"));
    }

    @Test
    @WithMockUser(username = "other@mail.com")
    void updateComment_WhenUserNoPermission_ShouldReturnForbidden() throws Exception {
        // Исправляем на hasUpdatePermission
        when(commentService.hasUpdatePermission(1L, "other@mail.com")).thenReturn(false);

        String jsonContent = "{\"text\": \"Updated comment\"}";

        mockMvc.perform(patch("/ads/1/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}