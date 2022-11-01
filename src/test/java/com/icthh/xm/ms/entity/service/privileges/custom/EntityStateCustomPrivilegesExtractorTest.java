package com.icthh.xm.ms.entity.service.privileges.custom;

import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.util.TypeSpecTestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EntityStateCustomPrivilegesExtractorTest extends AbstractUnitTest {

    EntityStateCustomPrivilegesExtractor entityStateCustomPrivilegesExtractor;

    @Mock
    XmEntityTenantConfigService xmEntityTenantConfigService;

    @Before
    public void setUp() {
        entityStateCustomPrivilegesExtractor = new EntityStateCustomPrivilegesExtractor(xmEntityTenantConfigService);
    }

    @Test
    public void getSectionName() {
        assertThat(entityStateCustomPrivilegesExtractor.getSectionName()).isEqualTo(EntityStateCustomPrivilegesExtractor.SECTION_NAME);
    }

    @Test
    public void getPrivilegePrefix() {
        assertThat(entityStateCustomPrivilegesExtractor.getPrivilegePrefix()).isEqualTo(EntityStateCustomPrivilegesExtractor.PRIVILEGE_PREFIX);
    }

    @Test
    public void entityStateCustomPrivilegesExtractorIsEnable() {
        XmEntityTenantConfigService.XmEntityTenantConfig c = new XmEntityTenantConfigService.XmEntityTenantConfig();
        XmEntityTenantConfigService.XmEntityTenantConfig.EntityFunctions f = new XmEntityTenantConfigService.XmEntityTenantConfig.EntityFunctions();
        f.setDynamicPermissionCheckEnabled(Boolean.TRUE);
        c.setEntityStates(f);
        when(xmEntityTenantConfigService.getXmEntityTenantConfig("TEST")).thenReturn(c);
        boolean test = entityStateCustomPrivilegesExtractor.isEnabled("TEST");
        assertThat(test).isTrue();
    }

    @Test
    public void entityStateCustomPrivilegesExtractorIsDisable() {
        XmEntityTenantConfigService.XmEntityTenantConfig c = new XmEntityTenantConfigService.XmEntityTenantConfig();
        when(xmEntityTenantConfigService.getXmEntityTenantConfig("TEST")).thenReturn(c);
        boolean test = entityStateCustomPrivilegesExtractor.isEnabled("TEST");
        assertThat(test).isFalse();
    }

    @Test
    public void toPrivilegesListReturnEmptyForNull() {
        Map<String, TypeSpec> cfg = null;
        List<String> strings = entityStateCustomPrivilegesExtractor.toPrivilegesList(cfg);
        assertThat(strings).isEqualTo(List.of());
    }

    @Test
    public void toPrivilegesListReturnListOfPermissions() {
        Map<String, TypeSpec> cfg = Map.of(
            "T2", newSpec("T2", List.of("S3", "S2", "S1"), List.of("S3", "S2"), false),
            "T1", newSpec("T1", List.of("S2", "S1"), List.of("S2"), true),
            "T0", newSpec("T0", List.of("S1"), List.of(), false)
        );
        List<String> strings = entityStateCustomPrivilegesExtractor.toPrivilegesList(cfg);
        assertThat(strings).isEqualTo(List.of(
            "XMENTITY.STATE.T1.S1.S2",
            "XMENTITY.STATE.T2.S1.S2",
            "XMENTITY.STATE.T2.S1.S3",
            "XMENTITY.STATE.T2.S2.S2",
            "XMENTITY.STATE.T2.S2.S3",
            "XMENTITY.STATE.T2.S3.S2",
            "XMENTITY.STATE.T2.S3.S3"));
    }

    private TypeSpec newSpec(String typeKey, List<String> states, List<String> nextStates, boolean filterSelf) {
        TypeSpec spec = new TypeSpec();
        spec.setKey(typeKey);

        List<StateSpec> stateSpecs = TypeSpecTestUtils.newStateSpecs(states, nextStates, filterSelf);

        spec.setStates(stateSpecs);
        return spec;
    }

}
