package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.lep.spring.LepUpdateMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestLepUpdateModeConfiguration {

    @Bean
    public LepUpdateMode lepUpdateMode() {
        return LepUpdateMode.SYNCHRONOUS;
    }

}
