package com.icthh.xm.ms.entity.security.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.icthh.xm.ms.entity.AbstractUnitTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class EntityFunctionPermissionMsNameFilterUnitTest extends AbstractUnitTest {

    private final EntityFunctionPermissionMsNameFilter filter = new EntityFunctionPermissionMsNameFilter();
    private final static String APP_NAME = "appName";

    @Test
    public void testFilterPermission() {
        assertTrue(filter.filterPermission(null));
        assertTrue(filter.filterPermission(""));
        assertTrue(filter.filterPermission("other app name"));
        assertTrue(filter.filterPermission(APP_NAME));

        ReflectionTestUtils.setField(filter, "msName", APP_NAME, String.class);

        assertFalse(filter.filterPermission(null));
        assertFalse(filter.filterPermission(""));
        assertFalse(filter.filterPermission("other app name"));
        assertTrue(filter.filterPermission(APP_NAME));

        ReflectionTestUtils.setField(filter, "msName", "entity", String.class);
        assertTrue(filter.filterPermission("entity"));
        assertTrue(filter.filterPermission("entity-functions"));
        assertTrue(filter.filterPermission("entity-dynamic-permission"));

        ReflectionTestUtils.setField(filter, "msName", "communication", String.class);
        assertFalse(filter.filterPermission("entity"));
    }
}
