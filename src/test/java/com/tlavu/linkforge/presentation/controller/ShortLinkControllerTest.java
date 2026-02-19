package com.tlavu.linkforge.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlavu.linkforge.application.dto.CreateShortLinkCommand;
import com.tlavu.linkforge.application.dto.ShortLinkResponse;
import com.tlavu.linkforge.application.usecase.CreateShortLinkUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShortLinkController.class)
class ShortLinkControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private CreateShortLinkUseCase createShortLinkUseCase;

        @Test
        @DisplayName("Should create short link and return 201 Created")
        void shouldCreateShortLink() throws Exception {
                // Given
                CreateShortLinkCommand command = new CreateShortLinkCommand("http://example.com", null);
                ShortLinkResponse response = new ShortLinkResponse(
                                "abc12345",
                                "http://example.com",
                                Instant.now(),
                                null,
                                "delete-token-123");

                when(createShortLinkUseCase.execute(any(CreateShortLinkCommand.class))).thenReturn(response);

                // When/Then
                mockMvc.perform(post("/api/v1/links")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(command)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.shortCode").value("abc12345"))
                                .andExpect(jsonPath("$.data.originalUrl").value("http://example.com"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when URL is invalid")
        void shouldReturn400WhenUrlIsInvalid() throws Exception {
                // Given
                CreateShortLinkCommand command = new CreateShortLinkCommand("invalid-url", null);

                // Mock exception
                when(createShortLinkUseCase.execute(any(CreateShortLinkCommand.class)))
                                .thenThrow(new com.tlavu.linkforge.domain.exception.InvalidUrlException("Invalid URL"));

                // When/Then
                mockMvc.perform(post("/api/v1/links")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(command)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("Invalid URL"));
        }
}
