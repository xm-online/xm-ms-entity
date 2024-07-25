package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Properties specific to JHipster.
 *
 * <p>Properties are configured in the application.yml file.
 */
@Component
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
@Getter
@Setter
public class ApplicationProperties {

    private final Amazon amazon = new Amazon();
    private final Retry retry = new Retry();
    private final Lep lep = new Lep();
    private final Jpa jpa = new Jpa();

    private List<String> tenantIgnoredPathList = Collections.emptyList();
    private List<String> timelineIgnoredHttpMethods = Collections.emptyList();
    /**
     * Default max avatar size 1Mb
     */
    private long maxAvatarSize = 1024 * 1024;
    private boolean timelinesEnabled;
    private boolean kafkaEnabled;
    private boolean schedulerEnabled;
    private List<String> tenantWithCreationAccessList;
    private List<String> tenantCreateServiceList;
    private Integer tenantClientConnectionTimeout;
    private Integer tenantClientReadTimeout;
    private String kafkaSystemTopic;
    private String kafkaSystemQueue;
    private Boolean autoSystemQueueEnabled;
    private Integer kafkaMetadataMaxAge;
    private String emailPathPattern;

    private String specificationFolderPathPattern;
    private String specificationPathPattern;
    private String specificationName;

    private String specificationTemplatesPathPattern;
    private String specificationTemplatesName;

    private String specificationWebappName;
    private String webappName;
    private String dbSchemaSuffix;
    private String elasticSchemaSuffix;
    private Integer elasticBatchSize;

    private Integer requestCacheLimit;
    private List<String> requestCacheIgnoredPathPatternList = Collections.emptyList();

    private Integer periodicMetricPoolSize;

    private KafkaMetric kafkaMetric;

    private DomainEvent domainEvent;

    private Elastic elastic;

    @Getter
    @Setter
    public static class Amazon {

        private final Avatar avatar = new Avatar();
        private final Aws aws = new Aws();
        private final S3 s3 = new S3();

        @Getter
        @Setter
        public static class Avatar {
            private String prePersistUrlFullPattern;
            private String postLoadUrlPartPattern;
        }

        @Getter
        @Setter
        public static class Aws {

            private String endpoint;
            private String template;
            private String region;
            private String accessKeyId;
            private String accessKeySecret;
        }

        @Getter
        @Setter
        public static class S3 {

            private String bucketPrefix;
            private String bucket;
        }
    }

    @Getter
    @Setter
    private static class Retry {
        private int maxAttempts;
        private long delay;
        private int multiplier;
    }

    @Getter
    @Setter
    public static class Lep {
        private TenantScriptStorage tenantScriptStorage;
        private String lepResourcePathPattern;
        private Boolean warmupScripts;
        private List<String> tenantsWithLepWarmup;
    }

    @Getter
    @Setter
    public static class KafkaMetric {
       private boolean enabled;
       private int connectionTimeoutTopic;
       List<String> metricTopics;
    }

    @Getter
    @Setter
    public static class DomainEvent {
        private boolean enabled;
    }
    @Getter
    @Setter
    public static class Jpa {
        private Integer findOneByIdForUpdateTimeout = 10000;
    }

    @Getter
    @Setter
    public static class Elastic {
        private String scheme;
        private String userName;
        private String password;
        private String host;
        private Integer port;
        private Integer connectTimeout;
        private Integer socketTimeout;
        private Integer connectRequestTimeout;
    }
}
