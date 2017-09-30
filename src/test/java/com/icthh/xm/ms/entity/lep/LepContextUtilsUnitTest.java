package com.icthh.xm.ms.entity.lep;

import com.icthh.lep.api.ContextScopes;
import com.icthh.lep.api.ContextsHolder;
import com.icthh.lep.api.ScopedContext;
import com.icthh.xm.ms.entity.config.tenant.TenantInfo;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * The {@link LepContextUtilsUnitTest} class.
 */
public class LepContextUtilsUnitTest {

    private static final String TEST_TENANT_NAME = "test-tenant";

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Mock
    private ContextsHolder contextsHolder;
    @Mock
    private ScopedContext threadContext;
    @Mock
    private TenantInfo testTenantInfo;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void whenValidContextThenReturnTenantName() {
        when(contextsHolder.getContext(eq(ContextScopes.THREAD))).thenReturn(threadContext);
        when(threadContext.getValue(eq(XmLepConstants.CONTEXT_KEY_TENANT),
                                    eq(TenantInfo.class))).thenReturn(testTenantInfo);
        when(testTenantInfo.getTenant()).thenReturn(TEST_TENANT_NAME);

        assertEquals(TEST_TENANT_NAME, LepContextUtils.getTenantName(contextsHolder));
    }

    @Test
    public void whenNoThreadContextThenThrowException() {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage(Matchers.startsWith("LEP manager thread context doesn't initialized."));

        when(contextsHolder.getContext(eq(ContextScopes.THREAD))).thenReturn(null);

        LepContextUtils.getTenantName(contextsHolder);
    }

    @Test
    public void whenNoTenantInfoThenThrowException() {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage(Matchers.startsWith("LEP manager thread context doesn't have value for var: "));

        when(contextsHolder.getContext(eq(ContextScopes.THREAD))).thenReturn(threadContext);
        when(threadContext.getValue(eq(XmLepConstants.CONTEXT_KEY_TENANT),
                                    eq(TenantInfo.class))).thenReturn(null);

        assertEquals(TEST_TENANT_NAME, LepContextUtils.getTenantName(contextsHolder));
    }

}
