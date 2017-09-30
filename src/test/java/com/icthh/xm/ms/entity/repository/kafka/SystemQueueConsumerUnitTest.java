package com.icthh.xm.ms.entity.repository.kafka;

import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.service.ProfileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EntityApp.class, SecurityBeanOverrideConfiguration.class, WebappTenantOverrideConfiguration.class})
public class SystemQueueConsumerUnitTest {

    private static final String USER_KEY = "f81d3142-a259-4ff8-99e4-be533d68ca99";

    private static final String CREATE_PROFILE_EVENT = "{  \n" +
        "   \"eventId\":\"f81d3142-a259-4ff8-99e4-be533d68ca88\",\n" +
        "   \"messageSource\":\"ms-uaa\",\n" +
        "   \"tenantInfo\":{  \n" +
        "      \"tenant\":\"XM\",\n" +
        "      \"xmToken\":\"\",\n" +
        "      \"xmCookie\":\"\",\n" +
        "      \"xmUserId\":\"\",\n" +
        "      \"xmLocale\":\"en\",\n" +
        "      \"userLogin\":\"\",\n" +
        "      \"userKey\":\"" + USER_KEY + "\"\n" +
        "   },\n" +
        "   \"eventType\":\"CREATE_PROFILE\",\n" +
        "   \"startDate\":\"2017-11-20T13:15:30Z\",\n" +
        "   \"data\":{  \n" +
        "      \"id\":1234,\n" +
        "      \"firstName\":\"Olena\",\n" +
        "      \"lastName\":\"Kashyna\",\n" +
        "      \"imageUrl\":\"\",\n" +
        "      \"activated\":true,\n" +
        "      \"langKey\":\"en\",\n" +
        "      \"createdBy\":\"system\",\n" +
        "      \"createdDate\":\"2017-11-20T13:15:30Z\",\n" +
        "      \"lastModifiedBy\":\"\",\n" +
        "      \"lastModifiedDate\":\"\",\n" +
        "\n" +
        "      \"userKey\":\"" + USER_KEY + "\"\n" +
        "   }\n" +
        "}";

    private static final String UPDATE_PROFILE_EVENT = "{  \n" +
        "   \"eventId\":\"f81d3142-a259-4ff8-99e4-be533d68ca88\",\n" +
        "   \"messageSource\":\"ms-uaa\",\n" +
        "   \"tenantInfo\":{  \n" +
        "      \"tenant\":\"XM\",\n" +
        "      \"xmToken\":\"\",\n" +
        "      \"xmCookie\":\"\",\n" +
        "      \"xmUserId\":\"\",\n" +
        "      \"xmLocale\":\"en\",\n" +
        "      \"userLogin\":\"\",\n" +
        "      \"userKey\":\"" + USER_KEY + "\"\n" +
        "   },\n" +
        "   \"eventType\":\"UPDATE_PROFILE\",\n" +
        "   \"startDate\":\"2017-11-20T13:15:30Z\",\n" +
        "   \"data\":{  \n" +
        "      \"id\":1234,\n" +
        "      \"firstName\":\"Olena\",\n" +
        "      \"lastName\":\"Kashyna\",\n" +
        "      \"imageUrl\":\"\",\n" +
        "      \"activated\":true,\n" +
        "      \"langKey\":\"en\",\n" +
        "      \"createdBy\":\"system\",\n" +
        "      \"createdDate\":\"2017-11-20T13:15:30Z\",\n" +
        "      \"lastModifiedBy\":\"\",\n" +
        "      \"lastModifiedDate\":\"\",\n" +
        "\n" +
        "      \"userKey\":\"" + USER_KEY + "\"\n" +
        "   }\n" +
        "}";

    private ProfileService profileService;

    private SystemQueueConsumer consumer;

    @Before
    public void init() {
        profileService = mock(ProfileService.class);
        consumer = new SystemQueueConsumer(profileService);
        TenantContext.setCurrent("TEST");
    }

    @Test
    public void createProfile() {
        when(profileService.getProfile(USER_KEY)).thenReturn(null);
        when(profileService.save(anyObject())).thenReturn(new Profile());
        consumer.consumeEvent(new ConsumerRecord<>("test", 0, 0, "", CREATE_PROFILE_EVENT));

        verify(profileService).getProfile(USER_KEY);
        verify(profileService).save(anyObject());

    }

    @Test
    public void createExistsProfile() {
        when(profileService.getProfile(USER_KEY)).thenReturn(new Profile());
        when(profileService.save(anyObject())).thenReturn(new Profile());
        consumer.consumeEvent(new ConsumerRecord<>("test", 0, 0, "", CREATE_PROFILE_EVENT));

        verify(profileService).getProfile(USER_KEY);
        verify(profileService, times(0)).save(anyObject());

    }

    @Test
    public void updateProfile() {
        when(profileService.getProfile(USER_KEY)).thenReturn(new Profile());
        when(profileService.save(anyObject())).thenReturn(new Profile());
        consumer.consumeEvent(new ConsumerRecord<>("test", 0, 0, "", UPDATE_PROFILE_EVENT));

        verify(profileService).getProfile(USER_KEY);
        verify(profileService).save(anyObject());
    }

    @Test
    public void updateNotExistsProfile() {
        when(profileService.getProfile(USER_KEY)).thenReturn(null);
        when(profileService.save(anyObject())).thenReturn(new Profile());
        consumer.consumeEvent(new ConsumerRecord<>("test", 0, 0, "", UPDATE_PROFILE_EVENT));

        verify(profileService).getProfile(USER_KEY);
        verify(profileService, times(0)).save(anyObject());
    }

}
