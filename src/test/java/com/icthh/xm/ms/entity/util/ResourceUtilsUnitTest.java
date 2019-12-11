package com.icthh.xm.ms.entity.util;

import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

import com.icthh.xm.ms.entity.AbstractUnitTest;
import org.junit.Test;

public class ResourceUtilsUnitTest extends AbstractUnitTest {

    @Test
    public void testResourceExists() {
        String result = ResourceUtils.getResourceAsStr("config/application.yml");

        assertThat(result, not(isEmptyString()));
    }

    @Test(expected = IllegalStateException.class)
    public void testResourceNotExists() {
        String result = ResourceUtils.getResourceAsStr("config/bad.yml");

        assertThat(result, isEmptyString());
    }
}
