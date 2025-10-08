package com.icthh.xm.ms.entity.service;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static com.icthh.xm.ms.entity.config.TenantConfigMockConfiguration.getXmEntityTemplatesSpec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class XmEntityTemplatesSpecServiceUnitTest extends AbstractJupiterUnitTest {

    private static final String TENANT = "TEST";

    private static final String URL = "/config/tenants/{tenantName}/entity/templates/search-templates.yml";

    private XmEntityTemplatesSpecService target;

    @Mock
    private TenantContextHolder tenantContextHolder;
    @Mock
    private TenantContext tenantContext;


    @BeforeEach
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
