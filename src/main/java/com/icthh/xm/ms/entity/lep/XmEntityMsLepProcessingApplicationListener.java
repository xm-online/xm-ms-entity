package com.icthh.xm.ms.entity.lep;

import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_KEY_COMMONS;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_KEY_REPOSITORIES;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_KEY_SERVICES;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_KEY_TEMPLATES;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_METRICS_CONTEXT;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_COMMENT_SERVICE;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_PERMISSION_SERVICE;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_PROFILE_EVENT_PRODUCER_SERVICE;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_REPOSITORY_SEARCH;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_REPOSITORY_XM_ENTITY;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_REQUEST_FACTORY;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_SERVICE_ATTACHMENT;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_SERVICE_CALENDAR_SERVICE;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_SERVICE_DOMAIN_EVENT_FACTORY;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_SERVICE_ELASTICSEARCH_INDEXS;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_SERVICE_EVENT_PUBLISHER;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_SERVICE_EVENT_SERVICE;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_SERVICE_LEP_RESOURCE;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_SERVICE_LINK;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_SERVICE_LOCATION_SERVICE;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_SERVICE_MAIL_SERVICE;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_SERVICE_PROFILE;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_SERVICE_SEPARATE_TRANSACTION_EXECUTOR;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_SERVICE_TAG_SERVICE;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_SERVICE_TENANT_CONFIG_SERICE;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_SERVICE_XM_ENTITY;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_SERVICE_XM_TENANT_LC;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_TEMPLATE_ELASTIC;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_TEMPLATE_KAFKA;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_TEMPLATE_REST;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_TEMPLATE_PLAIN_REST;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_TEMPLATE_S3;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_KEY_METRICS_ADAPTER;
import static com.icthh.xm.ms.entity.lep.LepXmEntityMsConstants.BINDING_SUB_KEY_SERVICE_COMMUNICATION_SERVICE;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.domainevent.service.EventPublisher;
import com.icthh.xm.commons.domainevent.service.builder.DomainEventFactory;
import com.icthh.xm.commons.lep.commons.CommonsExecutor;
import com.icthh.xm.commons.lep.commons.CommonsService;
import com.icthh.xm.commons.lep.spring.SpringLepProcessingApplicationListener;
import com.icthh.xm.commons.messaging.communication.service.CommunicationService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import com.icthh.xm.lep.api.ScopedContext;
import com.icthh.xm.ms.entity.config.RestTemplateConfiguration.PathTimeoutHttpComponentsClientHttpRequestFactory;
import com.icthh.xm.ms.entity.config.amazon.AmazonS3Template;
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

import java.util.HashMap;
import java.util.Map;

import com.icthh.xm.ms.entity.service.metrics.MetricsAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * The {@link XmEntityMsLepProcessingApplicationListener} class.
 */
@RequiredArgsConstructor
public class XmEntityMsLepProcessingApplicationListener extends SpringLepProcessingApplicationListener {

    private final XmEntityService xmEntityService;
    private final XmTenantLifecycleService xmTenantLifecycleService;
    private final XmEntityRepository xmEntityRepository;
    private final ProfileService profileService;
    private final LinkService linkService;
    private final MailService mailService;
    private final TenantConfigService tenantConfigService;
    private final AttachmentService attachmentService;
    private final RestTemplate restTemplate;
    private final RestTemplate plainRestTemplate;
    private final PathTimeoutHttpComponentsClientHttpRequestFactory requestFactory;
    private final LocationService locationService;
    private final TagService tagService;
    private final ProfileEventProducer profileEventProducer;
    private final CommentService commentService;
    private final CommonsService commonsService;
    private final PermissionCheckService permissionCheckService;
    private final EventService eventService;
    private final CalendarService calendarService;
    private final TenantLepResource tenantLepResource;
    private final AmazonS3Template s3Template;
    private final ElasticsearchTemplate elasticsearchTemplate;
    private final PermittedSearchRepository permittedSearchRepository;
    private final ElasticsearchIndexService elasticsearchIndexService;
    private final SeparateTransactionExecutor transactionExecutor;
    private final CustomMetricsContext customMetricsContext;
    private final KafkaTemplateService kafkaTemplateService;
    private final MetricsAdapter metricsAdapter;
    private final EventPublisher eventPublisher;
    private final DomainEventFactory domainEventFactory;
    private final CommunicationService communicationService;

