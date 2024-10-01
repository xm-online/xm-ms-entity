package com.icthh.xm.ms.entity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.permission.domain.Role;
import com.icthh.xm.commons.permission.service.RoleService;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService.XmEntityTenantConfig;
import com.icthh.xm.ms.entity.config.tenant.LocalXmEntitySpecService;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.LinkSpec;
import com.icthh.xm.ms.entity.domain.spec.LocationSpec;
import com.icthh.xm.ms.entity.domain.spec.NextSpec;
import com.icthh.xm.ms.entity.domain.spec.RatingSpec;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import com.icthh.xm.ms.entity.domain.spec.TagSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.UniqueFieldSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.json.JsonListenerService;
import com.icthh.xm.ms.entity.service.privileges.custom.ApplicationCustomPrivilegesExtractor;
import com.icthh.xm.ms.entity.service.privileges.custom.EntityCustomPrivilegeService;
import com.icthh.xm.ms.entity.service.privileges.custom.FunctionCustomPrivilegesExtractor;
import com.icthh.xm.ms.entity.service.privileges.custom.FunctionWithXmEntityCustomPrivilegesExtractor;
import com.icthh.xm.ms.entity.service.processor.DefinitionSpecProcessor;
import com.icthh.xm.ms.entity.service.processor.FormSpecProcessor;
import com.icthh.xm.ms.entity.service.spec.XmEntitySpecCustomizer;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Maps.newHashMap;
import static com.icthh.xm.ms.entity.config.TenantConfigMockConfiguration.getXmEntitySpec;
import static com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService.CONFIG_SECTION;
import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.nullSafe;
import static com.icthh.xm.ms.entity.web.rest.XmEntitySaveIntTest.loadFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Slf4j
public class XmEntitySpecServiceUnitTest extends AbstractUnitTest {

    private static final String DYNAMIC_FUNCTION_PERMISSION_FEATURE = "dynamicPermissionCheckEnabled";

    private static final String TENANT = "TEST";

    private static final String SPEC_FOLDER_URL = "/config/tenants/{tenantName}/entity/specs/xmentityspecs";

    private static final String URL = SPEC_FOLDER_URL + ".yml";

    private static final String ROOT_KEY = "";

    private static final String KEY1 = "TYPE1";

    private static final String KEY2 = "TYPE2";

    private static final String KEY3 = "TYPE1.SUBTYPE1";

    private static final String KEY4 = "TYPE1.SUBTYPE1.SUBTYPE2";

    private static final String KEY5 = "TYPE1-OTHER";

    private static final String KEY6 = "TYPE3";
    private static final String KEY7 = "TYPE_WITH_NULL_IS_APP_AND_IS_ABSTRACT";

    private static final String PRIVILEGES_PATH = "/config/tenants/TEST/custom-privileges.yml";
    private static final String RELATIVE_FORMS_PATH_TO_FILE = "xmentityspec/forms/form-specification-int.json";
    private static final String RELATIVE_DEFINITIONS_PATH_TO_FILE = "xmentityspec/definitions/definition-specification-int.json";

    private static final String MAX_FILE_SIZE = "1Mb";

    private XmEntitySpecService xmEntitySpecService;

    private TenantContextHolder tenantContextHolder;

    @Mock
    private CommonConfigRepository commonConfigRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private XmEntitySpecCustomizer xmEntitySpecCustomizer;
    @Mock
    private TenantConfigRepository tenantConfigRepository;
    private JsonListenerService jsonListenerService = new JsonListenerService();
    @InjectMocks
    @Spy
    private DynamicPermissionCheckService dynamicPermissionCheckService;
    @Spy
    private XmEntityTenantConfigService tenantConfig = new XmEntityTenantConfigService(new XmConfigProperties(),
                                                                                       tenantContextHolder());

    private TenantContextHolder tenantContextHolder() {
        TenantContextHolder mock = mock(TenantContextHolder.class);
        when(mock.getTenantKey()).thenReturn("XM");
        return mock;
    }

    @Before
    @SneakyThrows
    public void init() {
        MockitoAnnotations.initMocks(this);

        tenantContextHolder = mock(TenantContextHolder.class);
        mockTenant(TENANT);

        ApplicationProperties ap = new ApplicationProperties();
        ap.setSpecificationPathPattern(URL);
        ap.setSpecificationFolderPathPattern(SPEC_FOLDER_URL + "/*");
        xmEntitySpecService = createXmEntitySpecService(ap, tenantContextHolder);
    }

    private XmEntitySpecService createXmEntitySpecService(ApplicationProperties applicationProperties,
                                                                TenantContextHolder tenantContextHolder) {

        return spy(new LocalXmEntitySpecService(tenantConfigRepository,
                                            applicationProperties,
                                            tenantContextHolder,
                                            new EntityCustomPrivilegeService(
                                                commonConfigRepository,
                                                asList(
                                                    new ApplicationCustomPrivilegesExtractor(),
                                                    new FunctionCustomPrivilegesExtractor(tenantConfig),
                                                    new FunctionWithXmEntityCustomPrivilegesExtractor(tenantConfig)
                                                ),
                                                tenantConfig
                                            ),
                                            dynamicPermissionCheckService,
                                            tenantConfig,
                                            xmEntitySpecCustomizer,
                                            new DefinitionSpecProcessor(jsonListenerService),
                                            new FormSpecProcessor(jsonListenerService), MAX_FILE_SIZE));
    }

