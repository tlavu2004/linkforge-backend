package com.tlavu.linkforge.application.usecase;

public interface CreatePaymentLinkUseCase {
    String execute(Long userId, String packageCode, String ipAddress);
}
