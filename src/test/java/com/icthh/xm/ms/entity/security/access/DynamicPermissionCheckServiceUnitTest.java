package com.icthh.xm.ms.entity.security.access;

import com.google.common.collect.Maps;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService.*;
import static org.assertj.core.api.Assertions.assertThat;
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
        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "Z", "Y"));
    }

    @Test
    public void assertCallCustomCheckIfTenantConfigProvided() {
        //setUp enabled feature
        final Map config = getMockedConfig(CONFIG_SECTION,
            DYNAMIC_FUNCTION_PERMISSION_FEATURE, Boolean.TRUE);

        BDDMockito.given(tenantConfig.getConfig()).willReturn(config);
        //privilege == X.Y, assume hasPermission = true
        BDDMockito.given(permissionCheckService.hasPermission(null, "X.Y")).willReturn(true);
        boolean result = dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "X", "Y");
        Assertions.assertThat(result).isTrue();

        //privilege == X.Y, assume hasPermission = false
        BDDMockito.given(permissionCheckService.hasPermission(null, "Z.Y")).willReturn(false);
        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "Z", "Y"));

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

    @Test
    public void resultNotChangedIfFunctionFilterFeatureIsOff(){
        BDDMockito.given(tenantConfig.getConfig()).willReturn(Maps.newHashMap());
        final List<Boolean> someList = Lists.newArrayList(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE);
        Function<List<Boolean>, List<Boolean>> listSupplier = list -> list;
        final List<Boolean> resultList = listSupplier.
            andThen(dynamicPermissionCheckService.dynamicFunctionListFilter(inversionMapper))
            .apply(someList);
        assertThat(resultList).containsExactly(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void resultInvertedIfFunctionFilterFeatureIsOn(){
        //setUp enabled feature
        final Map config = getMockedConfig(CONFIG_SECTION,
            DYNAMIC_FUNCTION_PERMISSION_FEATURE, Boolean.TRUE);

        BDDMockito.given(tenantConfig.getConfig()).willReturn(config);
        final List<Boolean> someList = Lists.newArrayList(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE);
        Function<List<Boolean>, List<Boolean>> listSupplier = list -> list;
        final List<Boolean> resultList = listSupplier
            .andThen(dynamicPermissionCheckService.dynamicFunctionListFilter(inversionMapper))
            .apply(someList);
        assertThat(resultList).containsExactly(Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE);
    }

    @Test
    public void singleItemNotFilteredIfFeatureIsOff(){
        BDDMockito.given(tenantConfig.getConfig()).willReturn(Maps.newHashMap());
        Boolean item = Boolean.TRUE;
        Function<Boolean, Boolean> itemSupplier = i -> i;
        Boolean result = itemSupplier
            .andThen(dynamicPermissionCheckService.dynamicFunctionFilter(inversionMapper))
            .apply(item);
        assertThat(result).isTrue();
    }

    @Test
    public void singleItemInvertedIfFeatureIsOn(){
        BDDMockito.given(tenantConfig.getConfig()).willReturn(Maps.newHashMap());
        Boolean item = Boolean.TRUE;
        Function<Boolean, Boolean> itemSupplier = i -> i;
        Boolean result = itemSupplier
            .andThen(dynamicPermissionCheckService.dynamicFunctionFilter(inversionMapper))
            .apply(item);
        assertThat(result).isFalse();
    }

    @Test
    public void testPermissionFilterPredicate(){

        final String role = "ROLE";
        final String privKey = "PK";

        List<Permission> mockedList = Lists.newArrayList(
            createMockedPermission(role + "_A", privKey+"-1"),
            createMockedPermission(role + "_A", privKey+"-2"),
            createMockedPermission(role + "_A", privKey+"-3"),
            createMockedPermission(role + "_A", privKey+"-4"),
            createMockedPermission(role + "_B", privKey+"-3"),
            createMockedPermission(role + "_B", privKey+"-4"),
            createMockedPermission(role + "_B", privKey+"-5"),
            createMockedPermission(role + "_B", privKey+"-6"),
            createMockedPermission(role + "_C", privKey+"-1"),
            createMockedPermission(role + "_A", privKey+"-7")
        );

        List<Permission> filteredList = mockedList.stream()
            .filter(dynamicPermissionCheckService.functionPermissionMatcher(role + "_XXX"))
            .collect(Collectors.toList());

        assertThat(filteredList.stream().map(Permission::getPrivilegeKey).collect(Collectors.toSet()).isEmpty())
            .isTrue();

        filteredList = mockedList.stream()
            .filter(dynamicPermissionCheckService.functionPermissionMatcher(role + "_A"))
            .collect(Collectors.toList());

        assertThat(filteredList.stream().map(Permission::getPrivilegeKey).collect(Collectors.toSet()))
            .containsExactlyInAnyOrder("PK-1", "PK-2", "PK-3", "PK-4", "PK-7");

        filteredList = mockedList.stream()
            .filter(dynamicPermissionCheckService.functionPermissionMatcher(role + "_B"))
            .collect(Collectors.toList());

        assertThat(filteredList.stream().map(Permission::getPrivilegeKey).collect(Collectors.toSet()))
            .containsExactlyInAnyOrder("PK-3", "PK-4", "PK-5", "PK-6");

        filteredList = mockedList.stream()
            .filter(dynamicPermissionCheckService.functionPermissionMatcher(role + "_C"))
            .collect(Collectors.toList());

        assertThat(filteredList.stream().map(Permission::getPrivilegeKey).collect(Collectors.toSet()))
            .containsExactlyInAnyOrder("PK-1");

    }

    /**
     * Inverse incoming item. Used to test feature enabled flag
     */
    private BiFunction<Boolean, Set<String>, Boolean> inversionMapper = (item, set) -> !item;

    private Permission createMockedPermission(String role, String permKey) {
        Permission p = new Permission();
        p.setRoleKey(role);
        p.setPrivilegeKey(permKey);
        p.setMsName(CONFIG_SECTION);
        return p;
    }

    private Map<String, Object> getMockedConfig(String configSectionName, String featureName, Boolean status) {
        Map<String, Object> map = Maps.newHashMap();
        Map<String, Object> section = Maps.newHashMap();
        section.put(featureName, status);
        map.put(configSectionName, section);
        return map;
    }
}
