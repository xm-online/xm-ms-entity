package com.icthh.xm.ms.entity.bug.hibernate.update;

import static org.mockito.Mockito.when;

import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import java.util.Optional;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class XmEntityHibernateBatchUpdateErrorConfigOverride {

    @Bean
    @Primary
    XmAuthenticationContext xmAuthenticationContext() {
        XmAuthenticationContext xmAuthContext = Mockito.mock(XmAuthenticationContext.class);
        when(xmAuthContext.getUserKey()).thenReturn(Optional.of("qa"));
        return xmAuthContext;
    }

    @Bean
    @Primary
    XmAuthenticationContextHolder XmAuthenticationContextHolder(XmAuthenticationContext xmAuthContext) {
        XmAuthenticationContextHolder contextHolder = Mockito.mock(XmAuthenticationContextHolder.class);
        when(contextHolder.getContext()).thenReturn(xmAuthContext);
        return contextHolder;
    }

}
