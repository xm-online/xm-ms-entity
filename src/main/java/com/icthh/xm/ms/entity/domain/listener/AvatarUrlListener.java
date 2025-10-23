package com.icthh.xm.ms.entity.domain.listener;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.XmEntity;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.icthh.xm.ms.entity.config.Constants.FILE_PREFIX;

@Slf4j
@Component
public class AvatarUrlListener {

    private String prefix;
    private String patternFull;
    private String patternPart;
    private ApplicationProperties.StorageType avatarStorageType;
    private String dbAvatarPrefix;
    private String dbUrlTemplate;

    private ApplicationProperties applicationProperties;

    @Autowired
    public void setApplicationProperties(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @PostConstruct
    public void init() {
        prefix = String.format(applicationProperties.getAmazon().getAws().getTemplate(),
            applicationProperties.getAmazon().getS3().getBucket());
        log.debug("Initializing AvatarUrlListener prefix={}", prefix);
        patternFull = applicationProperties.getAmazon().getAvatar().getPrePersistUrlFullPattern();
        log.debug("Initializing AvatarUrlListener patternFull={}", patternFull);
        patternPart = applicationProperties.getAmazon().getAvatar().getPostLoadUrlPartPattern();
        log.debug("Initializing AvatarUrlListener patternPart={}", patternPart);
        avatarStorageType = applicationProperties.getObjectStorage().getStorageType();
        dbAvatarPrefix = applicationProperties.getObjectStorage().getDbFilePrefix();
        dbUrlTemplate = applicationProperties.getObjectStorage().getDbUrlTemplate();
    }

    @PrePersist
    @PreUpdate
    public void prePersist(XmEntity obj) {
        String avatarUrl = obj.getAvatarUrlRelative();
        if (StringUtils.isNoneBlank(avatarUrl)) {
            if (isUrlMatchesPattern(avatarUrl, patternFull)) {
                obj.setAvatarUrlRelative(FilenameUtils.getName(avatarUrl));
            } else {
                obj.setAvatarUrlRelative(avatarUrl);
            }
        }
    }

    @PostLoad
    @PostPersist
    @PostUpdate
    public void postLoad(XmEntity obj) {
        String avatarUrl = obj.getAvatarUrlRelative();
        if (StringUtils.isNoneBlank(avatarUrl)) {

            if (ApplicationProperties.StorageType.DB == avatarStorageType) {
                if (StringUtils.startsWith(avatarUrl, dbAvatarPrefix)) {
                    obj.setAvatarUrlFull(dbUrlTemplate + "/" + avatarUrl);
                    return;
                }
            }

            if (ApplicationProperties.StorageType.FILE == avatarStorageType) {
                if (StringUtils.startsWith(avatarUrl, FILE_PREFIX)) {
                    //{murmur-fileName}
                    String avatarFileName = StringUtils.substringAfter(avatarUrl, FILE_PREFIX);
                    obj.setAvatarUrlFull(avatarUrl);
                    return;
                }
            }

            if (isUrlMatchesPattern(avatarUrl, patternPart)) {
                obj.setAvatarUrlFull(prefix + avatarUrl);
            } else {
                obj.setAvatarUrlFull(avatarUrl);
            }
        }
    }

    private boolean isUrlMatchesPattern(String avatarUrl, String pattern) {
        return avatarUrl != null && pattern != null && avatarUrl.matches(pattern);
    }

}