    @Override
    protected void bindExecutionContext(ScopedContext executionContext) {
        // services
        Map<String, Object> services = new HashMap<>();
        services.put(BINDING_SUB_KEY_SERVICE_PROFILE, profileService);
        services.put(BINDING_SUB_KEY_SERVICE_LINK, linkService);
        services.put(BINDING_SUB_KEY_SERVICE_XM_ENTITY, xmEntityService);
        services.put(BINDING_SUB_KEY_SERVICE_XM_TENANT_LC, xmTenantLifecycleService);
        services.put(BINDING_SUB_KEY_SERVICE_MAIL_SERVICE, mailService);
        services.put(BINDING_SUB_KEY_SERVICE_TENANT_CONFIG_SERICE, tenantConfigService);
        services.put(BINDING_SUB_KEY_SERVICE_ATTACHMENT, attachmentService);
        services.put(BINDING_SUB_KEY_SERVICE_LOCATION_SERVICE, locationService);
        services.put(BINDING_SUB_KEY_SERVICE_TAG_SERVICE, tagService);
        services.put(BINDING_SUB_KEY_SERVICE_EVENT_SERVICE, eventService);
        services.put(BINDING_SUB_KEY_SERVICE_CALENDAR_SERVICE, calendarService);
        services.put(BINDING_SUB_KEY_SERVICE_LEP_RESOURCE, tenantLepResource);
        services.put(BINDING_SUB_KEY_PROFILE_EVENT_PRODUCER_SERVICE, profileEventProducer);
        services.put(BINDING_SUB_KEY_COMMENT_SERVICE, commentService);
        services.put(BINDING_SUB_KEY_PERMISSION_SERVICE, permissionCheckService);
        services.put(BINDING_SUB_KEY_SERVICE_ELASTICSEARCH_INDEXS, elasticsearchIndexService);
        services.put(BINDING_SUB_KEY_SERVICE_SEPARATE_TRANSACTION_EXECUTOR, transactionExecutor);
        services.put(BINDING_KEY_METRICS_ADAPTER, metricsAdapter);
        services.put(BINDING_SUB_KEY_SERVICE_EVENT_PUBLISHER, eventPublisher);
        services.put(BINDING_SUB_KEY_SERVICE_DOMAIN_EVENT_FACTORY, domainEventFactory);
        services.put(BINDING_SUB_KEY_SERVICE_COMMUNICATION_SERVICE, communicationService);

        executionContext.setValue(BINDING_KEY_COMMONS, new CommonsExecutor(commonsService));

        executionContext.setValue(BINDING_KEY_SERVICES, services);

        executionContext.setValue(BINDING_METRICS_CONTEXT, customMetricsContext);

        // repositories
        Map<String, Object> repositories = new HashMap<>();
        repositories.put(BINDING_SUB_KEY_REPOSITORY_XM_ENTITY, xmEntityRepository);
        repositories.put(BINDING_SUB_KEY_REPOSITORY_SEARCH, permittedSearchRepository);

        executionContext.setValue(BINDING_KEY_REPOSITORIES, repositories);

        // templates
        Map<String, Object> templates = new HashMap<>();
        templates.put(BINDING_SUB_KEY_TEMPLATE_REST, restTemplate);
        templates.put(BINDING_SUB_KEY_TEMPLATE_PLAIN_REST, plainRestTemplate);
        templates.put(BINDING_SUB_KEY_REQUEST_FACTORY, requestFactory);
        templates.put(BINDING_SUB_KEY_TEMPLATE_S3, s3Template);
        templates.put(BINDING_SUB_KEY_TEMPLATE_ELASTIC, elasticsearchTemplate);
        templates.put(BINDING_SUB_KEY_TEMPLATE_KAFKA, kafkaTemplateService);

        executionContext.setValue(BINDING_KEY_TEMPLATES, templates);
    }
}

