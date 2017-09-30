package com.icthh.xm.ms.entity.lep;

import com.icthh.lep.api.LepResourceKey;
import com.icthh.lep.commons.GroupMode;
import com.icthh.lep.commons.SeparatorSegmentedLepKey;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * The {@link XmExtensionServiceUnitTest} class.
 */
public class XmExtensionServiceUnitTest {

    private XmExtensionService xmExtensionService;

    @Before
    public void before() {
        this.xmExtensionService = new XmExtensionService();
    }

    @Test
    public void returnNullResourceKeyOnNullExtensionKey() {
        LepResourceKey resourceKey = xmExtensionService.getResourceKey(null, null);
        assertNull(resourceKey);
    }

    @Test
    public void returnValidResourceKeyOnExtensionKey() {
        GroupMode groupMode = new GroupMode.Builder().prefixExcludeLastSegmentsAndIdIncludeGroup(1).build();
        SeparatorSegmentedLepKey extensionKey = new SeparatorSegmentedLepKey("com.icthh.xm.lep.Script",
                                                                             XmLepConstants.EXTENSION_KEY_SEPARATOR,
                                                                             groupMode);
        LepResourceKey resourceKey = xmExtensionService.getResourceKey(extensionKey, null);
        assertNotNull(resourceKey);
        assertEquals("lep:/com/icthh/xm/lep/Script.groovy", resourceKey.getId());
        assertNull(resourceKey.getVersion());
    }

}
