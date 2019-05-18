package com.icthh.xm.ms.entity.security.access;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService.CONFIG_SECTION;
import static com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService.DYNAMIC_FUNCTION_PERMISSION_FEATURE;
import static com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService.FeatureContext;
import static com.icthh.xm.ms.entity.service.impl.FunctionServiceImpl.FUNCTION_CALL_PRIV;
import static com.icthh.xm.ms.entity.service.impl.FunctionServiceImpl.XM_ENITITY_FUNCTION_CALL_PRIV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.BDDMockito.given;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class DynamicPermissionCheckServiceUnitTest extends AbstractUnitTest {

    @Mock
    private PermissionCheckService permissionCheckService;
    @Mock
    private TenantConfigService tenantConfig;
    @InjectMocks
    @Spy
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
        given(tenantConfig.getConfig()).willReturn(newHashMap());
        given(permissionCheckService.hasPermission(null, "X")).willReturn(true);
        boolean result = dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "X", "Y");
        Assertions.assertThat(result).isTrue();

        given(permissionCheckService.hasPermission(null, "Z")).willReturn(false);
        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "Z", "Y"));
    }

    @Test
    public void assertCallCustomCheckIfTenantConfigProvided() {
        //setUp enabled feature
        final Map config = getMockedConfig(CONFIG_SECTION,
            DYNAMIC_FUNCTION_PERMISSION_FEATURE, Boolean.TRUE);

        given(tenantConfig.getConfig()).willReturn(config);
        //privilege == X.Y, assume hasPermission = true
        given(permissionCheckService.hasPermission(null, "X.Y")).willReturn(true);
        boolean result = dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "X", "Y");
        Assertions.assertThat(result).isTrue();

        //privilege == X.Y, assume hasPermission = false
        given(permissionCheckService.hasPermission(null, "Z.Y")).willReturn(false);
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
    public void resultNotChangedIfFunctionFilterFeatureIsOff() {

        given(tenantConfig.getConfig()).willReturn(newHashMap());
        final List<Boolean> someList = newArrayList(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE);

        List<Boolean> resultList = dynamicPermissionCheckService
            .filterInnerListByPermission(someList, null, null, null);

        assertThat(resultList).containsExactly(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void resultInvertedIfFunctionFilterFeatureIsOn() {
        //setUp enabled feature
        final Map<String, Object> config = getMockedConfig(CONFIG_SECTION,
                                                           DYNAMIC_FUNCTION_PERMISSION_FEATURE, Boolean.TRUE);

        given(tenantConfig.getConfig()).willReturn(config);

        Set<String> rolesPrivileges = newHashSet(XM_ENITITY_FUNCTION_CALL_PRIV + ".F1", FUNCTION_CALL_PRIV + ".F3");
        given(dynamicPermissionCheckService.getRoleFunctionPermissions()).willReturn(rolesPrivileges);

        List<FunctionSpec> functions = newArrayList(
            createEntityFunction("F1"),
            createFunction("F2"),
            createFunction("F3")
        );
        TypeSpec typeSpec = new TypeSpec();
        typeSpec.setFunctions(functions);

        TypeSpec result = dynamicPermissionCheckService.filterInnerListByPermission(typeSpec,
                                                                                    typeSpec::getFunctions,
                                                                                    typeSpec::setFunctions,
                                                                                    FunctionSpec::getDynamicPrivilegeKey);

        assertThat(result.getFunctions().stream().map(FunctionSpec::getKey))
            .containsExactly("F1", "F3");

    }

    private FunctionSpec createFunction(String key){
        FunctionSpec spec = new FunctionSpec();
        spec.setKey(key);
        return spec;
    }

    private FunctionSpec createEntityFunction(String key){
        FunctionSpec spec = createFunction(key);
        spec.setWithEntityId(true);
        return spec;
    }

    @Test
    public void testPermissionFilterPredicate(){

        final String role = "ROLE";
        final String permissionKey = "PK";

        List<Permission> mockedList = newArrayList(
            createMockedPermission(role + "_A", permissionKey+"-1"),
            createMockedPermission(role + "_A", permissionKey+"-2"),
            createMockedPermission(role + "_A", permissionKey+"-3"),
            createMockedPermission(role + "_A", permissionKey+"-4"),
            createMockedPermission(role + "_B", permissionKey+"-3"),
            createMockedPermission(role + "_B", permissionKey+"-4"),
            createMockedPermission(role + "_B", permissionKey+"-5"),
            createMockedPermission(role + "_B", permissionKey+"-6"),
            createMockedPermission(role + "_C", permissionKey+"-1"),
            createMockedPermission(role + "_A", permissionKey+"-7"),
            createMockedPermission(role + "_C", permissionKey+"-2", false, false),
            createMockedPermission(role + "_C", permissionKey+"-4", true, false),
            createMockedPermission(role + "_C", permissionKey+"-5", false, true),
            createMockedPermission(role + "_C", permissionKey+"-6", true, true)
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
            .containsExactlyInAnyOrder("PK-1", "PK-2");

        filteredList = mockedList.stream()
            .filter(dynamicPermissionCheckService.functionPermissionMatcher(RoleConstant.SUPER_ADMIN))
            .collect(Collectors.toList());

        assertThat(filteredList.stream().map(Permission::getPrivilegeKey).collect(Collectors.toSet()))
            .containsExactlyInAnyOrder("PK-7", "PK-6", "PK-3", "PK-2", "PK-5", "PK-4", "PK-1");

    }

    private Permission createMockedPermission(String role, String permKey, boolean iDisabled, boolean isDeleted) {
        Permission p = new Permission();
        p.setRoleKey(role);
        p.setPrivilegeKey(permKey);
        p.setDeleted(isDeleted);
        p.setDisabled(iDisabled);
        p.setMsName(CONFIG_SECTION);
        return p;
    }

    private Permission createMockedPermission(String role, String permKey) {
        return createMockedPermission(role, permKey, false, false);
    }

    private Map<String, Object> getMockedConfig(String configSectionName, String featureName, Boolean status) {
        Map<String, Object> map = newHashMap();
        Map<String, Object> section = newHashMap();
        section.put(featureName, status);
        map.put(configSectionName, section);
        return map;
    }
}
