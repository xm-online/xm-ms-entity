package com.icthh.xm.ms.entity.repository.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.contrib.jsonpath.annotation.JsonPath;
import lombok.Getter;
import lombok.Setter;
import org.junit.Before;
import org.junit.Test;

public class ElasticPathStrategyUnitTest {

    private ElasticPathStrategy elasticPathStrategy;

    @Before
    public void before() {
        elasticPathStrategy = new ElasticPathStrategy();
    }

    @Test
    public void testMap() {
        Car result  = elasticPathStrategy.map("{\"key\":\"hello\"}", Car.class);

        assertNotNull(result);
        assertEquals("hello", result.getKey());
    }

    @Setter
    @Getter
    public static class Car {
        @JsonPath("key")
        private String key;
    }
}
