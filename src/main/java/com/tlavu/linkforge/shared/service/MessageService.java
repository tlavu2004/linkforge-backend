package com.tlavu.linkforge.shared.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    @NonNull
    public String getMessage(@NonNull String code) {
        return getMessage(code, (Object[]) null);
    }

    @NonNull
    public String getMessage(@NonNull String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(code, args, code, locale);
        return Objects.requireNonNull(message);
    }

    @NonNull
    public String getMessage(@NonNull String code, @NonNull Locale locale, Object... args) {
        String message = messageSource.getMessage(code, args, code, locale);
        return Objects.requireNonNull(message);
    }
}
