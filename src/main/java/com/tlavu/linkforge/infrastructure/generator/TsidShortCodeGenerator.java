package com.tlavu.linkforge.infrastructure.generator;

import com.tlavu.linkforge.domain.service.ShortCodeGenerator;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import com.tlavu.linkforge.domain.util.Base62Encoder;
import io.hypersistence.tsid.TSID;
import org.springframework.stereotype.Service;

@Service
public class TsidShortCodeGenerator implements ShortCodeGenerator {

    @Override
    public ShortCode generate() {
        // Generate a new TSID
        TSID tsid = TSID.fast();
        // Get the long value
        long id = tsid.toLong();
        // Encode to Base62
        String code = Base62Encoder.encode(id);
        // Return as ShortCode value object
        return ShortCode.of(code);
    }
}