    @Test
    public void testTypeSpecByKey() {
        TypeSpec type = xmEntitySpecService.getTypeSpecByKey(KEY1).orElse(null);
        assertNotNull(type);
        assertEquals(KEY1, type.getKey());

        type = xmEntitySpecService.getTypeSpecByKey(KEY3).orElse(null);
        assertNotNull(type);
        assertEquals(KEY3, type.getKey());

        Optional<TypeSpec> typeSpecByKey = xmEntitySpecService.getTypeSpecByKey(KEY4);
        assertThat(typeSpecByKey).isEmpty();
    }

    @Test
    public void testFindSpecByKey() {
        TypeSpec type = xmEntitySpecService.findTypeByKey(KEY1);
        assertNotNull(type);
        assertEquals(KEY1, type.getKey());

        type = xmEntitySpecService.findTypeByKey(KEY3);
        assertNotNull(type);
        assertEquals(KEY3, type.getKey());

        type = xmEntitySpecService.findTypeByKey(KEY4);
        assertThat(type).isNull();
    }

    @Test
    public void testFindAllTypes() {

        prepareConfig(newHashMap());
        List<TypeSpec> types = xmEntitySpecService.findAllTypes();
        assertNotNull(types);
        assertEquals(6, types.size());

        List<String> keys = types.stream().map(TypeSpec::getKey).collect(Collectors.toList());
        assertThat(keys).containsExactlyInAnyOrder(KEY1, KEY2, KEY3, KEY5, KEY6, KEY7);

        List<FunctionSpec> functions = flattenFunctions(types);

        assertThat(functions.size()).isEqualTo(10);

    }

    @Test
    public void testRefreshWithIgnoreList() {

        mockTenant("IGNORE-INHERITANCE");
        ApplicationProperties ap = new ApplicationProperties();
        ap.setSpecificationPathPattern(URL);
        xmEntitySpecService = createXmEntitySpecService(ap, tenantContextHolder);

        Map<String, TypeSpec> typeSpecs = xmEntitySpecService.getTypeSpecs();

        TypeSpec typeSpec = typeSpecs.get("ACCOUNT.ADMIN");
        assertEquals(typeSpec.getFunctions().size(), 2);
        assertTrue(typeSpec.getFunctions().stream().allMatch(f-> Set.of("C", "D").contains(f.getKey())));
        assertEquals(typeSpec.getTags().size(), 2);
        assertTrue(typeSpec.getTags().stream().allMatch(t-> Set.of("TEST2", "TEST3").contains(t.getKey())));
        assertEquals(typeSpec.getStates().size(), 4);
    }

    @SneakyThrows
    public void prepareConfig(Map<String, Object> map) {
        tenantConfig.onRefresh("/config/tenants/XM/tenant-config.yml", new ObjectMapper().writeValueAsString(map));
    }

    @Test
    public void testFindAllTypesWithFunctionFilterAndNoPrivilege() {
        prepareConfig(getMockedConfig(CONFIG_SECTION,
            DYNAMIC_FUNCTION_PERMISSION_FEATURE, Boolean.TRUE));

        List<TypeSpec> types = xmEntitySpecService.findAllTypes();
        assertNotNull(types);
        assertEquals(6, types.size());

        List<String> keys = types.stream().map(TypeSpec::getKey).collect(Collectors.toList());
        assertThat(keys).containsExactlyInAnyOrder(KEY1, KEY2, KEY3, KEY5, KEY6, KEY7);

        List<FunctionSpec> functions = flattenFunctions(types);

        assertThat(functions.size()).isEqualTo(0);
    }

    @Test
    public void testFindAllAppTypes() {
        prepareConfig(newHashMap());
        List<TypeSpec> types = xmEntitySpecService.findAllAppTypes();
        assertNotNull(types);
        assertEquals(3, types.size());

        List<String> keys = types.stream().map(TypeSpec::getKey).collect(Collectors.toList());
        assertThat(keys).containsExactlyInAnyOrder(KEY1, KEY2, KEY6);

        List<FunctionSpec> functions = flattenFunctions(types);
        assertThat(functions.size()).isEqualTo(6);
    }

    @Test
    public void testFindAllAppTypesWithFunctionFilterAndNoPrivilege() {
        prepareConfig(getMockedConfig(CONFIG_SECTION,
            DYNAMIC_FUNCTION_PERMISSION_FEATURE, Boolean.TRUE));
        List<TypeSpec> types = xmEntitySpecService.findAllAppTypes();
        assertNotNull(types);
        assertEquals(3, types.size());

        List<String> keys = types.stream().map(TypeSpec::getKey).collect(Collectors.toList());
        assertThat(keys).containsExactlyInAnyOrder(KEY1, KEY2, KEY6);

        List<FunctionSpec> functions = flattenFunctions(types);

        assertThat(functions.size()).isEqualTo(0);
    }

