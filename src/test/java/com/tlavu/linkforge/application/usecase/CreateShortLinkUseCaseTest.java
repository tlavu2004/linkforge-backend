package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.CreateShortLinkCommand;
import com.tlavu.linkforge.application.dto.ShortLinkResponse;
import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.service.ShortCodeGenerator;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateShortLinkUseCaseTest {

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @InjectMocks
    private CreateShortLinkUseCaseImpl useCase;

    @Test
    @DisplayName("Should create short link successfully")
    void shouldCreateShortLinkSuccessfully() {
        // Given
        String originalUrl = "http://example.com";
        CreateShortLinkCommand command = new CreateShortLinkCommand(originalUrl, null);
        ShortCode expectedCode = ShortCode.of("abc12345");

        when(shortCodeGenerator.generate()).thenReturn(expectedCode);
        when(shortLinkRepository.save(any(ShortLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ShortLinkResponse response = useCase.execute(command);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.shortCode()).isEqualTo(expectedCode.code());
        assertThat(response.originalUrl()).isEqualTo(originalUrl);
        assertThat(response.createdAt()).isNotNull();

        verify(shortCodeGenerator).generate();
        verify(shortLinkRepository).save(any(ShortLink.class));
    }
}
