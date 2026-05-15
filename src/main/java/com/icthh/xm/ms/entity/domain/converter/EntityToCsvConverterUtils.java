package com.icthh.xm.ms.entity.domain.converter;

import static tools.jackson.core.StreamWriteFeature.IGNORE_UNKNOWN;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectWriter;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

/**
 * Convert {@link com.icthh.xm.ms.entity.domain.XmEntity} to csv file.
 */
@Slf4j
@UtilityClass
public class EntityToCsvConverterUtils {

    /**
     * Writes entities to csv file.
     * @param o the object which serialize to csv
     * @param clazz the class from which csv schema based would be
     * @return byte array of csv file
     */
    public static byte[] toCsv(Object o, Class clazz) {
        if (o == null) {
            log.warn("Passed empty object for serialize, therefore return empty byte array which represents csv file");
            return new byte[0];
        }

        CsvMapper mapper = createDefaultCsvMapper();
        ObjectWriter csvWriter = mapper.writer(clazz == null ? CsvSchema.emptySchema()
                                                            : createCsvSchemaBasedOnClass(mapper, clazz));
        try {
            return csvWriter.writeValueAsBytes(o);
        } catch (JacksonException e) {
            throw new IllegalStateException("Exception while writing data to csv file", e);
        }
    }

    /**
     * Writes entities to csv file.
     * @param o the object which serialize to csv
     * @param schema the csv schema
     * @return byte array of csv file
     */
    public static byte[] toCsv(Object o, CsvSchema schema) {
        if (o == null) {
            log.warn("Passed empty object for serialize, therefore return empty byte array which represents csv file");
            return new byte[0];
        }

        CsvMapper mapper = createDefaultCsvMapper();
        ObjectWriter csvWriter = mapper.writer(schema);
        try {
            return csvWriter.writeValueAsBytes(o);
        } catch (JacksonException e) {
            throw new IllegalStateException("Exception while writing data to csv file", e);
        }
    }

    private static CsvSchema createCsvSchemaBasedOnClass(CsvMapper mapper, Class clazz) {
        return mapper.schemaFor(clazz).withHeader();
    }

    private static CsvMapper createDefaultCsvMapper() {
        return CsvMapper.builder()
        .configure(IGNORE_UNKNOWN, true)
        .build();
    }

}
