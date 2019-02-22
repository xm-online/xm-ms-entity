package com.icthh.xm.ms.entity.config.tenant;

import static org.junit.Assert.assertEquals;

import com.icthh.xm.commons.migration.db.tenant.SchemaChangeResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

/**
 * Test schema change resolver.
 */
public class SchemaChangeResolverUnitTest {

    private SchemaChangeResolver resolver;

    @Mock
    private Environment environment;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        resolver = new SchemaChangeResolver(environment);
    }

    @Test
    public void testDefaultSchemaResolver() {
        assertEquals("USE %s", resolver.getSchemaSwitchCommand());
    }

}
