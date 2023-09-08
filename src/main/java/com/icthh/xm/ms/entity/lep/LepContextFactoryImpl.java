package com.icthh.xm.ms.entity.lep;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.domainevent.service.EventPublisher;
import com.icthh.xm.commons.domainevent.service.builder.DomainEventFactory;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepContextFactory;
import com.icthh.xm.commons.lep.commons.CommonsService;
import com.icthh.xm.commons.messaging.communication.service.CommunicationService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
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
import com.icthh.xm.ms.entity.service.mail.MailService;
import com.icthh.xm.ms.entity.service.metrics.CustomMetricsContext;
import com.icthh.xm.ms.entity.service.metrics.MetricsAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * The {@link LepContextFactoryImpl} class.
 */
@Component
@RequiredArgsConstructor
public class LepContextFactoryImpl implements LepContextFactory {

    private final XmEntityService xmEntityService;
    private final XmTenantLifecycleService xmTenantLifecycleService;
    private final XmEntityRepository xmEntityRepository;
    private final ProfileService profileService;
    private final LinkService linkService;
    private final MailService mailService;
    private final TenantConfigService tenantConfigService;
    private final AttachmentService attachmentService;
    @Qualifier("loadBalancedRestTemplateWithTimeout")
    private final RestTemplate restTemplate;
    @Qualifier("plainRestTemplate")
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
        lepContext.templates.elastic = elasticsearchTemplate;
        lepContext.templates.kafka = kafkaTemplateService;

        return lepContext;
    }
}

