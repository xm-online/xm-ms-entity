package com.icthh.xm.ms.entity.security.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.permission.service.PermissionService;
import com.icthh.xm.commons.tenant.PlainTenant;
import com.icthh.xm.commons.tenant.Tenant;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.NextSpec;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.privileges.custom.EntityStateCustomPrivilegesExtractor;
import com.icthh.xm.ms.entity.service.privileges.custom.FunctionCustomPrivilegesExtractor;
import com.icthh.xm.ms.entity.util.TypeSpecTestUtils;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.newHashMap;
import static com.icthh.xm.commons.permission.constants.RoleConstant.SUPER_ADMIN;
import static com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService.FeatureContext;
import static com.icthh.xm.ms.entity.service.impl.FunctionServiceImpl.FUNCTION_CALL_PRIV;
import static com.icthh.xm.ms.entity.service.impl.FunctionServiceImpl.XM_ENITITY_FUNCTION_CALL_PRIV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DynamicPermissionCheckServiceUnitTest extends AbstractUnitTest {

    private static final String DYNAMIC_FUNCTION_PERMISSION_FEATURE = "dynamicPermissionCheckEnabled";

    @Mock
    private OAuth2Authentication auth;
    @Mock
    private TenantContextHolder tenantContextHolder;
    @Mock
    private PermissionCheckService permissionCheckService;
    @Mock
    private PermissionService permissionService;
    @Spy
    private XmEntityTenantConfigService tenantConfig = new XmEntityTenantConfigService(new XmConfigProperties(),
                                                                                       tenantContextHolder());

    private TenantContextHolder tenantContextHolder() {
        TenantContextHolder mock = mock(TenantContextHolder.class);
        applyMock(mock);
        return mock;
    }

    private void applyMock(TenantContextHolder mock) {
        when(mock.getTenantKey()).thenReturn("XM");
        when(mock.getContext()).thenReturn(new TenantContext() {
            @Override
            public boolean isInitialized() {
                return true;
            }

            @Override
            public Optional<Tenant> getTenant() {
                return Optional.of(new PlainTenant(new TenantKey("XM")));
            }
        });
    }

    @InjectMocks
    @Spy
    private DynamicPermissionCheckService dynamicPermissionCheckService;

    @Before
    public void setUp() {
        when(permissionService.getPermissions("XM")).thenReturn(new HashMap<>());
        applyMock(tenantContextHolder);
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
    public void checkContextWithSupplierPermissionFailsIfBasePermissionIsNull() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, null, () -> "SSS"));
    }

    @Test
    public void checkContextPermissionFailsIfBasePermissionIsEmpty() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "", "SSS"));
    }

    @Test
    public void checkContextPermissionFailsIfSuffixPermissionIsNull() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "XXX", (String) null));
    }

    @Test
    public void checkContextPermissionFailsIfSuffixPermissionIsEmpty() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "XXX", ""));
    }

    @Test
    public void assertCallBasicCheckIfTenantConfigNotProvided() {
        prepareConfig(newHashMap());
        given(permissionCheckService.hasPermission(null, "X")).willReturn(true);
        boolean result = dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "X", "Y");
        Assertions.assertThat(result).isTrue();

        given(permissionCheckService.hasPermission(null, "Z")).willReturn(false);
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "Z", "Y"));
    }

    @Test
    public void assertCallBasicCheckIfTenantConfigNotProvidedWithSupplier() {
        prepareConfig(newHashMap());
        given(permissionCheckService.hasPermission(null, "X")).willReturn(true);
        boolean result = dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "X", () -> "Y");
        Assertions.assertThat(result).isTrue();

        given(permissionCheckService.hasPermission(null, "Z")).willReturn(false);
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "Z", () -> "Y"));
    }

    @Test
    public void assertCallCustomCheckIfTenantConfigProvided() {
        //setUp enabled feature
        final Map config = getMockedConfig(FunctionCustomPrivilegesExtractor.SECTION_NAME,
            DYNAMIC_FUNCTION_PERMISSION_FEATURE, Boolean.TRUE);

        prepareConfig(config);
        //privilege == X.Y, assume hasPermission = true
        given(permissionCheckService.hasPermission(null, "X.Y")).willReturn(true);
        boolean result = dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "X", "Y");
        Assertions.assertThat(result).isTrue();

        //privilege == X.Y, assume hasPermission = false
        given(permissionCheckService.hasPermission(null, "Z.Y")).willReturn(false);
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.FUNCTION, "Z", "Y"));

    }

    @Test
    public void checkStateChangePermissionFailsIfSuffixPermissionIsNull() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.CHANGE_STATE, "XXX", (String) null));
    }

    @Test
    public void checkStateChangePermissionFailsIfSuffixPermissionIsEmpty() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.CHANGE_STATE, "XXX", ""));
    }

    @Test
    public void checkStateChangeAccessDeniedException() {
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> dynamicPermissionCheckService.checkContextPermission(FeatureContext.CHANGE_STATE, "XXX", "ZZ"));
    }

    @Test
    public void resultNotChangedIfFunctionFilterFeatureIsOff() {

        prepareConfig(newHashMap());
        final List<Boolean> someList = newArrayList(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE);

        List<Boolean> resultList = dynamicPermissionCheckService
            .filterInnerFunctionListByPermission(Set.of(), someList, null, null, null);

        assertThat(resultList).containsExactly(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void filterFunctionPermissionForSuperAdmin() {
        withMockUser(auth, SUPER_ADMIN, () -> filterFunctionPermission("F1", "F2", "F3"));
    }

    @Test
    public void resultInvertedIfFunctionFilterFeatureIsOn() {
        withMockUser(auth, "USER", () -> filterFunctionPermission("F1", "F3"));
    }

    @Test
    public void filterStatePermissionForSuperAdmin() {
        withMockUser(auth, SUPER_ADMIN, () -> {
            List<StateSpec> filteredList = filterStatePermission();
            TypeSpec typeSpec = demoTypeSpec();
            assertThat(typeSpec.getStates()).isEqualTo(filteredList);
        });
    }

    @Test
    public void resultInvertedIfStateFilterFeatureIsOn() {
        withMockUser(auth, "USER", () -> {
            List<StateSpec> filteredList = filterStatePermission();
            StateSpec s1 = filteredList.stream().filter(st -> "S1".equals(st.getKey())).findFirst().orElseThrow();
            assertThat(s1.getNext()).hasSize(1);
            assertThat(s1.getNext().get(0).getStateKey()).isEqualTo("S2");

            StateSpec s2 = filteredList.stream().filter(st -> "S2".equals(st.getKey())).findFirst().orElseThrow();
            assertThat(s2.getNext()).isEmpty();

            StateSpec s4 = filteredList.stream().filter(st -> "S4".equals(st.getKey())).findFirst().orElseThrow();
            assertThat(s4.getNext()).isEmpty();

            StateSpec s3 = filteredList.stream().filter(st -> "S3".equals(st.getKey())).findFirst().orElseThrow();
            assertThat(s3.getNext()).hasSize(2);

            Set<String> collect = s3.getNext().stream().map(NextSpec::getStateKey).collect(Collectors.toSet());
            assertThat(collect).isEqualTo(Set.of("S2","S4"));
        });
    }

    public void filterFunctionPermission(String... expected) {
        //setUp enabled feature
        final Map<String, Object> config = getMockedConfig(FunctionCustomPrivilegesExtractor.SECTION_NAME,
                                                           DYNAMIC_FUNCTION_PERMISSION_FEATURE, Boolean.TRUE);

        prepareConfig(config);

        when(permissionService.getPermissions(eq("XM"))).thenReturn(Map.of(
            XM_ENITITY_FUNCTION_CALL_PRIV + ".F1", createMockedPermission("USER", XM_ENITITY_FUNCTION_CALL_PRIV + ".F1", FunctionCustomPrivilegesExtractor.SECTION_NAME),
            FUNCTION_CALL_PRIV + ".F3", createMockedPermission("USER", FUNCTION_CALL_PRIV + ".F3", FunctionCustomPrivilegesExtractor.SECTION_NAME)
        ));


        TypeSpec result = dynamicPermissionCheckService.evaluateDynamicPermissions(demoTypeSpec());

        assertThat(result.getFunctions().stream().map(FunctionSpec::getKey))
                .containsExactly(expected);

    }

    public List<StateSpec> filterStatePermission() {
        //setUp enabled feature
        final Map<String, Object> config = getMockedConfig("entityStates",
            DYNAMIC_FUNCTION_PERMISSION_FEATURE, Boolean.TRUE);

        prepareConfig(config);

        when(permissionService.getPermissions(eq("XM"))).thenReturn(Map.of(
            EntityStateCustomPrivilegesExtractor.buildPermissionKey("DEMO", "S1", "S2"), createMockedPermission("USER", EntityStateCustomPrivilegesExtractor.buildPermissionKey("DEMO", "S1", "S2")),
            EntityStateCustomPrivilegesExtractor.buildPermissionKey("DEMO", "S3", "S2"), createMockedPermission("USER", EntityStateCustomPrivilegesExtractor.buildPermissionKey("DEMO", "S3", "S2")),
            EntityStateCustomPrivilegesExtractor.buildPermissionKey("DEMO", "S3", "S4"), createMockedPermission("USER", EntityStateCustomPrivilegesExtractor.buildPermissionKey("DEMO", "S3", "S4"))
        ));

        TypeSpec typeSpec = demoTypeSpec();
        TypeSpec result = dynamicPermissionCheckService.evaluateDynamicPermissions(typeSpec);

        assertThat(result.getStates()).hasSize(4);

        return result.getStates();
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

    private Map<String, Object> getMockedConfig(String configSectionName, String featureName, Boolean status) {
        Map<String, Object> map = newHashMap();
        Map<String, Object> section = newHashMap();
        section.put(featureName, status);
        map.put(configSectionName, section);
        return map;
    }

    @SneakyThrows
    public void prepareConfig(Map<String, Object> map) {
        tenantConfig.onRefresh("/config/tenants/XM/tenant-config.yml", new ObjectMapper().writeValueAsString(map));
    }

    private Permission createMockedPermission(String role, String permKey, boolean iDisabled, boolean isDeleted) {
        Permission p = new Permission();
        p.setRoleKey(role);
        p.setPrivilegeKey(permKey);
        p.setDeleted(isDeleted);
        p.setDisabled(iDisabled);
        p.setMsName(FunctionCustomPrivilegesExtractor.SECTION_NAME);
        return p;
    }

    private Permission createMockedPermission(String role, String permKey) {
        return createMockedPermission(role, permKey, false, false);
    }

    private Permission createMockedPermission(String role, String permKey, String msName) {
        Permission p =  createMockedPermission(role, permKey, false, false);
        p.setMsName(msName);
        return p;
    }

    private TypeSpec demoTypeSpec() {

        List<FunctionSpec> functions = newArrayList(
            createEntityFunction("F1"),
            createFunction("F2"),
            createFunction("F3")
        );

        List<StateSpec> stateSpecs = TypeSpecTestUtils.newStateSpecs(List.of("S1", "S2", "S3", "S4"), List.of("S2", "S4"), true);

        TypeSpec typeSpec = new TypeSpec();
        typeSpec.setKey("DEMO");
        typeSpec.setFunctions(functions);
        typeSpec.setStates(stateSpecs);

        return typeSpec;
    }

}
