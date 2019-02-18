package com.icthh.xm.ms.entity.security.access;

import com.google.common.collect.Maps;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import javax.validation.ConstraintViolationException;

import java.util.Map;

import static com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService.*;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@RunWith(MockitoJUnitRunner.class)
public class DynamicPermissionCheckServiceUnitTest {

    @Mock
    private PermissionCheckService permissionCheckService;
    @Mock
    private TenantConfigService tenantConfig;
    @InjectMocks
    private DynamicPermissionCheckService dynamicPermissionCheckService;

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void checkContextPermissionFailsIfBasePermissionIsNull() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, null, "SSS"));
    }

    @Test
    public void checkContextPermissionFailsIfBasePermissionIsEmpty() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "", "SSS"));
    }

    @Test
    public void checkContextPermissionFailsIfSuffixPermissionIsNull() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "XXX", null));
    }

    @Test
    public void checkContextPermissionFailsIfSuffixPermissionIsEmpty() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "XXX", ""));
    }

    @Test
    public void assertCallBasicCheckIfTenantConfigNotProvided() {
        BDDMockito.given(tenantConfig.getConfig()).willReturn(Maps.newHashMap());
        BDDMockito.given(permissionCheckService.hasPermission(null, "X")).willReturn(true);
        boolean result = dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "X", "Y");
        Assertions.assertThat(result).isTrue();

        BDDMockito.given(permissionCheckService.hasPermission(null, "Z")).willReturn(false);
        result = dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "Z", "Y");
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void assertCallCustomCheckIfTenantConfigProvided() {
        //setUp enabled feature
        final Map config = getMockedConfig(DynamicPermissionCheckService.CONFIG_SECTION,
            DYNAMIC_FUNCTION_PERMISSION_FEATURE, Boolean.TRUE);

        BDDMockito.given(tenantConfig.getConfig()).willReturn(config);
        //privilege == X.Y, assume hasPermission = true
        BDDMockito.given(permissionCheckService.hasPermission(null, "X.Y")).willReturn(true);
        boolean result = dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "X", "Y");
        Assertions.assertThat(result).isTrue();

        //privilege == X.Y, assume hasPermission = false
        BDDMockito.given(permissionCheckService.hasPermission(null, "Z.Y")).willReturn(false);
        result = dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "Z", "Y");
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void checkStateChangePermissionFailsIfSuffixPermissionIsNull() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.CHANGE_STATE, "XXX", null));
    }

    @Test
    public void checkStateChangePermissionFailsIfSuffixPermissionIsEmpty() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.CHANGE_STATE, "XXX", ""));
    }

    @Test
    public void checkStateChangeUnsupportedYet() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.CHANGE_STATE, "XXX", "YY"));
    }

    private Map<String, Object> getMockedConfig(String configSectionName, String featureName, Boolean status) {
        Map<String, Object> map = Maps.newHashMap();
        Map<String, Object> section = Maps.newHashMap();
        section.put(featureName, status);
        map.put(configSectionName, section);
        return map;
    }
}
