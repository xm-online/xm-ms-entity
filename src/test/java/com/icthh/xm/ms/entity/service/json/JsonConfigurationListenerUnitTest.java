package com.icthh.xm.ms.entity.service.json;

import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.service.spec.XmEntitySpecContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class JsonConfigurationListenerUnitTest extends AbstractJupiterUnitTest {

    private static final String ENTITY_APP_NAME = "entity";
    private static final String TENANT_NAME = "XM";
    private static final String PATH_TO_FILE = "/config/tenants/XM/entity/xmentityspec/definitions/user.json";
    private static final String CONFIG = "{user: type: object}";

    private JsonConfigurationListener subject;
    @Mock
    private JsonListenerService jsonListenerService;
    @Mock
    private XmEntitySpecContextService xmEntitySpecContextService;

    @BeforeEach
    public void setUp() {
        subject = new JsonConfigurationListener(ENTITY_APP_NAME, xmEntitySpecContextService, jsonListenerService);
    }

    @Test
    public void isListeningConfigurationSuccess() {
        boolean actual = subject.isListeningConfiguration(PATH_TO_FILE);

        assertThat(actual).isTrue();
    }

    @Test
    public void isListeningConfigurationWhenFileNotJsonFail() {
        String incorrectFile = "/config/tenants/XM/entity/definitions/user.yml";
        boolean actual = subject.isListeningConfiguration(incorrectFile);

        assertThat(actual).isFalse();
    }

    @Test
    public void onRefreshSuccess() {
        String relativePath = "xmentityspec/definitions/user.json";

        subject.onRefresh(PATH_TO_FILE, CONFIG);

        verify(jsonListenerService).processTenantSpecification(eq(TENANT_NAME), eq(relativePath), eq(CONFIG));
        verifyNoMoreInteractions(jsonListenerService);
    }

    @Test
    public void refreshFinishedSuccess() {
        subject.refreshFinished(Collections.singletonList(PATH_TO_FILE));

        verify(xmEntitySpecContextService).updateByTenantState(eq(TENANT_NAME));
        verifyNoMoreInteractions(xmEntitySpecContextService);
    }
}
