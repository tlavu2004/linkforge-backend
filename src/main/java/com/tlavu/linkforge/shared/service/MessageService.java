package com.tlavu.linkforge.shared.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    public String getMessage(String code) {
        return getMessage(code, (Object[]) null);
    }

    public String getMessage(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return Objects.requireNonNull(messageSource.getMessage(code, args, code, locale));
    }

    public String getMessage(String code, Locale locale, Object... args) {
        return Objects.requireNonNull(messageSource.getMessage(code, args, code, locale));
    }
}
