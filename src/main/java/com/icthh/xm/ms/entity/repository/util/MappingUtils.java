package com.icthh.xm.ms.entity.repository.util;

import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

@Slf4j
@UtilityClass
public final class MappingUtils {

    public static final String CF_PREFIX = "cf_char_";

    public static List<XmEntity> mapEntityList(List<?> entities) {
        List<XmEntity> result = new ArrayList<>(entities.size());
        for (Object from : entities) {
            XmEntity to = mapEntity(from);
            result.add(to);
        }
        return result;
    }

    public static XmEntity mapEntity(Object from) {
        XmEntity to = new XmEntity();
        BeanUtils.copyProperties(from, to);
        return to;
    }

    public static List<Link> mapLinkList(List<?> entities) {
        List<Link> result = new ArrayList<>(entities.size());
        for (Object from : entities) {
            Link to = mapLink(from);
            result.add(to);
        }
        return result;
    }

    public static Link mapLink(Object from) {
        Link to = new Link();
        BeanUtils.copyProperties(from, to);
        return to;
    }

    public static <T> T getOne(List<T> list) {
        return CollectionUtils.isNotEmpty(list) ? list.iterator().next() : null;
    }

    public static Double parseDouble(final String s) {
        if (s != null) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                log.error("Wrong double: {}", s, e);
                return null;
            }
        }
        return null;
    }

    public static Instant parseInstantMillis(final String millis) {
        try {
            return StringUtils.isNotBlank(millis) ? Instant.ofEpochMilli(Long.parseLong(millis)) : null;
        } catch (NumberFormatException e) {
            log.error("Wrong millis: {}", millis, e);
            return null;
        }
    }

    public static Instant parseInstantTimestamp(final String timestamp) {
        try {
            return StringUtils.isNotBlank(timestamp) ? Instant.parse(timestamp) : null;
        } catch (DateTimeParseException e) {
            log.error("Wrong timestamp: {}", timestamp, e);
            return null;
        }
    }

    public static String format(Instant instant, String pattern) {
        if (instant != null) {
            try {
                return DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault()).format(instant);
            } catch (DateTimeException | IllegalArgumentException e) {
                log.error("Wrong instant: {} or pattern: {}", instant, pattern, e);
                return null;
            }
        }
        return null;
    }

    public static String key(String typeKey, String id) {
        return typeKey + "-" + id;
    }
}
