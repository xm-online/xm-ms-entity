package com.icthh.xm.ms.entity.domain.converter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

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
        } catch (JsonProcessingException e) {
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
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Exception while writing data to csv file", e);
        }
    }

    private static CsvSchema createCsvSchemaBasedOnClass(CsvMapper mapper, Class clazz) {
        return mapper.schemaFor(clazz).withHeader();
    }

    private static CsvMapper createDefaultCsvMapper() {
        CsvMapper mapper = new CsvMapper();
        mapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

}
