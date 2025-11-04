package com.icthh.xm.ms.entity.lep;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.domainevent.service.EventPublisher;
import com.icthh.xm.commons.domainevent.service.builder.DomainEventFactory;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepContextFactory;
import com.icthh.xm.commons.messaging.communication.service.CommunicationService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.search.ElasticsearchOperations;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import com.icthh.xm.lep.api.LepMethod;
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
import com.icthh.xm.ms.entity.service.impl.XmEntityAvatarService;
import com.icthh.xm.ms.entity.service.mail.MailService;
import com.icthh.xm.ms.entity.service.metrics.CustomMetricsContext;
import com.icthh.xm.ms.entity.service.metrics.MetricsAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * The {@link LepContextFactoryImpl} class.
 */
@Component
public class LepContextFactoryImpl implements LepContextFactory {

    private final XmEntityService xmEntityService;
    private final XmTenantLifecycleService xmTenantLifecycleService;
    private final XmEntityRepository xmEntityRepository;
    private final ProfileService profileService;
    private final LinkService linkService;
    private final MailService mailService;
    private final TenantConfigService tenantConfigService;
    private final AttachmentService attachmentService;
    private final XmEntityAvatarService xmEntityAvatarService;
    private final RestTemplate restTemplate;
    private final RestTemplate plainRestTemplate;
    private final PathTimeoutHttpComponentsClientHttpRequestFactory requestFactory;
    private final LocationService locationService;
    private final TagService tagService;
    private final ProfileEventProducer profileEventProducer;
    private final CommentService commentService;
    private final PermissionCheckService permissionCheckService;
    private final EventService eventService;
    private final CalendarService calendarService;
    private final TenantLepResource tenantLepResource;
    private final AmazonS3Template s3Template;
    private final ElasticsearchOperations elasticsearchOperations;
    private final PermittedSearchRepository permittedSearchRepository;
    private final ElasticsearchIndexService elasticsearchIndexService;
    private final SeparateTransactionExecutor transactionExecutor;
    private final CustomMetricsContext customMetricsContext;
    private final KafkaTemplateService kafkaTemplateService;
    private final MetricsAdapter metricsAdapter;
    private final EventPublisher eventPublisher;
    private final DomainEventFactory domainEventFactory;
    private final CommunicationService communicationService;

    public LepContextFactoryImpl(XmEntityService xmEntityService,
                                 XmTenantLifecycleService xmTenantLifecycleService,
                                 XmEntityRepository xmEntityRepository, ProfileService profileService,
                                 LinkService linkService,
                                 MailService mailService,
                                 TenantConfigService tenantConfigService,
                                 AttachmentService attachmentService,
                                 XmEntityAvatarService xmEntityAvatarService,
                                 @Qualifier("loadBalancedRestTemplateWithTimeout")
                                 RestTemplate restTemplate,
                                 @Qualifier("plainRestTemplate")
                                 RestTemplate plainRestTemplate,
                                 PathTimeoutHttpComponentsClientHttpRequestFactory requestFactory,
                                 LocationService locationService,
                                 TagService tagService,
                                 ProfileEventProducer profileEventProducer,
                                 CommentService commentService,
                                 PermissionCheckService permissionCheckService,
                                 EventService eventService,
                                 CalendarService calendarService,
                                 TenantLepResource tenantLepResource,
                                 AmazonS3Template s3Template,
                                 ElasticsearchOperations elasticsearchOperations,
                                 PermittedSearchRepository permittedSearchRepository,
                                 ElasticsearchIndexService elasticsearchIndexService,
                                 SeparateTransactionExecutor transactionExecutor,
                                 CustomMetricsContext customMetricsContext,
                                 KafkaTemplateService kafkaTemplateService,
                                 MetricsAdapter metricsAdapter,
                                 EventPublisher eventPublisher,
                                 DomainEventFactory domainEventFactory,
                                 CommunicationService communicationService) {
        this.xmEntityService = xmEntityService;
        this.xmTenantLifecycleService = xmTenantLifecycleService;
        this.xmEntityRepository = xmEntityRepository;
        this.profileService = profileService;
        this.linkService = linkService;
        this.mailService = mailService;
        this.tenantConfigService = tenantConfigService;
        this.attachmentService = attachmentService;
        this.xmEntityAvatarService = xmEntityAvatarService;
        this.restTemplate = restTemplate;
        this.plainRestTemplate = plainRestTemplate;
        this.requestFactory = requestFactory;
        this.locationService = locationService;
        this.tagService = tagService;
        this.profileEventProducer = profileEventProducer;
        this.commentService = commentService;
        this.permissionCheckService = permissionCheckService;
        this.eventService = eventService;
        this.calendarService = calendarService;
        this.tenantLepResource = tenantLepResource;
        this.s3Template = s3Template;
        this.elasticsearchOperations = elasticsearchOperations;
        this.permittedSearchRepository = permittedSearchRepository;
        this.elasticsearchIndexService = elasticsearchIndexService;
        this.transactionExecutor = transactionExecutor;
        this.customMetricsContext = customMetricsContext;
        this.kafkaTemplateService = kafkaTemplateService;
        this.metricsAdapter = metricsAdapter;
        this.eventPublisher = eventPublisher;
        this.domainEventFactory = domainEventFactory;
        this.communicationService = communicationService;
    }

    @Override
    public BaseLepContext buildLepContext(LepMethod lepMethod) {
        LepContext lepContext = new LepContext();
        lepContext.metricsContext = customMetricsContext;

        lepContext.services = new LepContext.LepServices();
        lepContext.services.profileService = profileService;
        lepContext.services.linkService = linkService;
        lepContext.services.xmEntity = xmEntityService;
        lepContext.services.xmTenantLifeCycle = xmTenantLifecycleService;
        lepContext.services.mailService = mailService;
        lepContext.services.tenantConfigService = tenantConfigService;
        lepContext.services.attachmentService = attachmentService;
        lepContext.services.xmEntityAvatarService = xmEntityAvatarService;
        lepContext.services.locationService = locationService;
        lepContext.services.tagService = tagService;
        lepContext.services.eventService = eventService;
        lepContext.services.calendarService = calendarService;
        lepContext.services.lepResource = tenantLepResource;
        lepContext.services.profileEventProducer = profileEventProducer;
        lepContext.services.commentService = commentService;
        lepContext.services.permissionService = permissionCheckService;
        lepContext.services.elasticsearchIndexService = elasticsearchIndexService;
        lepContext.services.separateTransactionExecutor = transactionExecutor;
        lepContext.services.metricsAdapter = metricsAdapter;
        lepContext.services.eventPublisher = eventPublisher;
        lepContext.services.domainEventFactory = domainEventFactory;
        lepContext.services.communicationService = communicationService;

        lepContext.repositories = new LepContext.LepRepositories();
        lepContext.repositories.xmEntity = xmEntityRepository;
        lepContext.repositories.xmEntitySearch = permittedSearchRepository;

        lepContext.templates = new LepContext.LepTemplates();
        lepContext.templates.rest = restTemplate;
        lepContext.templates.plainRest = plainRestTemplate;
        lepContext.templates.requestFactory = requestFactory;
        lepContext.templates.s3 = s3Template;
        lepContext.templates.elastic = elasticsearchOperations;
        lepContext.templates.kafka = kafkaTemplateService;

        return lepContext;
    }
}

