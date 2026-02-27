package com.tlavu.linkforge.application.usecase;

public interface VerifyAdTokenUseCase {
    String execute(String token, String shortCode);
}