    @Test
    public void testFindAllNonAbstractTypes() {
        prepareConfig(newHashMap());
        List<TypeSpec> types = xmEntitySpecService.findAllNonAbstractTypes();
        assertNotNull(types);
        assertEquals(5, types.size());

        List<String> keys = types.stream().map(TypeSpec::getKey).collect(Collectors.toList());
        assertThat(keys).containsExactlyInAnyOrder(KEY2, KEY3, KEY5, KEY6, KEY7);

        List<FunctionSpec> functions = flattenFunctions(types);
        assertThat(functions.size()).isEqualTo(9);
    }

    @Test
    public void testFindAllNonAbstractTypesWithFunctionFilterAndNoPrivilege() {
        prepareConfig(getMockedConfig(CONFIG_SECTION,
            DYNAMIC_FUNCTION_PERMISSION_FEATURE, Boolean.TRUE));

        List<TypeSpec> types = xmEntitySpecService.findAllNonAbstractTypes();
        assertNotNull(types);
        assertEquals(5, types.size());

        List<String> keys = types.stream().map(TypeSpec::getKey).collect(Collectors.toList());
        assertThat(keys).containsExactlyInAnyOrder(KEY2, KEY3, KEY5, KEY6, KEY7);

        List<FunctionSpec> functions = flattenFunctions(types);
        assertThat(functions.size()).isEqualTo(0);
    }

    @Test
    public void testFindNonAbstractTypesByPrefix() {
        List<TypeSpec> types = xmEntitySpecService.findNonAbstractTypesByPrefix(KEY1);
        assertNotNull(types);
        assertEquals(1, types.size());

        List<String> keys = types.stream().map(TypeSpec::getKey).collect(Collectors.toList());
        assertThat(keys).containsExactlyInAnyOrder(KEY3);
    }

    @Test
    public void testFindNonAbstractTypesByRootPrefix() {
        List<TypeSpec> types = xmEntitySpecService.findNonAbstractTypesByPrefix(ROOT_KEY);
        assertNotNull(types);
        assertEquals(5, types.size());

        List<String> keys = types.stream().map(TypeSpec::getKey).collect(Collectors.toList());
        assertThat(keys).containsExactlyInAnyOrder(KEY2, KEY3, KEY5, KEY6, KEY7);
    }

    @Test
    public void testFindNonAbstractTypesByPrefixEqualKey() {
        List<TypeSpec> types = xmEntitySpecService.findNonAbstractTypesByPrefix(KEY2);
        assertNotNull(types);
        assertEquals(1, types.size());

        List<String> keys = types.stream().map(TypeSpec::getKey).collect(Collectors.toList());
        assertThat(keys).containsExactlyInAnyOrder(KEY2);
    }

    @Test
    public void testFindAttachment() {
        Optional<AttachmentSpec> attachment = xmEntitySpecService.findAttachment(KEY3, "PDF");
        assertTrue(attachment.isPresent());
        assertEquals("PDF", attachment.get().getKey());
    }

    @Test
    public void testFindLink() {
        Optional<LinkSpec> link = xmEntitySpecService.findLink(KEY3, "LINK1");
        assertTrue(link.isPresent());
        assertEquals("LINK1", link.get().getKey());
    }

    @Test
    public void testFindLocation() {
        Optional<LocationSpec> location = xmEntitySpecService.findLocation(KEY2, "LOCATION1");
        assertTrue(location.isPresent());
        assertEquals("LOCATION1", location.get().getKey());
    }

    @Test
    public void testFindRating() {
        Optional<RatingSpec> rating = xmEntitySpecService.findRating(KEY3, "RATING1");
        assertTrue(rating.isPresent());
        assertEquals("RATING1", rating.get().getKey());
    }

    @Test
    public void testFindState() {
        Optional<StateSpec> state = xmEntitySpecService.findState(KEY3, "STATE2");
        assertTrue(state.isPresent());
        assertEquals("STATE2", state.get().getKey());
    }

    @Test
    public void testNext() {
        List<NextSpec> states = xmEntitySpecService.next(KEY3, "STATE2");
        assertNotNull(states);
        assertEquals(2, states.size());
        List<String> keys = states.stream().map(NextSpec::getStateKey).collect(Collectors.toList());
        assertThat(keys).containsExactlyInAnyOrder("STATE1", "STATE3");
    }

    @Test
    public void testNextStates() {
        List<StateSpec> states = xmEntitySpecService.nextStates(KEY3, "STATE2");
        assertNotNull(states);
        assertEquals(2, states.size());
        List<String> keys = states.stream().map(StateSpec::getKey).collect(Collectors.toList());
        assertThat(keys).containsExactlyInAnyOrder("STATE1", "STATE3");
    }

    @Test
    public void testFindTag() {
        Optional<TagSpec> tag = xmEntitySpecService.findTag(KEY3, "TAG1");
        assertTrue(tag.isPresent());
        assertEquals("TAG1", tag.get().getKey());
    }

