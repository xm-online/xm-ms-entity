package com.icthh.xm.ms.entity.repository.kafka;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import com.icthh.xm.ms.entity.domain.kafka.SystemEvent;
import org.junit.Test;

public class SystemEventUnitTest {

    @Test
    public void testToString() {
        SystemEvent event = new SystemEvent();
        event.setData("data");
        event.setEventId("eventId");
        event.setEventType("eventType");
        event.setMessageSource("messageSource");
        event.setStartDate("2017-11-21T09:14:35.826Z");
        event.setTenantKey("tenantKey");
        event.setUserLogin("userLogin");

        String result = event.toString();
        assertTrue(result.contains("eventId"));
        assertTrue(result.contains("eventType"));
        assertTrue(result.contains("messageSource"));
        assertFalse(result.contains("data"));
        assertFalse(result.contains("startDate"));
        assertFalse(result.contains("tenantKey"));
        assertFalse(result.contains("userLogin"));
    }
}
