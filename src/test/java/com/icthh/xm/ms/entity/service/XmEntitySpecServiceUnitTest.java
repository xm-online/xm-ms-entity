package com.icthh.xm.ms.entity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.tenant.LocalXmEntitySpecService;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.domain.spec.LinkSpec;
import com.icthh.xm.ms.entity.domain.spec.LocationSpec;
import com.icthh.xm.ms.entity.domain.spec.NextSpec;
import com.icthh.xm.ms.entity.domain.spec.RatingSpec;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import com.icthh.xm.ms.entity.domain.spec.TagSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.UniqueFieldSpec;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class XmEntitySpecServiceUnitTest {

    private static final String TENANT = "TEST";

    private static final String URL = "/config/tenants/{tenantName}/entity/specs/xmentityspecs.yml";

    private static final String ROOT_KEY = "";

    private static final String KEY1 = "TYPE1";

    private static final String KEY2 = "TYPE2";

    private static final String KEY3 = "TYPE1.SUBTYPE1";

    private static final String KEY4 = "TYPE1.SUBTYPE1.SUBTYPE2";

    private static final String KEY5 = "TYPE1-OTHER";

    private static final String KEY6 = "TYPE3";

    private XmEntitySpecService xmEntitySpecService;

    private TenantContextHolder tenantContextHolder;


    @Before
    @SneakyThrows
    public void init() {
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(TENANT)));

        tenantContextHolder = mock(TenantContextHolder.class);
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);

        ApplicationProperties ap = new ApplicationProperties();
        ap.setSpecificationPathPattern(URL);
        xmEntitySpecService = createXmEntitySpecService(ap, tenantContextHolder);
    }

    private static XmEntitySpecService createXmEntitySpecService(ApplicationProperties applicationProperties,
                                                                TenantContextHolder tenantContextHolder) {
        return new LocalXmEntitySpecService(mock(TenantConfigRepository.class),
                                            applicationProperties,
                                            tenantContextHolder);
    }

    @Test
    public void testFindTypeByKey() {
        TypeSpec type = xmEntitySpecService.findTypeByKey(KEY1);
        assertNotNull(type);
        assertEquals(KEY1, type.getKey());

        type = xmEntitySpecService.findTypeByKey(KEY3);
        assertNotNull(type);
        assertEquals(KEY3, type.getKey());

        type = xmEntitySpecService.findTypeByKey(KEY4);
        assertNull(type);
    }

    @Test
    public void testFindAllTypes() {
        List<TypeSpec> types = xmEntitySpecService.findAllTypes();
        assertNotNull(types);
        assertEquals(5, types.size());

        List<String> keys = types.stream().map(TypeSpec::getKey).collect(Collectors.toList());
        assertThat(keys).containsExactlyInAnyOrder(KEY1, KEY2, KEY3, KEY5, KEY6);
    }

    @Test
    public void testFindAllAppTypes() {
        List<TypeSpec> types = xmEntitySpecService.findAllAppTypes();
        assertNotNull(types);
        assertEquals(3, types.size());

        List<String> keys = types.stream().map(TypeSpec::getKey).collect(Collectors.toList());
        assertThat(keys).containsExactlyInAnyOrder(KEY1, KEY2, KEY6);
    }

    @Test
    public void testFindAllNonAbstractTypes() {
        List<TypeSpec> types = xmEntitySpecService.findAllNonAbstractTypes();
        assertNotNull(types);
        assertEquals(4, types.size());

        List<String> keys = types.stream().map(TypeSpec::getKey).collect(Collectors.toList());
        assertThat(keys).containsExactlyInAnyOrder(KEY2, KEY3, KEY5, KEY6);
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
        assertEquals(4, types.size());

        List<String> keys = types.stream().map(TypeSpec::getKey).collect(Collectors.toList());
        assertThat(keys).containsExactlyInAnyOrder(KEY2, KEY3, KEY5, KEY6);
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
        assertEquals(5, keys.size());
        assertEquals(1, keys.get("TYPE1").get("LinkSpec").size());
        assertEquals(3, keys.get("TYPE1").get("StateSpec").size());
    }

    @Test
    public void testUniqueField() {
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("RESINTTEST")));
        tenantContextHolder = mock(TenantContextHolder.class);
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        ApplicationProperties ap = new ApplicationProperties();
        ap.setSpecificationPathPattern(URL);
        xmEntitySpecService = createXmEntitySpecService(ap, tenantContextHolder);

        for(TypeSpec typeSpec: xmEntitySpecService.getTypeSpecs().values()) {
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


}
