package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.CreateShortLinkCommand;
import com.tlavu.linkforge.application.dto.ShortLinkResponse;

public interface CreateShortLinkUseCase {
    ShortLinkResponse execute(CreateShortLinkCommand command);
}
