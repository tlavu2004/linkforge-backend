package com.tlavu.linkforge.presentation.controller;

import com.tlavu.linkforge.application.dto.ShortLinkResponse;
import com.tlavu.linkforge.application.usecase.ResolveShortLinkUseCase;
import com.tlavu.linkforge.domain.exception.ShortLinkNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RedirectController.class)
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResolveShortLinkUseCase resolveShortLinkUseCase;

    @Test
    @DisplayName("Should redirect to original URL with 301 status")
    void shouldRedirectToOriginalUrl() throws Exception {
        // Given
        String shortCode = "abc12345";
        String originalUrl = "http://example.com";
        ShortLinkResponse response = new ShortLinkResponse(shortCode, originalUrl, Instant.now(), null, null);

        when(resolveShortLinkUseCase.execute(shortCode)).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/r/{shortCode}", shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", originalUrl));
    }

    @Test
    @DisplayName("Should return 404 Not Found when link does not exist")
    void shouldReturn404WhenLinkNotFound() throws Exception {
        // Given
        String shortCode = "notfound";
        when(resolveShortLinkUseCase.execute(shortCode))
                .thenThrow(new ShortLinkNotFoundException("Link not found"));

        // When/Then
        mockMvc.perform(get("/r/{shortCode}", shortCode))
                .andExpect(status().isNotFound());
    }
}
