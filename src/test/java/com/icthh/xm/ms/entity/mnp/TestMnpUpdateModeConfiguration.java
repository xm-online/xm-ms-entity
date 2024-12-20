package com.icthh.xm.ms.entity.mnp;

import com.icthh.xm.commons.lep.spring.LepUpdateMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TestMnpUpdateModeConfiguration {

    @Bean
    @Primary
    public LepUpdateMode lepUpdateMode() {
        return LepUpdateMode.LIVE;
    }

}
