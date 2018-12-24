package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.lep.commons.CommonsService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.ms.entity.lep.TenantLepResource;
import com.icthh.xm.ms.entity.lep.XmEntityMsLepProcessingApplicationListener;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.kafka.ProfileEventProducer;
import com.icthh.xm.ms.entity.repository.kafka.SystemTopicEventProducer;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.service.AttachmentService;
import com.icthh.xm.ms.entity.service.CalendarService;
import com.icthh.xm.ms.entity.service.CommentService;
import com.icthh.xm.ms.entity.service.EventService;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.service.LocationService;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.TagService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmTenantLifecycleService;
import com.icthh.xm.ms.entity.service.mail.MailService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * The {@link LepAppEventListenerConfiguration} class.
 */
@Configuration
public class LepAppEventListenerConfiguration {

    @Bean
    XmEntityMsLepProcessingApplicationListener buildLepProcessingApplicationListener(
                    XmEntityService xmEntityService,
                    XmTenantLifecycleService xmTenantLifecycleService,
                    XmEntityRepository xmEntityRepository,
                    ProfileService profileService,
                    LinkService linkService,
                    MailService mailService,
                    EventService eventService,
                    CalendarService calendarService,
                    CommentService commentService,
                    TenantConfigService tenantConfigService,
                    AttachmentService attachmentService,
                    @Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate,
                    LocationService locationService,
                    TagService tagService,
                    ProfileEventProducer profileEventProducer,
                    SystemTopicEventProducer systemTopicEventProducer,
                    CommonsService commonsService,
                    PermissionCheckService permissionCheckService,
                    TenantLepResource tenantLepResource,
                    PermittedSearchRepository permittedSearchRepository) {

        return new XmEntityMsLepProcessingApplicationListener(xmEntityService,
                        xmTenantLifecycleService, xmEntityRepository, profileService, linkService,
                        mailService, tenantConfigService, attachmentService, restTemplate,
                        locationService, tagService, profileEventProducer, systemTopicEventProducer, commentService, commonsService,
                        permissionCheckService, eventService, calendarService, tenantLepResource, permittedSearchRepository);
    }

}
