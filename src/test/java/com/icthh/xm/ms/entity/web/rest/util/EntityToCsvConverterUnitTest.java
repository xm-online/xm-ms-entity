package com.icthh.xm.ms.entity.web.rest.util;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.collect.ImmutableMap;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.domain.converter.EntityToCsvConverterUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests csv converter.
 *
 * @see EntityToCsvConverterUnitTest
 */
public class EntityToCsvConverterUnitTest extends AbstractJupiterUnitTest {

    @Test
    public void convertWithBasedOnSchema() throws Exception {
        byte[] media = EntityToCsvConverterUtils.toCsv(new HumanTestModel("Foo", 228), HumanTestModel.class);
        assertNotNull(media);
        assertTrue(media.length > 1);
    }

    @Test
    public void convertWithCustomSchema() throws Exception {
        Map<String, Object> data = ImmutableMap.<String, Object>builder()
            .put("keyOne", "foooooo")
            .put("keyTwo", 1)
            .put("keyThree", false)
            .build();

        CsvSchema.Builder schemaBuilder = CsvSchema.builder();
        schemaBuilder.setUseHeader(true);
        // init headers
        data.keySet().forEach(key -> schemaBuilder.addColumn(key));
        byte[] media = EntityToCsvConverterUtils.toCsv(data, schemaBuilder.build());
        assertNotNull(media);
        assertTrue(media.length > 1);
    }

    @Getter
    @AllArgsConstructor
    class HumanTestModel {
        private String name;
        private Integer age;
    }
}
