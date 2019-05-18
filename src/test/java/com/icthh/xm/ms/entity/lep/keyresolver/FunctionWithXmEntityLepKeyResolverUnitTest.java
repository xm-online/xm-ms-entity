package com.icthh.xm.ms.entity.lep.keyresolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.lep.api.LepKey;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.MethodSignature;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.commons.lep.XmLepConstants;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;

/**
 * The {@link FunctionWithXmEntityLepKeyResolverUnitTest} class.
 */
public class FunctionWithXmEntityLepKeyResolverUnitTest extends AbstractUnitTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private SeparatorSegmentedLepKey initialExtensionKey;
    private FunctionWithXmEntityLepKeyResolver resolver;
    private LepManagerService managerService;
    private LepMethod lepMethod;

    @Before
    public void before() {
        initialExtensionKey = new SeparatorSegmentedLepKey(XmLepConstants.EXTENSION_KEY_SEPARATOR,
                                                           Arrays.asList("xm", "entity", "function", "Function"),
                                                           XmLepConstants.EXTENSION_KEY_GROUP_MODE);
        resolver = new FunctionWithXmEntityLepKeyResolver();
        managerService = null;

        lepMethod = mock(LepMethod.class);

        MethodSignature methodSignature = mock(MethodSignature.class);
        when(methodSignature.getParameterNames()).thenReturn(new String[] {
            "xmEntityTypeKey",
            "functionKey"
        });
        when(lepMethod.getMethodSignature()).thenReturn(methodSignature);
    }

    @Test
    public void validFunctionKeyEntityTypeKey() {
        when(lepMethod.getMethodArgValues()).thenReturn(new Object[] {
            "ACCOUNT",
            "ACCOUNT.EXTRACT-LINKEDIN-PROFILE"
        });

        // initialExtensionKey == 'xm.entity.function.Function'
        LepKey extensionKey = resolver.resolve(initialExtensionKey, lepMethod, managerService);
        assertNotNull(extensionKey);
        assertEquals("xm.entity.function", extensionKey.getGroupKey().getId());
        assertEquals("xm.entity.function.Function.ACCOUNT$EXTRACT_LINKEDIN_PROFILE", extensionKey.getId());
    }

    @Test
    public void exceptionOnNullFunctionKey() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage(Matchers.startsWith("LEP method"));

        when(lepMethod.getMethodArgValues()).thenReturn(new Object[] {
            null,
            null
        });
        resolver.resolve(initialExtensionKey, lepMethod, managerService);
    }

    @Test
    public void baseKeyCantBeNull() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("baseKey can't be null");

        when(lepMethod.getMethodArgValues()).thenReturn(new Object[] {
            null,
            null
        });
        resolver.resolve(null, lepMethod, managerService);
    }

}
