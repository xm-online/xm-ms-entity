package com.icthh.xm.ms.entity.service;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static com.icthh.xm.ms.entity.config.TenantConfigMockConfiguration.getXmEntityTemplatesSpec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class XmEntityTemplatesSpecServiceUnitTest {

    private static final String TENANT = "TEST";

    private static final String URL = "/config/tenants/{tenantName}/entity/templates/search-templates.yml";

    private XmEntityTemplatesSpecService target;

    @Mock
    private TenantContextHolder tenantContextHolder;
    @Mock
    private TenantContext tenantContext;


    @Before
    @SneakyThrows
    public void init() {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(TENANT)));
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);

        ApplicationProperties ap = new ApplicationProperties();
        ap.setSpecificationTemplatesPathPattern(URL);
        target = new XmEntityTemplatesSpecService(ap, tenantContextHolder);
        String tenantName = getRequiredTenantKeyValue(tenantContextHolder);
        String config = getXmEntityTemplatesSpec(tenantName);
        String key = ap.getSpecificationTemplatesPathPattern().replace("{tenantName}", tenantName);
        target.onRefresh(key, config);
    }

    @Test
    public void shouldFindTemplate() {
        String query = target.findTemplate("BY_TYPEKEY_AND_NAME");
        assertThat(query).isEqualTo("typeKey:%s AND name:%s");
    }

    @Test
    public void shouldNotFindTemplate() {
        assertThatThrownBy(() -> target.findTemplate("BAD_KEY"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Template query not found: BAD_KEY");
    }
}
