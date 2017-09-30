package com.icthh.xm.ms.entity.lep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;

import com.icthh.lep.api.ContextScopes;
import com.icthh.lep.api.ContextsHolder;
import com.icthh.lep.api.LepResource;
import com.icthh.lep.api.LepResourceDescriptor;
import com.icthh.lep.api.LepResourceKey;
import com.icthh.lep.api.ScopedContext;
import com.icthh.lep.commons.UrlLepResourceKey;
import com.icthh.xm.ms.entity.config.tenant.TenantInfo;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;

/**
 * The {@link XmClasspathLepResourceServiceUnitTest} class.
 */
@RunWith(SpringRunner.class)
public class XmClasspathLepResourceServiceUnitTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Autowired
    private ResourceLoader resourceLoader;

    private XmClasspathLepResourceService buildResourceService(String tenantName) {
        TenantInfo tenantInfo = Mockito.mock(TenantInfo.class);
        Mockito.when(tenantInfo.getTenant()).thenReturn(tenantName);

        ScopedContext threadContext = Mockito.mock(ScopedContext.class);
        Mockito.when(threadContext.getValue(eq(XmLepConstants.CONTEXT_KEY_TENANT),
                        eq(TenantInfo.class))).thenReturn(tenantInfo);

        ContextsHolder holder = Mockito.mock(ContextsHolder.class);
        Mockito.when(holder.getContext(eq(ContextScopes.THREAD))).thenReturn(threadContext);

        XmClasspathLepResourceService resourceService = new XmClasspathLepResourceService(holder);
        resourceService.setResourceLoader(resourceLoader);
        return resourceService;
    }

    @Test
    public void getResourceDescriptorThrowsNpeOnNullKey() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("resourceKey can't be null");

        buildResourceService("super").getResourceDescriptor(null);
    }

    @Test
    public void getResourceDescriptorReturnNullOnNonExistingKey() {
        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
                        .valueOfUrlResourcePath("/general/_QWE_Script_XYZ$$type.groovy");
        // create resource service
        XmClasspathLepResourceService resourceService = buildResourceService("super");

        LepResourceDescriptor resourceDescriptor = resourceService
                        .getResourceDescriptor(resourceKey);
        assertNull(resourceDescriptor);
    }

    private static void assertValidClasspathResourceDescriptor(
                    LepResourceKey resourceKey,
                    LepResourceDescriptor resourceDescriptor) {
        assertNotNull(resourceDescriptor);
        assertEquals(XmLepResourceType.GROOVY, resourceDescriptor.getType());
        assertEquals(resourceKey, resourceDescriptor.getKey());
        assertEquals(Instant.EPOCH, resourceDescriptor.getCreationTime());
        assertNotNull(resourceDescriptor.getModificationTime());
    }

    @Test
    public void getValidBeforeResourceDescriptor() {
        // create resource service
        XmClasspathLepResourceService resourceService = buildResourceService("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
                        .valueOfUrlResourcePath("/general/ScriptWithBeforeAfter$$before.groovy");

        LepResourceDescriptor resourceDescriptor = resourceService
                        .getResourceDescriptor(resourceKey);
        assertValidClasspathResourceDescriptor(resourceKey, resourceDescriptor);
    }

    @Test
    public void getValidAroundResourceDescriptor() {
        // create resource service
        XmClasspathLepResourceService resourceService = buildResourceService("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
                        .valueOfUrlResourcePath("/general/ScriptWithAround$$around.groovy");

        LepResourceDescriptor resourceDescriptor = resourceService
                        .getResourceDescriptor(resourceKey);
        assertValidClasspathResourceDescriptor(resourceKey, resourceDescriptor);
    }

    @Test
    public void getValidTenantResourceDescriptor() {
        // create resource service
        XmClasspathLepResourceService resourceService = buildResourceService("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
                        .valueOfUrlResourcePath("/general/ScriptWithTenant$$tenant.groovy");

        LepResourceDescriptor resourceDescriptor = resourceService
                        .getResourceDescriptor(resourceKey);
        assertValidClasspathResourceDescriptor(resourceKey, resourceDescriptor);
    }

    @Test
    public void getValidDefaultResourceDescriptor() {
        // create resource service
        XmClasspathLepResourceService resourceService = buildResourceService("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
                        .valueOfUrlResourcePath("/general/Script$$default.groovy");

        LepResourceDescriptor resourceDescriptor = resourceService
                        .getResourceDescriptor(resourceKey);
        assertValidClasspathResourceDescriptor(resourceKey, resourceDescriptor);
    }

    @Test
    public void getValidAfterResourceDescriptor() {
        // create resource service
        XmClasspathLepResourceService resourceService = buildResourceService("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
                        .valueOfUrlResourcePath("/general/ScriptWithBeforeAfter$$after.groovy");

        LepResourceDescriptor resourceDescriptor = resourceService
                        .getResourceDescriptor(resourceKey);
        assertValidClasspathResourceDescriptor(resourceKey, resourceDescriptor);
    }

    // =========== RESOURCES TESTS

    @Test
    public void getResourceThrowsNpeOnNullKey() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("resourceKey can't be null");

        buildResourceService("super puper").getResource(null);
    }

    @Test
    public void getResourceReturnNullOnNonExistingKey() {
        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
                        .valueOfUrlResourcePath("/general/Abc_Script_Xyz$$some-type.groovy");

        // create resource service
        XmClasspathLepResourceService resourceService = buildResourceService("super");

        LepResource resource = resourceService.getResource(resourceKey);
        assertNull(resource);
    }

    private static void assertValidClasspathResource(
                    LepResourceKey resourceKey,
                    LepResource resource,
                    String expectedScriptText) {
        assertNotNull(resource);
        assertNotNull(resource.getDescriptor());

        assertValidClasspathResourceDescriptor(resourceKey, resource.getDescriptor());

        String scriptText = resource.getValue(String.class);
        assertTrue("Expected contains script text: [" + expectedScriptText + "], actual text: ["
                        + scriptText + "]", scriptText.startsWith(expectedScriptText));
    }

    @Test
    public void getValidBeforeResource() {
        // create resource service
        XmClasspathLepResourceService resourceService = buildResourceService("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
                        .valueOfUrlResourcePath("/general/ScriptWithBeforeAfter$$before.groovy");

        LepResource resource = resourceService.getResource(resourceKey);
        assertValidClasspathResource(resourceKey, resource,
                        "return \"ScriptWithBeforeAfter.groovy before, tenant: super\"");
    }

    @Test
    public void getValidAroundResource() {
        // create resource service
        XmClasspathLepResourceService resourceService = buildResourceService("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
                        .valueOfUrlResourcePath("/general/ScriptWithAround$$around.groovy");

        LepResource resource = resourceService.getResource(resourceKey);
        assertValidClasspathResource(resourceKey, resource,
                        "return \"ScriptWithAround.groovy around, tenant: super\"");
    }

    @Test
    public void getValidTenantResource() {
        // create resource service
        XmClasspathLepResourceService resourceService = buildResourceService("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
                        .valueOfUrlResourcePath("/general/ScriptWithTenant$$tenant.groovy");

        LepResource resource = resourceService.getResource(resourceKey);
        assertValidClasspathResource(resourceKey, resource,
                        "return \"ScriptWithTenant.groovy tenant, tenant: super\"");
    }

    @Test
    public void getValidDefaultResource() {
        // create resource service
        XmClasspathLepResourceService resourceService = buildResourceService("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
                        .valueOfUrlResourcePath("/general/Script$$default.groovy");

        LepResource resource = resourceService.getResource(resourceKey);
        assertValidClasspathResource(resourceKey, resource, "return \"Script.groovy default\"");
    }

    @Test
    public void getValidAfterResource() {
        // create resource service
        XmClasspathLepResourceService resourceService = buildResourceService("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
                        .valueOfUrlResourcePath("/general/ScriptWithBeforeAfter$$after.groovy");

        LepResource resource = resourceService.getResource(resourceKey);
        assertValidClasspathResource(resourceKey, resource,
                        "return \"ScriptWithBeforeAfter.groovy after, tenant: super\"");
    }

    @Test
    public void getValidTenantResourceForTenant() {
        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
                        .valueOfUrlResourcePath("/general/ScriptWithTenant$$tenant.groovy");

        // TENANT 'super'

        // create resource service
        XmClasspathLepResourceService resourceService = buildResourceService("super");

        LepResource resource = resourceService.getResource(resourceKey);
        assertValidClasspathResource(resourceKey, resource,
                        "return \"ScriptWithTenant.groovy tenant, tenant: super\"");

        // TENANT 'test'

        // create resource service
        resourceService = buildResourceService("test");

        resource = resourceService.getResource(resourceKey);
        assertValidClasspathResource(resourceKey, resource,
                        "return \"ScriptWithTenant.groovy tenant, tenant: test\"");
    }

}
