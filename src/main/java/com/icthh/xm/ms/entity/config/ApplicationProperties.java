package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static com.icthh.xm.ms.entity.config.Constants.DEFAULT_AVATAR_URL;
import static com.icthh.xm.ms.entity.config.Constants.DEFAULT_AVATAR_URL_PREFIX;

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

    public static final Integer DEFAULT_MAX_AVATAR_SIZE = 1024 * 1024;
    public static final Integer DEFAULT_MAX_IMAGE_SIZE = 100;
    public static final String DEFAULT_DB_FILE_PREFIX = "db://xme/entity/obj/";
    public static final String DEFAULT_DB_URL_TMPLT = "https://%s.xm-online.com/entity/api/storage/object";
    public static final String DEFAULT_FILE_ROOT = "/home/xme";

    private final Amazon amazon = new Amazon();
    private final Retry retry = new Retry();
    private final Lep lep = new Lep();
    private final Jpa jpa = new Jpa();
    private final ObjectStorage objectStorage = new ObjectStorage();
    private final AvatarDefault avatarDefault = new AvatarDefault();

    private List<String> tenantIgnoredPathList = Collections.emptyList();
    private List<String> timelineIgnoredHttpMethods = Collections.emptyList();
    /**
     * Default max avatar size 1Mb
     */
    private long maxAvatarSize = DEFAULT_MAX_AVATAR_SIZE;
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
    private String permissionContextUri;
    private String dbSchemaSuffix;
    private String elasticSchemaSuffix;
    private Integer elasticBatchSize;

    private Integer requestCacheLimit;
    private List<String> requestCacheIgnoredPathPatternList = Collections.emptyList();

    private Integer periodicMetricPoolSize;

    private KafkaMetric kafkaMetric;

    private DomainEvent domainEvent;

    private DynamicPermissionCheck dynamicPermissionCheck;

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
    public static class DynamicPermissionCheck {
        private boolean enabled;
    }

    @Getter
    @Setter
    public static class Jpa {
        private Integer findOneByIdForUpdateTimeout = 10000;
    }

    @Setter
    public static class ObjectStorage {

        @Getter
        private StorageType storageType = StorageType.S3;
        private Integer maxImageSize = DEFAULT_MAX_IMAGE_SIZE;
        private Integer maxSize = DEFAULT_MAX_AVATAR_SIZE;
        private String dbFilePrefix;
        private String dbUrlTemplate;
        private String fileRoot;

        public Integer getMaxImageSize() {
            return (maxImageSize == null || maxImageSize <= 0) ? DEFAULT_MAX_IMAGE_SIZE : maxImageSize;
        }

        public Integer getMaxSize() {
            return (maxSize == null || maxSize <= 0) ? DEFAULT_MAX_AVATAR_SIZE : maxSize;
        }

        public String getDbFilePrefix() {
            return StringUtils.isEmpty(dbFilePrefix) ? DEFAULT_DB_FILE_PREFIX : dbFilePrefix;
        }

        public String getDbUrlTemplate() {
            return StringUtils.isEmpty(dbUrlTemplate) ? DEFAULT_DB_URL_TMPLT : dbUrlTemplate;
        }
        public String getFileRoot() {
            return StringUtils.isEmpty(fileRoot) ? DEFAULT_FILE_ROOT : fileRoot;
        }

    }

    @Getter
    @Setter
    public static class AvatarDefault {
        private String defaultAvatarUrlPrefix = DEFAULT_AVATAR_URL_PREFIX;
        private String defaultAvatarUrl = DEFAULT_AVATAR_URL;
        private long maxAvatarSize = DEFAULT_MAX_AVATAR_SIZE;
    }

    public enum StorageType {
        DB, S3, FILE
    }

}