    @Test
    public void testGetAllKeys() {
        Map<String, Map<String, Set<String>>> keys = xmEntitySpecService.getAllKeys();
        assertNotNull(keys);
        assertEquals(6, keys.size());
        assertEquals(1, keys.get("TYPE1").get("LinkSpec").size());
        assertEquals(3, keys.get("TYPE1").get("StateSpec").size());
    }

    @Test
    public void testUniqueField() {
        mockTenant("RESINTTEST");
        ApplicationProperties ap = new ApplicationProperties();
        ap.setSpecificationPathPattern(URL);
        xmEntitySpecService = createXmEntitySpecService(ap, tenantContextHolder);

        for(TypeSpec typeSpec: xmEntitySpecService.getTypeSpecs().values()) {
            if (typeSpec.getKey().equals("TEST_UNIQUE_FIELD")) {
                continue;
            }
            if (typeSpec.getKey().equals("TEST_UNIQ_FIELDS")) {
                assertEquals(typeSpec.getUniqueFields().size(), 4);
                assertTrue(typeSpec.getUniqueFields().contains(new UniqueFieldSpec("$.uniqField")));
                assertTrue(typeSpec.getUniqueFields().contains(new UniqueFieldSpec("$.notUniqObject.uniqueField")));
                assertTrue(typeSpec.getUniqueFields().contains(new UniqueFieldSpec("$.uniqObject")));
                assertTrue(typeSpec.getUniqueFields().contains(new UniqueFieldSpec("$.uniqObject.uniqueField")));
            } else {
                assertEquals(typeSpec.getUniqueFields().size(), 0);
            }
        }
    }

