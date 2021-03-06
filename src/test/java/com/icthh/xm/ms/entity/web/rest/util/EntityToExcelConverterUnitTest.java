package com.icthh.xm.ms.entity.web.rest.util;

import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.SimpleExportXmEntityDto;
import com.icthh.xm.ms.entity.domain.XmEntity;

import com.icthh.xm.ms.entity.domain.converter.EntityToExcelConverterUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.modelmapper.ModelMapper;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Tests excel converter.
 * @see EntityToCsvConverterUnitTest
 */
public class EntityToExcelConverterUnitTest extends AbstractUnitTest {

    private static final Long DEFAULT_LONG_VALUE = 1L;
    private static final String DEFAULT_STRING_VALUE = "FOO";

    private List<XmEntity> xmEntities = new LinkedList<>();

    @Before
    public void init() {
        xmEntities.clear();
        IntStream.range(0, 10).forEach(i -> xmEntities.add(buildXmEntity()));
    }

    @Test
    public void convertEntityToExcel() throws Exception {
        ModelMapper modelMapper = new ModelMapper();
        byte[] media = EntityToExcelConverterUtils.toExcel(
                        xmEntities.stream().map(entity -> modelMapper.map(entity, SimpleExportXmEntityDto.class))
                                        .collect(Collectors.toList()), "defaultSheetName");
        Assert.assertNotNull(media);
        Assert.assertThat(media.length, Matchers.greaterThan(1));
    }

    @Test
    public void convertEmptyEntitiesToExcel() throws Exception {
        byte[] media = EntityToExcelConverterUtils.toExcel(null, null);
        Assert.assertNotNull(media);
        Assert.assertThat(media.length, Matchers.is(0));
    }

    private XmEntity buildXmEntity() {
        XmEntity entity = new XmEntity();
        entity.setId(DEFAULT_LONG_VALUE);
        return entity.key(DEFAULT_STRING_VALUE).typeKey(DEFAULT_STRING_VALUE)
                        .stateKey(DEFAULT_STRING_VALUE).name(DEFAULT_STRING_VALUE)
                        .startDate(Instant.now()).updateDate(Instant.now()).endDate(Instant.now())
                        .avatarUrl(DEFAULT_STRING_VALUE).description(DEFAULT_STRING_VALUE)
                        .createdBy(DEFAULT_STRING_VALUE).removed(false);
    }
}
