package com.icthh.xm.ms.entity.security.filter;

import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class EntityFunctionPermissionMsNameFilterUnitTest extends AbstractJupiterUnitTest {

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
