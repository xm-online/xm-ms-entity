package com.icthh.xm.ms.entity.service.json;

import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(MockitoJUnitRunner.class)
public class JsonListenerServiceUnitTest extends AbstractUnitTest {

    private static final String TENANT_NAME = "XM";
    private static final String RELATIVE_PATH_TO_FILE = "xmentityspec/definitions/user.json";
    private static final String CONFIG = "{user: type: object}";

    private JsonListenerService subject;

    @Before
    public void setUp() {
        subject = new JsonListenerService();
    }

    @Test
    public void onRefreshSuccess() {
        subject.processTenantSpecification(TENANT_NAME, RELATIVE_PATH_TO_FILE, CONFIG);

        Map<String, String> actual = subject.getSpecificationByTenant(TENANT_NAME);

        assertThat(actual.get(RELATIVE_PATH_TO_FILE)).isEqualTo(CONFIG);
    }

    @Test
    public void onRefreshWhenConfigEmptyFail() {
        subject.processTenantSpecification(TENANT_NAME, null, null);

        Map<String, String> actual = subject.getSpecificationByTenant(TENANT_NAME);

        assertThat(actual).isNull();
    }

    @Test
    public void getSpecificationByTenantRelativePathSuccess() {
        subject.processTenantSpecification(TENANT_NAME, RELATIVE_PATH_TO_FILE, CONFIG);

        String actual = subject.getSpecificationByTenantRelativePath(TENANT_NAME, RELATIVE_PATH_TO_FILE);

        assertThat(actual).isEqualTo(CONFIG);
    }

    @Test
    public void getSpecificationByTenantRelativePathWhenPathNotEqualsFail() {
        String notRelativePath = "not/relative/path/user.json";
        subject.processTenantSpecification(TENANT_NAME, RELATIVE_PATH_TO_FILE, CONFIG);

        String specificationByTenantRelativePath = subject.getSpecificationByTenantRelativePath(TENANT_NAME, notRelativePath);

        assertThat(specificationByTenantRelativePath).isEmpty();
    }
}
