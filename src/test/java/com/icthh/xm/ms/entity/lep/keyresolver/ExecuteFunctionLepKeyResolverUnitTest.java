package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.lep.api.LepKey;
import com.icthh.lep.api.LepManagerService;
import com.icthh.lep.api.LepMethod;
import com.icthh.lep.api.MethodSignature;
import com.icthh.lep.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.ms.entity.lep.XmLepConstants;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The {@link ExecuteFunctionLepKeyResolverUnitTest} class.
 */
public class ExecuteFunctionLepKeyResolverUnitTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private SeparatorSegmentedLepKey initialExtensionKey;
    private ExecuteFunctionLepKeyResolver resolver;
    private LepManagerService managerService;
    private LepMethod lepMethod;

    @Before
    public void before() {
        initialExtensionKey = new SeparatorSegmentedLepKey(XmLepConstants.EXTENSION_KEY_SEPARATOR,
                                                           Arrays.asList("xm", "entity", "function", "Function"),
                                                           XmLepConstants.EXTENSION_KEY_GROUP_MODE);
        resolver = new ExecuteFunctionLepKeyResolver();
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
        assertEquals("xm.entity.function.Function.ACCOUNT.ACCOUNT$EXTRACT_LINKEDIN_PROFILE", extensionKey.getId());
    }

    @Test
    public void validFunctionKeyWithoutEntityTypeKey() {
        when(lepMethod.getMethodArgValues()).thenReturn(new Object[] {
            null,
            "ACCOUNT.EXTRACT-LINKEDIN-PROFILE"
        });

        // initialExtensionKey == 'xm.entity.function.Function'
        LepKey extensionKey = resolver.resolve(initialExtensionKey, lepMethod, managerService);
        assertNotNull(extensionKey);
        assertEquals("xm.entity.function", extensionKey.getGroupKey().getId());
        assertEquals("xm.entity.function.Function._ANY_.ACCOUNT$EXTRACT_LINKEDIN_PROFILE", extensionKey.getId());
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
