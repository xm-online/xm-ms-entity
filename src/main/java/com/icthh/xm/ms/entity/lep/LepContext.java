package com.icthh.xm.ms.entity.lep;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.domainevent.outbox.service.OutboxTransportService;
import com.icthh.xm.commons.domainevent.service.EventPublisher;
import com.icthh.xm.commons.domainevent.service.builder.DomainEventFactory;
import com.icthh.xm.commons.lep.BaseProceedingLep;
import com.icthh.xm.commons.lep.spring.LepThreadHelper;
import com.icthh.xm.commons.lep.spring.lepservice.LepServiceFactory;
import com.icthh.xm.commons.logging.trace.TraceService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import com.icthh.xm.ms.entity.config.RestTemplateConfiguration;
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
import com.icthh.xm.ms.entity.service.mail.MailService;
import com.icthh.xm.ms.entity.service.metrics.CustomMetricsContext;
import com.icthh.xm.ms.entity.service.metrics.MetricsAdapter;
import com.icthh.xm.ms.entity.service.search.ElasticsearchTemplateWrapper;
import org.springframework.web.client.RestTemplate;

public class LepContext {

    public Object commons;
    public Object inArgs;
    public BaseProceedingLep lep;
    public LepThreadHelper thread;
    public TraceService traceService;
    public OutboxTransportService outboxTransportService;
    public XmAuthenticationContext authContext;
    public TenantContext tenantContext;
    public Object methodResult;

    public LepServiceFactory lepServices;
    public LepServices services;
    public LepRepositories repositories;
    public LepTemplates templates;
    public CustomMetricsContext metricsContext;

    public static class LepServices {
        public Object xmTenantLifeCycle; // do not user this field
        public XmEntityService xmEntity;
        public ProfileService profileService;
        public LinkService linkService;
        public AttachmentService attachmentService;
        public MailService mailService;
        public TenantConfigService tenantConfigService;
        public LocationService locationService;
        public TagService tagService;
        public ProfileEventProducer profileEventProducer;
        public CommentService commentService;
        public PermissionCheckService permissionService;
        public EventService eventService;
        public CalendarService calendarService;
        public TenantLepResource lepResource;
        public ElasticsearchIndexService elasticsearchIndexService;
        public SeparateTransactionExecutor separateTransactionExecutor;
        public MetricsAdapter metricsAdapter;
        public EventPublisher eventPublisher;
        public DomainEventFactory domainEventFactory;
    }

    public static class LepRepositories {
        public XmEntityRepository xmEntity;
        public PermittedSearchRepository xmEntitySearch;
    }

    public static class LepTemplates {
        public RestTemplate rest;
        public RestTemplate plainRest;
        public RestTemplateConfiguration.PathTimeoutHttpComponentsClientHttpRequestFactory requestFactory;
        public AmazonS3Template s3;
        @Deprecated // switch it to elasticsearchTemplateWrapper
        public ElasticsearchTemplateWrapper elastic;
        public KafkaTemplateService kafka;
    }

}
