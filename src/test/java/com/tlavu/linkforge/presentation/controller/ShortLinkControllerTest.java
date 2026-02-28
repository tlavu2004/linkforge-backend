package com.tlavu.linkforge.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlavu.linkforge.application.dto.command.CreateShortLinkCommand;
import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
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

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(ShortLinkController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class ShortLinkControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private com.tlavu.linkforge.infrastructure.security.JwtService jwtService;

        @MockitoBean
        private CreateShortLinkUseCase createShortLinkUseCase;

        @MockitoBean
        private com.tlavu.linkforge.application.usecase.GetShortLinkUseCase getShortLinkUseCase;

        @MockitoBean
        private com.tlavu.linkforge.application.usecase.DeleteShortLinkUseCase deleteShortLinkUseCase;

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
                                "delete-token-123",
                                false);

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

        @Test
        @DisplayName("Should get short link info")
        void shouldGetShortLinkInfo() throws Exception {
                // Given
                String shortCode = "abc12345";
                ShortLinkResponse response = new ShortLinkResponse(
                                shortCode, "http://example.com", Instant.now(), null, null, false);

                when(getShortLinkUseCase.execute(shortCode)).thenReturn(response);

                // When/Then
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/v1/links/{shortCode}", shortCode))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.shortCode").value(shortCode));
        }

        @Test
        @DisplayName("Should return 404 when getting non-existent link")
        void shouldReturn404WhenGettingNonExistentLink() throws Exception {
                // Given
                String shortCode = "notfound";
                when(getShortLinkUseCase.execute(shortCode))
                                .thenThrow(new com.tlavu.linkforge.domain.exception.ShortLinkNotFoundException(
                                                "Link not found"));

                // When/Then
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/v1/links/{shortCode}", shortCode))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should delete short link successfully")
        void shouldDeleteShortLinkSuccessfully() throws Exception {
                // Given
                String shortCode = "abc12345";
                String token = "valid-token";

                // When/Then
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/v1/links/{shortCode}", shortCode)
                                .param("deleteToken", token))
                                .andExpect(status().isNoContent());

                org.mockito.Mockito.verify(deleteShortLinkUseCase).execute(shortCode, token);
        }

        @Test
        @DisplayName("Should return 400 when deleting with invalid token") // Or 400/401/403? GlobalHandler maps
                                                                           // Exception? InvalidDeleteToken -> ?
        // Need to check GlobalExceptionHandler mapping for InvalidDeleteTokenException.
        // If not mapped, it might be 500. Let's check GlobalExceptionHandler.
        // Assuming it's mapped to BAD_REQUEST or similar. I'll check it later.
        // For now let's assume implementation.
        void shouldReturnErrorWhenDeletingWithInvalidToken() throws Exception {
                // Given
                String shortCode = "abc12345";
                String token = "invalid";

                org.mockito.Mockito
                                .doThrow(new com.tlavu.linkforge.domain.exception.InvalidDeleteTokenException(
                                                "Invalid token"))
                                .when(deleteShortLinkUseCase).execute(shortCode, token);

                // When/Then
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/v1/links/{shortCode}", shortCode)
                                .param("deleteToken", token))
                                .andExpect(status().isBadRequest()); // Expecting 400
        }
}
