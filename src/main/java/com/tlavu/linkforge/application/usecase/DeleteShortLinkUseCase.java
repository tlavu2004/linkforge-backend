package com.tlavu.linkforge.application.usecase;

public interface DeleteShortLinkUseCase {
    void execute(String shortCode, String deleteToken);
}
