package com.icthh.xm.ms.entity.domain.converter;

import com.icthh.xm.ms.entity.domain.SimpleExportXmEntityDto;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.DateFormatConverter;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Convert {@link com.icthh.xm.ms.entity.domain.XmEntity} to excel file.
 */
@UtilityClass
@Slf4j
public class EntityToExcelConverterUtils {

    private static final String[] headers = new String[] {"id", "key", "typeKey", "stateKey",
            "name", "startDate", "updateDate", "endDate", "avatarUrl", "description", "removed",
            "createdBy"};

    /**
     * Writes entities to excel file.
     * @param entities the entities list
     * @return byte array of excel file
     */
    public static byte[] toExcel(List<SimpleExportXmEntityDto> entities, String sheetName) {
        if (CollectionUtils.isEmpty(entities)) {
            log.warn("Passed empty object for serialize, therefore return empty byte array which represents excel file");
            return new byte[0];
        }
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet(sheetName);
            XSSFCreationHelper creationHelper = workbook.getCreationHelper();
            XSSFCellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setDataFormat(creationHelper.createDataFormat().getFormat(
                            DateFormatConverter.getJavaDateTimePattern(0, Locale.US)));

            int rowCount = 0;
            XSSFRow headerRow = sheet.createRow(rowCount);
            IntStream.range(0, headers.length).forEach(i -> headerRow.createCell(i).setCellValue(headers[i]));

            for (SimpleExportXmEntityDto entity : entities) {
                Row row = sheet.createRow(++rowCount);
                int columnCount = 0;
                Cell cell = row.createCell(columnCount);
                cell.setCellValue(entity.getOrElseId(0L));
                cell = row.createCell(++columnCount);
                cell.setCellValue(entity.getOrElseKey(StringUtils.EMPTY));
                cell = row.createCell(++columnCount);
                cell.setCellValue(entity.getOrElseTypeKey(StringUtils.EMPTY));
                cell = row.createCell(++columnCount);
                cell.setCellValue(entity.getOrElseStateKey(StringUtils.EMPTY));
                cell = row.createCell(++columnCount);
                cell.setCellValue(entity.getOrElseName(StringUtils.EMPTY));
                cell = row.createCell(++columnCount);
                cell.setCellValue(Date.from(entity.getOrElseStartDate(Instant.now())));
                cell.setCellStyle(cellStyle);
                cell = row.createCell(++columnCount);
                cell.setCellValue(Date.from(entity.getOrElseStartDate(Instant.now())));
                cell.setCellStyle(cellStyle);
                cell = row.createCell(++columnCount);
                cell.setCellValue(Date.from(entity.getOrElseEndDate(Instant.now())));
                cell.setCellStyle(cellStyle);
                cell = row.createCell(++columnCount);
                cell.setCellValue(entity.getOrElseAvatarUrl(StringUtils.EMPTY));
                cell = row.createCell(++columnCount);
                cell.setCellValue(entity.getOrElseDescription(StringUtils.EMPTY));
                cell = row.createCell(++columnCount);
                cell.setCellValue(entity.isOrElseRemoved(false));
                cell = row.createCell(++columnCount);
                cell.setCellValue(entity.getOrElseCreatedBy(StringUtils.EMPTY));
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Exception while writing data to excel file");
        }
    }

}
