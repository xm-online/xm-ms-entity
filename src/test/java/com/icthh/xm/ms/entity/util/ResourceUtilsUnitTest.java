package com.icthh.xm.ms.entity.util;

import static org.junit.jupiter.api.Assertions.*;

import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import org.junit.jupiter.api.Test;

public class ResourceUtilsUnitTest extends AbstractJupiterUnitTest {

    @Test
    public void testResourceExists() {
        String result = ResourceUtils.getResourceAsStr("config/application.yml");
        assertFalse(result.isEmpty());
    }

    @Test
    public void testResourceNotExists() {
        assertThrows(IllegalStateException.class, () -> {
            String result = ResourceUtils.getResourceAsStr("config/bad.yml");
        });
    }
}
