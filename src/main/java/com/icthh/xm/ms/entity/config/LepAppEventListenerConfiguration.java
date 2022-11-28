package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.domain.event.service.EventPublisher;
import com.icthh.xm.commons.domain.event.service.OutboxTransportService;
import com.icthh.xm.commons.domain.event.service.builder.DomainEventFactory;
import com.icthh.xm.commons.lep.commons.CommonsService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import com.icthh.xm.ms.entity.config.RestTemplateConfiguration.PathTimeoutHttpComponentsClientHttpRequestFactory;
import com.icthh.xm.ms.entity.config.amazon.AmazonS3Template;
import com.icthh.xm.ms.entity.lep.TenantLepResource;
import com.icthh.xm.ms.entity.lep.XmEntityMsLepProcessingApplicationListener;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.kafka.ProfileEventProducer;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.service.AttachmentService;
import com.icthh.xm.ms.entity.service.CalendarService;
import com.icthh.xm.ms.entity.service.CommentService;
import com.icthh.xm.ms.entity.service.ElasticsearchIndexService;
import com.icthh.xm.ms.entity.service.EventService;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.service.LocationService;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.SeparateTransactionExecutor;
import com.icthh.xm.ms.entity.service.TagService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmTenantLifecycleService;
import com.icthh.xm.ms.entity.service.mail.MailService;
import com.icthh.xm.ms.entity.service.metrics.CustomMetricsContext;
import com.icthh.xm.ms.entity.service.metrics.MetricsAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
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
        @Qualifier("loadBalancedRestTemplateWithTimeout") RestTemplate loadBalancedRestTemplateWithTimeout,
        @Qualifier("plainRestTemplate") RestTemplate plainRestTemplate,
        PathTimeoutHttpComponentsClientHttpRequestFactory requestFactory,
        LocationService locationService,
        TagService tagService,
        ProfileEventProducer profileEventProducer,
        CommonsService commonsService,
        PermissionCheckService permissionCheckService,
        TenantLepResource tenantLepResource,
        AmazonS3Template amazonS3Template,
        ElasticsearchTemplate elasticsearchTemplate,
        PermittedSearchRepository permittedSearchRepository,
        ElasticsearchIndexService elasticsearchIndexService,
        SeparateTransactionExecutor transactionExecutor,
        CustomMetricsContext customMetricsContext,
        KafkaTemplateService kafkaTemplateService,
        MetricsAdapter metricsAdapter,
        EventPublisher eventPublisher,
        OutboxTransportService outboxTransportService,
        DomainEventFactory domainEventFactory
    ) {

        return new XmEntityMsLepProcessingApplicationListener(xmEntityService,
            xmTenantLifecycleService, xmEntityRepository, profileService, linkService,
            mailService, tenantConfigService, attachmentService, loadBalancedRestTemplateWithTimeout, plainRestTemplate,
            requestFactory, locationService, tagService, profileEventProducer, commentService,
            commonsService, permissionCheckService, eventService, calendarService, tenantLepResource,
            amazonS3Template, elasticsearchTemplate, permittedSearchRepository, elasticsearchIndexService,
            transactionExecutor, customMetricsContext, kafkaTemplateService, metricsAdapter,
            eventPublisher, outboxTransportService, domainEventFactory);
    }

}
