package com.icthh.xm.ms.entity.repository.util;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import org.junit.Test;

public class MappingUtilsUnitTest {

    @Test
    public void testParseDouble() {
        assertEquals(null, MappingUtils.parseDouble("Hello"));
        assertEquals(null, MappingUtils.parseDouble(null));
        assertEquals(0.0, MappingUtils.parseDouble("0.0"), 0.000001);
    }

    @Test
    public void testParseInstantMillis() {
        assertEquals(null, MappingUtils.parseInstantMillis("Hello"));
        assertEquals(null, MappingUtils.parseInstantMillis(null));
        assertEquals(Instant.ofEpochMilli(123456), MappingUtils.parseInstantMillis("123456"));
    }

    @Test
    public void testParseInstantTimestamp() {
        assertEquals(null, MappingUtils.parseInstantTimestamp("Hello"));
        assertEquals(null, MappingUtils.parseInstantTimestamp(null));
        assertEquals(Instant.parse("2011-12-03T10:15:30Z"), MappingUtils.parseInstantTimestamp("2011-12-03T10:15:30Z"));
    }

    @Test
    public void testFormat() {
        assertEquals(null, MappingUtils.format(Instant.now(), "Hello"));
        assertEquals(null, MappingUtils.format(null, "yyyy-MM-dd"));
        assertEquals(null, MappingUtils.format(null, null));
        assertEquals("2011-12-03", MappingUtils.format(Instant.parse("2011-12-03T10:15:30Z"), "yyyy-MM-dd"));
    }
}
