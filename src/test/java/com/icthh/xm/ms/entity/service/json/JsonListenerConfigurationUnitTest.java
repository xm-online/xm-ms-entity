package com.icthh.xm.ms.entity.service.json;

import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.service.spec.XmEntitySpecContextService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class JsonListenerConfigurationUnitTest extends AbstractUnitTest {

    private static final String ENTITY_APP_NAME = "entity";
    private static final String TENANT_NAME = "XM";
    private static final String PATH_TO_FILE = "/config/tenants/XM/entity/xmentityspec/definitions/user.json";
    private static final String CONFIG = "{user: type: object}";

    private JsonListenerConfiguration subject;
    @Mock
    private JsonListenerService jsonListenerService;
    @Mock
    private XmEntitySpecContextService xmEntitySpecContextService;

    @Before
    public void setUp() {
        subject = new JsonListenerConfiguration(ENTITY_APP_NAME, xmEntitySpecContextService, jsonListenerService);
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
