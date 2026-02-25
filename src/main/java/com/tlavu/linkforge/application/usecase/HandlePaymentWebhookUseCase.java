package com.tlavu.linkforge.application.usecase;

import java.util.Map;

public interface HandlePaymentWebhookUseCase {
    void execute(Map<String, String> params);
}
