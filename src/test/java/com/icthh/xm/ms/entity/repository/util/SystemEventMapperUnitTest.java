package com.icthh.xm.ms.entity.repository.util;

import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.domain.EntityState;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.kafka.SystemEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SystemEventMapperUnitTest extends AbstractJupiterUnitTest {

    private static final String DEFAULT_ID = "id";
    private static final String DEFAULT_FIRST_NAME = "firstName";
    private static final String DEFAULT_LAST_NAME = "lastName";
    private static final String DEFAULT_IMAGE_URL = "imageUrl";
    private static final String DEFAULT_ACTIVATED = "true";
    private static final Instant DEFAULT_CREATED_DATE = Instant.now();
    private static final Instant DEFAULT_LAST_MODIFIED_DATE = Instant.now();
    private static final String DEFAULT_USER_KEY = "userKey";

    @Test
    public void testMap() {
        Profile profile = new Profile();
        SystemEventMapper.toProfile(createEvent(), profile);

        assertNotNull(profile.getXmentity());
        assertEquals(DEFAULT_FIRST_NAME + " " + DEFAULT_LAST_NAME, profile.getXmentity().getName());
        assertEquals(Constants.ACCOUNT_TYPE_KEY + "-" + DEFAULT_ID, profile.getXmentity().getKey());
        assertEquals(DEFAULT_IMAGE_URL, profile.getXmentity().getAvatarUrl());
        assertEquals(EntityState.NEW.name(), profile.getXmentity().getStateKey());
        assertEquals(DEFAULT_CREATED_DATE, profile.getXmentity().getStartDate());
        assertEquals(DEFAULT_LAST_MODIFIED_DATE, profile.getXmentity().getUpdateDate());
        assertEquals(DEFAULT_USER_KEY, profile.getUserKey());
    }

    @Test
    public void testMapWithoutDate() {
        Profile profile = new Profile();
        SystemEventMapper.toProfile(createEventWithoutDates(), profile);

        assertNotNull(profile.getXmentity());
        assertEquals(DEFAULT_FIRST_NAME + " " + DEFAULT_LAST_NAME, profile.getXmentity().getName());
        assertEquals(Constants.ACCOUNT_TYPE_KEY + "-" + DEFAULT_ID, profile.getXmentity().getKey());
        assertEquals(DEFAULT_IMAGE_URL, profile.getXmentity().getAvatarUrl());
        assertEquals(EntityState.NEW.name(), profile.getXmentity().getStateKey());
        assertNotNull(profile.getXmentity().getStartDate());
        assertNotNull(profile.getXmentity().getUpdateDate());
        assertEquals(DEFAULT_USER_KEY, profile.getUserKey());
    }

    @Test
    public void testMapProfileAlreadyExists() {
        Long id = 1L;
        Profile profile = new Profile();
        profile.setId(id);
        profile.setXmentity(new XmEntity());
        profile.getXmentity().setStateKey(EntityState.ACTIVE.name());
        profile.getXmentity().setId(id);
        SystemEventMapper.toProfile(createEvent(), profile);

        assertNotNull(profile.getXmentity());
        assertEquals(id, profile.getId());
        assertEquals(id, profile.getXmentity().getId());
        assertEquals(DEFAULT_FIRST_NAME + " " + DEFAULT_LAST_NAME, profile.getXmentity().getName());
        assertEquals(Constants.ACCOUNT_TYPE_KEY + "-" + DEFAULT_ID, profile.getXmentity().getKey());
        assertEquals(DEFAULT_IMAGE_URL, profile.getXmentity().getAvatarUrl());
        assertEquals(EntityState.ACTIVE.name(), profile.getXmentity().getStateKey());
        assertNotNull(profile.getXmentity().getStartDate());
        assertNotNull(profile.getXmentity().getUpdateDate());
        assertEquals(DEFAULT_USER_KEY, profile.getUserKey());
    }

    private SystemEvent createEvent() {
        SystemEvent event = createEventWithoutDates();
        event.getDataMap().put(Constants.CREATED_DATE, DEFAULT_CREATED_DATE.toString());
        event.getDataMap().put(Constants.LAST_MODIFIED_DATE, DEFAULT_LAST_MODIFIED_DATE.toString());

        return event;
    }

    private SystemEvent createEventWithoutDates() {
        SystemEvent event = new SystemEvent();
        Map<String, Object> data = new HashMap<>();
        event.setData(data);
        data.put(Constants.ID, DEFAULT_ID);
        data.put(Constants.FIRST_NAME, DEFAULT_FIRST_NAME);
        data.put(Constants.LAST_NAME, DEFAULT_LAST_NAME);
        data.put(Constants.IMAGE_URL, DEFAULT_IMAGE_URL);
        data.put(Constants.ACTIVATED, DEFAULT_ACTIVATED);
        data.put(Constants.CREATED_DATE, "");
        data.put(Constants.LAST_MODIFIED_DATE, "");
        data.put(Constants.USER_KEY, DEFAULT_USER_KEY);

        return event;

    }
}
