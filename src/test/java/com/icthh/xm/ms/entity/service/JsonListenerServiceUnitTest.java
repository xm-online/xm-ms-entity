package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.AbstractUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(MockitoJUnitRunner.class)
public class JsonListenerServiceUnitTest extends AbstractUnitTest {
    private static final String ENTITY_APP_NAME = "entity";
    private static final String TENANT_NAME = "XM";
    private static final String PATH_TO_FILE = "/config/tenants/XM/entity/xmentityspec/definitions/user.json";
    private static final String CONFIG = "{user: type: object}";
    private JsonListenerService subject;

    @Before
    public void setUp(){
        subject = new JsonListenerService(ENTITY_APP_NAME);
    }

    @Test
    public void isListeningConfiguration_success() {
        boolean actual = subject.isListeningConfiguration(PATH_TO_FILE);

        assertThat(actual).isTrue();
    }

    @Test
    public void isListeningConfiguration_whenFileNotJson_fail(){
        String incorrectFile = "/config/tenants/XM/entity/definitions/user.yml";
        boolean actual = subject.isListeningConfiguration(incorrectFile);

        assertThat(actual).isFalse();
    }

    @Test
    public void onRefresh_success(){
        String relativePath = "definitions/user.json";

        subject.onRefresh(PATH_TO_FILE, CONFIG);

        Map<String, String> actual = subject.getSpecificationByTenant(TENANT_NAME);

        assertThat(actual.get(relativePath)).isEqualTo(CONFIG);
    }

    @Test
    public void onRefresh_whenConfigEmpty_fail(){
        subject.onRefresh(PATH_TO_FILE,null);

        Map<String, String> actual = subject.getSpecificationByTenant(TENANT_NAME);

        assertThat(actual).isNull();
    }

    @Test
    public void getSpecificationByTenantRelativePath_success(){
        String relativePath = "xmentityspec/definitions/user.json";
        subject.onRefresh(PATH_TO_FILE, CONFIG);

        String actual = subject.getSpecificationByTenantRelativePath(TENANT_NAME, relativePath);

        assertThat(actual).isEqualTo(CONFIG);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getSpecificationByTenantRelativePath_whenPathNotEquals_fail(){
        String notRelativePath = "not/relative/path/user.json";
        subject.onRefresh(PATH_TO_FILE, CONFIG);

        subject.getSpecificationByTenantRelativePath(TENANT_NAME, notRelativePath);
    }
}
