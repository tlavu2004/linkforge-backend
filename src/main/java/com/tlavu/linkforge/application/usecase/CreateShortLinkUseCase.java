package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.command.CreateShortLinkCommand;
import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;

public interface CreateShortLinkUseCase {
    ShortLinkResponse execute(CreateShortLinkCommand command);
}