    public void mockTenant(String resinttest) {
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(resinttest)));
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(tenantContextHolder.getTenantKey()).thenReturn(resinttest);
    }

    private void enableDynamicPermissionCheck() {
        XmEntityTenantConfig config = new XmEntityTenantConfig();
        when(tenantConfig.getXmEntityTenantConfig("TEST")).thenReturn(config);
        config.getEntityFunctions().setDynamicPermissionCheckEnabled(true);
    }

    @Test
    @SneakyThrows
    public void testUpdateCustomerPrivileges() {
        String customPrivileges = readFile("config/privileges/custom-privileges.yml");
        String expectedCustomPrivileges = readFile("config/privileges/expected-custom-privileges.yml");

        testUpdateCustomerPrivileges(customPrivileges, expectedCustomPrivileges);
    }

    @Test
    @SneakyThrows
    public void testUpdateCustomerPrivilegesWithEntity() {
        enableDynamicPermissionCheck();

        String customPrivileges = readFile("config/privileges/custom-privileges-with-function.yml");
        String expectedCustomPrivileges = readFile("config/privileges/expected-custom-privileges-with-function.yml");

        testUpdateCustomerPrivileges(customPrivileges, expectedCustomPrivileges);
    }

    @Test
    @SneakyThrows
    public void testDisableCustomPrivilegesGeneration() {
        disableDynamicPrivilegesGeneration();
        when(roleService.getRoles("TEST")).thenReturn(of("TEST_ROLE", new Role()));
        xmEntitySpecService.getTypeSpecs();
        verify(xmEntitySpecService).refreshFinished(List.of("/config/tenants/TEST/entity/specs/xmentityspecs.yml"));
        verifyNoMoreInteractions(commonConfigRepository);
    }

    private void disableDynamicPrivilegesGeneration() {
        XmEntityTenantConfig config = new XmEntityTenantConfig();
        config.setDisableDynamicPrivilegesGeneration(true);
        when(tenantConfig.getXmEntityTenantConfig("TEST")).thenReturn(config);
    }

    private void testUpdateCustomerPrivileges(String customPrivileges, String expectedCustomPrivileges) {
        String privilegesPath = PRIVILEGES_PATH;
        Map<String, Configuration> configs = of(
            privilegesPath, new Configuration(privilegesPath, customPrivileges)
                                               );
        when(commonConfigRepository.getConfig(isNull(), eq(List.of(privilegesPath)))).thenReturn(configs);
        when(roleService.getRoles("TEST")).thenReturn(of("TEST_ROLE", new Role()));

        xmEntitySpecService.getTypeSpecs();

        verify(commonConfigRepository).getConfig(isNull(), eq(List.of(privilegesPath)));

        ArgumentCaptor<Configuration> captor = ArgumentCaptor.forClass(Configuration.class);
        verify(commonConfigRepository).updateConfigFullPath(captor.capture(), eq(sha1Hex(customPrivileges)));
        assertEquals(privilegesPath, captor.getValue().getPath());
        assertEquals(expectedCustomPrivileges, captor.getValue().getContent());

        verifyNoMoreInteractions(commonConfigRepository);
    }

    @Test
    @SneakyThrows
    public void testCreateCustomPrivileges() {
        String privileges = readFile("config/privileges/new-privileges.yml");

        testCreateCustomPrivileges(privileges);
    }

    @Test
    @SneakyThrows
    public void testCreateCustomerPrivilegesWithFunctions() {
        enableDynamicPermissionCheck();

        String privileges = readFile("config/privileges/new-privileges-with-functions.yml");

        testCreateCustomPrivileges(privileges);
    }

    private void testCreateCustomPrivileges(String privileges) {
        String privilegesPath = PRIVILEGES_PATH;
        when(commonConfigRepository.getConfig(isNull(), eq(List.of(privilegesPath)))).thenReturn(null);
        when(roleService.getRoles("TEST")).thenReturn(of("ROLE_ADMIN", new Role(), "ROLE_AGENT", new Role()));

        xmEntitySpecService.getTypeSpecs();

        verify(commonConfigRepository).getConfig(isNull(), eq(List.of(privilegesPath)));

        ArgumentCaptor<Configuration> captor = ArgumentCaptor.forClass(Configuration.class);
        verify(commonConfigRepository).updateConfigFullPath(captor.capture(), isNull());
        assertEquals(privilegesPath, captor.getValue().getPath());
        assertEquals(privileges, captor.getValue().getContent());

        verifyNoMoreInteractions(commonConfigRepository);
    }

    @Test
    @SneakyThrows
    public void testUpdateRealPermissionFile() {
        String privileges = readFile("config/privileges/new-privileges.yml");

        testUpdateRealPermissionFile(privileges);
    }

    @Test
    @SneakyThrows
    public void testUpdateRealPermissionFileWithXmEntity() {
        enableDynamicPermissionCheck();

        String privileges = readFile("config/privileges/new-privileges-with-functions.yml");

        testUpdateRealPermissionFile(privileges);
    }

    public void testUpdateRealPermissionFile(String privileges) {
        String privilegesPath = PRIVILEGES_PATH;
        Map<String, Configuration> configs = of();
        when(commonConfigRepository.getConfig(isNull(), eq(List.of(privilegesPath)))).thenReturn(configs);
        when(roleService.getRoles("TEST")).thenReturn(of(
            "ROLE_ADMIN", new Role(),
            "ROLE_AGENT", new Role()
        ));

        xmEntitySpecService.getTypeSpecs();

        verify(commonConfigRepository).getConfig(isNull(), eq(List.of(privilegesPath)));
        verify(commonConfigRepository).updateConfigFullPath(refEq(new Configuration(privilegesPath, privileges)), isNull());
        verifyNoMoreInteractions(commonConfigRepository);
    }

    @Test
    public void testExtendsWithSeparateFiles() {
        mockTenant("RESINTTEST");
        String config = getXmEntitySpec("resinttest-part-2");
        String key = SPEC_FOLDER_URL.replace("{tenantName}", "RESINTTEST") + "/file.yml";
        xmEntitySpecService.onRefresh(key, config);
        // refresh multiple times (important)
        xmEntitySpecService.getTypeSpecs();
        xmEntitySpecService.getTypeSpecs();
        var typeSpecs = xmEntitySpecService.getTypeSpecs();

        TypeSpec extendedEntity = typeSpecs.get("BASE_ENTITY.EXTENDS_ENABLED");
        assertEquals(extendedEntity.getFunctions().size(), 1);
        TypeSpec extendedEntityFromSeparateFile = typeSpecs.get("BASE_ENTITY.SEPARATE_FILE_EXTENDS_ENABLED");
        assertEquals(extendedEntityFromSeparateFile.getFunctions().size(), 1);
    }

    @Test
    public void testExtendsDataSpecWithSeparateFiles() {
        String config = getXmEntitySpec("resinttest-part-2");
        String key = SPEC_FOLDER_URL.replace("{tenantName}", "RESINTTEST") + "/file.yml";
        xmEntitySpecService.onRefresh(key, config);

        testExtendsDataSpec();

        var bothEntityData = Map.of(
                "field1", "field1value",
                "field5", "field4value",
                "object1", Map.of(
                        "field2", "field2value",
                        "field6", "field6value"
                )
        );

        setExtendsFormAndSpec(false);
        var typeSpecs = xmEntitySpecService.getTypeSpecs();
        TypeSpec extendedEntity = typeSpecs.get("BASE_ENTITY.SEPARATE_FILE_EXTENDS_ENABLED");
        assertTrue(validateByJsonSchema(extendedEntity.getDataSpec(), bothEntityData));

        setExtendsFormAndSpec(true);
        TypeSpec extendedEntityWithGlobalExtends = typeSpecs.get("BASE_ENTITY.SEPARATE_FILE_EXTENDS_ENABLED");
        assertTrue(validateByJsonSchema(extendedEntityWithGlobalExtends.getDataSpec(), bothEntityData));
    }

    @Test
    public void testExtendsDataSpec() {

        mockTenant("RESINTTEST");

        var baseEntityData = Map.of(
                "field1", "field1value",
                "object1", Map.of(
                        "field2", "field2value"
                )
        );
        var extendsEntityData = Map.of(
                "field3", "field3value",
                "object1", Map.of(
                        "field4", "field4value"
                )
        );
        var bothEntityData = Map.of(
                "field1", "field1value",
                "field3", "field3value",
                "object1", Map.of(
                        "field2", "field2value",
                        "field4", "field4value"
                )
        );

        var typeSpecs = xmEntitySpecService.getTypeSpecs();
        TypeSpec baseEntity = typeSpecs.get("BASE_ENTITY");
        TypeSpec extendedEntity = typeSpecs.get("BASE_ENTITY.EXTENDS");
        assertTrue(validateByJsonSchema(baseEntity.getDataSpec(), baseEntityData));
        assertTrue(validateByJsonSchema(extendedEntity.getDataSpec(), extendsEntityData));
        assertFalse(validateByJsonSchema(baseEntity.getDataSpec(), extendsEntityData));
        assertFalse(validateByJsonSchema(extendedEntity.getDataSpec(), baseEntityData));
        assertFalse(validateByJsonSchema(extendedEntity.getDataSpec(), bothEntityData));

        TypeSpec extendedEntityWithEnabledExtends = typeSpecs.get("BASE_ENTITY.EXTENDS_ENABLED");
        assertTrue(validateByJsonSchema(extendedEntityWithEnabledExtends.getDataSpec(), extendsEntityData));
        assertTrue(validateByJsonSchema(extendedEntityWithEnabledExtends.getDataSpec(), bothEntityData));

        setExtendsFormAndSpec(true);

        var typeSpecsWithExtends = xmEntitySpecService.getTypeSpecs();
        TypeSpec baseEntityWithExtends = typeSpecsWithExtends.get("BASE_ENTITY");
        TypeSpec extendedEntityWithExtends = typeSpecsWithExtends.get("BASE_ENTITY.EXTENDS");
        assertTrue(validateByJsonSchema(baseEntityWithExtends.getDataSpec(), baseEntityData));
        assertTrue(validateByJsonSchema(extendedEntityWithExtends.getDataSpec(), extendsEntityData));
        assertFalse(validateByJsonSchema(baseEntity.getDataSpec(), extendsEntityData));
        assertTrue(validateByJsonSchema(extendedEntityWithExtends.getDataSpec(), baseEntityData));
        assertTrue(validateByJsonSchema(extendedEntityWithExtends.getDataSpec(), bothEntityData));
    }

    @Test
    public void testExtendsDataForm() {

        mockTenant("RESINTTEST");

        var baseDataFrom = Map.of(
                "form", List.of(
                Map.of("key", "field1"),
                Map.of("key", "object1.field2")
            )
        );

        var extendsDataFrom = Map.of(
                "form", List.of(
                        Map.of("key", "field3"),
                        Map.of("key", "object1.field4")
                )
        );

        var bothDataFrom = Map.of(
                "form", List.of(
                        Map.of("key", "field1"),
                        Map.of("key", "object1.field2"),
                        Map.of("key", "field3"),
                        Map.of("key", "object1.field4")
                )
        );

        var typeSpecs = xmEntitySpecService.getTypeSpecs();
        TypeSpec baseEntity = typeSpecs.get("BASE_ENTITY");
        TypeSpec extendedEntity = typeSpecs.get("BASE_ENTITY.EXTENDS");
        TypeSpec extendedEntityWithEnabledExtends = typeSpecs.get("BASE_ENTITY.EXTENDS_ENABLED");
        assertEqualsJson(baseDataFrom, baseEntity.getDataForm());
        assertEqualsJson(extendsDataFrom, extendedEntity.getDataForm());
        assertEqualsJson(bothDataFrom, extendedEntityWithEnabledExtends.getDataForm());

        setExtendsFormAndSpec(true);

        var typeSpecsWithExtends = xmEntitySpecService.getTypeSpecs();
        TypeSpec baseEntityWithExtends = typeSpecsWithExtends.get("BASE_ENTITY");
        TypeSpec extendedEntityWithExtends = typeSpecsWithExtends.get("BASE_ENTITY.EXTENDS");
        assertEqualsJson(baseDataFrom, baseEntityWithExtends.getDataForm());
        assertEqualsJson(bothDataFrom, extendedEntityWithExtends.getDataForm());
    }

    @Test
    public void testFindFunctionByKey() {
        // init typespecs
        xmEntitySpecService.getTypeSpecs();
        FunctionSpec functionSpec = xmEntitySpecService.findFunction("FUNCTION1", "POST").orElse(null);
        assertNotNull(functionSpec);
        assertEquals(functionSpec.getKey(), "FUNCTION1");
        assertNull(functionSpec.getPath());

        functionSpec = xmEntitySpecService.findFunction("FUNCTION3", "POST").orElse(null);
        assertNotNull(functionSpec);
        assertEquals(functionSpec.getKey(), "FUNCTION3");
        assertEquals(functionSpec.getPath(), "call/function/by-path/{id}");

        functionSpec = xmEntitySpecService.findFunction("in/package/FUNCTION4", "POST").orElse(null);
        assertNotNull(functionSpec);
        assertEquals(functionSpec.getKey(), "in/package/FUNCTION4");
        assertEquals(functionSpec.getPath(), "call/function/by-path/{id}/and/param/{param}");

        functionSpec = xmEntitySpecService.findFunction("FUNCTION_NOT_FOUND", "POST").orElse(null);
        assertNull(functionSpec);
    }

    @Test
    public void testFindFunctionByPath() {
        // init typespecs
        xmEntitySpecService.getTypeSpecs();
        FunctionSpec functionSpec = xmEntitySpecService.findFunction("call/function/by-path/111", "POST").orElse(null);
        assertNotNull(functionSpec);
        assertEquals(functionSpec.getKey(), "FUNCTION3");
        assertEquals(functionSpec.getPath(), "call/function/by-path/{id}");

        functionSpec = xmEntitySpecService.findFunction("call/function/by-path/D42/and/param/my-param-value", "POST").orElse(null);
        assertNotNull(functionSpec);
        assertEquals(functionSpec.getKey(), "in/package/FUNCTION4");
        assertEquals(functionSpec.getPath(), "call/function/by-path/{id}/and/param/{param}");

        functionSpec = xmEntitySpecService.findFunction("call/function/by-path/D42/filtered/by/methods/my-param-value", "POST").orElse(null);
        assertNotNull(functionSpec);
        assertEquals(functionSpec.getKey(), "in/package/FUNCTION5");
        assertEquals(functionSpec.getPath(), "call/function/by-path/{id}/filtered/by/methods/{param}");

        functionSpec = xmEntitySpecService.findFunction("call/function/by-path/D42/filtered/by/methods/my-param-value", "GET").orElse(null);
        assertNotNull(functionSpec);
        assertEquals(functionSpec.getKey(), "in/package/FUNCTION5");
        assertEquals(functionSpec.getPath(), "call/function/by-path/{id}/filtered/by/methods/{param}");

        functionSpec = xmEntitySpecService.findFunction("call/function/by-path/D42/filtered/by/methods/my-param-value", "POST_URLENCODED").orElse(null);
        assertNotNull(functionSpec);
        assertEquals(functionSpec.getKey(), "in/package/FUNCTION6");
        assertEquals(functionSpec.getPath(), "call/function/by-path/{id}/filtered/by/methods/{param}");

        functionSpec = xmEntitySpecService.findFunction("call/function/by-path/D42/filtered/by/methods/my-param-value", "PUT").orElse(null);
        assertNull(functionSpec);
    }

    @Test
    public void testFindFunctionByMostSpecificPath() {
        // init typespecs
        xmEntitySpecService.getTypeSpecs();
        FunctionSpec functionSpec = xmEntitySpecService.findFunction("v1/billing-cycles/123/action", "POST").orElse(null);
        assertNotNull(functionSpec);
        assertEquals(functionSpec.getKey(), "v1/billingcycles/manage");
        assertEquals(functionSpec.getPath(), "v1/billing-cycles/{billingCycleId}/{actionKey}");

        functionSpec = xmEntitySpecService.findFunction("v1/billing-cycles/325/products", "POST").orElse(null);
        assertNotNull(functionSpec);
        assertEquals(functionSpec.getKey(), "v1/billingcycles/products");
        assertEquals(functionSpec.getPath(), "v1/billing-cycles/{billingCycleId}/products");
    }

    @Test
    public void testUpdateTenantByStateWithDefinitionsAndForms() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        String config = getXmEntitySpec("specifications");
        String key = SPEC_FOLDER_URL.replace("{tenantName}", "RESINTTEST") + "/file.yml";
        mockTenant("RESINTTEST");
        jsonListenerService.processTenantSpecification("RESINTTEST", RELATIVE_FORMS_PATH_TO_FILE, loadFile("config/specs/form-specification-int.json"));
        jsonListenerService.processTenantSpecification("RESINTTEST", RELATIVE_DEFINITIONS_PATH_TO_FILE, loadFile("config/specs/definition-specification-int.json"));
        xmEntitySpecService.onRefresh(key, config);

        TypeSpec actualTypeSpec = xmEntitySpecService.getTypeSpecs().get("DEMO.TEST");
        FunctionSpec actualFunctionSpec = actualTypeSpec.getFunctions().get(0);
        NextSpec actualNextSpec = actualTypeSpec.getStates().get(0).getNext().get(0);

        XmEntitySpec expectedXmEntity = objectMapper.readValue(getXmEntitySpec("specifications-expected"), XmEntitySpec.class);
        TypeSpec expectedTypeSpec = expectedXmEntity.getTypes().get(0);
        FunctionSpec expectedFunctionSpec = expectedTypeSpec.getFunctions().get(0);
        NextSpec expectedNextSpec = expectedTypeSpec.getStates().get(0).getNext().get(0);

        assertEqualsTypeSpecFields(actualTypeSpec.getDataSpec(), expectedTypeSpec.getDataSpec());
        assertEqualsTypeSpecFields(actualTypeSpec.getDataForm(), expectedTypeSpec.getDataForm());
        assertEqualsTypeSpecFields(actualFunctionSpec.getInputSpec(), expectedFunctionSpec.getInputSpec());
        assertEqualsTypeSpecFields(actualFunctionSpec.getInputForm(), expectedFunctionSpec.getInputForm());
        assertEqualsTypeSpecFields(actualFunctionSpec.getContextDataSpec(), expectedFunctionSpec.getContextDataSpec());
        assertEqualsTypeSpecFields(actualFunctionSpec.getContextDataForm(), expectedFunctionSpec.getContextDataForm());
        assertEqualsTypeSpecFields(actualNextSpec.getInputSpec(), expectedNextSpec.getInputSpec());
        assertEqualsTypeSpecFields(actualNextSpec.getInputForm(), expectedNextSpec.getInputForm());
    }

    @Test
    public void testUpdateTenantByStateWithDefinitionsAndFormsAndUpdatedJsonFile() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        String config = getXmEntitySpec("specifications");
        String key = SPEC_FOLDER_URL.replace("{tenantName}", "RESINTTEST") + "/file.yml";
        mockTenant("RESINTTEST");

        jsonListenerService.processTenantSpecification("RESINTTEST", RELATIVE_FORMS_PATH_TO_FILE, loadFile("config/specs/form-specification-int.json"));
        jsonListenerService.processTenantSpecification("RESINTTEST", RELATIVE_DEFINITIONS_PATH_TO_FILE, loadFile("config/specs/definition-specification-int.json"));

        xmEntitySpecService.onRefresh(key, config);

        jsonListenerService.processTenantSpecification("RESINTTEST", RELATIVE_FORMS_PATH_TO_FILE, loadFile("config/specs/updated-form-specification-int.json"));
        jsonListenerService.processTenantSpecification("RESINTTEST", RELATIVE_DEFINITIONS_PATH_TO_FILE, loadFile("config/specs/updated-definition-specification-int.json"));

        xmEntitySpecService.onRefresh(key, config);

        TypeSpec actualTypeSpec = xmEntitySpecService.getTypeSpecs().get("DEMO.TEST");
        FunctionSpec actualFunctionSpec = actualTypeSpec.getFunctions().get(0);
        NextSpec actualNextSpec = actualTypeSpec.getStates().get(0).getNext().get(0);

        XmEntitySpec expectedXmEntity = objectMapper.readValue(getXmEntitySpec("specifications-updated-expected"), XmEntitySpec.class);
        TypeSpec expectedTypeSpec = expectedXmEntity.getTypes().get(0);
        FunctionSpec expectedFunctionSpec = expectedTypeSpec.getFunctions().get(0);
        NextSpec expectedNextSpec = expectedTypeSpec.getStates().get(0).getNext().get(0);

        assertEqualsTypeSpecFields(actualTypeSpec.getDataSpec(), expectedTypeSpec.getDataSpec());
        assertEqualsTypeSpecFields(actualTypeSpec.getDataForm(), expectedTypeSpec.getDataForm());
        assertEqualsTypeSpecFields(actualFunctionSpec.getInputSpec(), expectedFunctionSpec.getInputSpec());
        assertEqualsTypeSpecFields(actualFunctionSpec.getInputForm(), expectedFunctionSpec.getInputForm());
        assertEqualsTypeSpecFields(actualFunctionSpec.getContextDataSpec(), expectedFunctionSpec.getContextDataSpec());
        assertEqualsTypeSpecFields(actualFunctionSpec.getContextDataForm(), expectedFunctionSpec.getContextDataForm());
        assertEqualsTypeSpecFields(actualNextSpec.getInputSpec(), expectedNextSpec.getInputSpec());
        assertEqualsTypeSpecFields(actualNextSpec.getInputForm(), expectedNextSpec.getInputForm());
    }

    @SneakyThrows
    private void assertEqualsTypeSpecFields(String expectedField, String actualField) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map expected = objectMapper.readValue(expectedField, Map.class);
        Map actual = objectMapper.readValue(actualField, Map.class);

        assertEquals(actual, expected);
    }

    @SneakyThrows
    private void assertEqualsJson(Map<String, ?> expected, String actual) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedTree = objectMapper.readTree(objectMapper.writeValueAsString(expected));
        JsonNode actualTree = objectMapper.readTree(actual);
        assertEquals(expectedTree, actualTree);
    }

    @SneakyThrows
    private boolean validateByJsonSchema(String schema, Map<String, Object> value) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode valueJsonNode = objectMapper.readTree(objectMapper.writeValueAsString(value));
        JsonNode schemaNode = new ObjectMapper().readTree(schema);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        Set<ValidationMessage> report = jsonSchema.validate(valueJsonNode);
        log.info("Validation: {}", report.toString());
        return report.isEmpty();
    }

    private void setExtendsFormAndSpec(Boolean value) {
        XmEntityTenantConfig config = new XmEntityTenantConfig();
        config.getEntitySpec().setEnableDataFromInheritance(value);
        config.getEntitySpec().setEnableDataSpecInheritance(value);
        when(tenantConfig.getXmEntityTenantConfig("RESINTTEST")).thenReturn(config);
    }

    private String readFile(String path1) throws IOException {
        InputStream cfgInputStream = new ClassPathResource(path1).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    private Map<String, Object> getMockedConfig(String configSectionName, String featureName, Boolean status) {
        Map<String, Object> map = newHashMap();
        Map<String, Object> section = newHashMap();
        section.put(featureName, status);
        map.put(configSectionName, section);
        return map;
    }

    private List<FunctionSpec> flattenFunctions(List<TypeSpec> types) {
        return types.stream()
            .map(type -> nullSafe(type.getFunctions()) )
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }
}
