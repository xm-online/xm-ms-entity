package com.icthh.xm.ms.entity.domain.listener;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.lep.ElasticIndexManagerService;
import com.icthh.xm.ms.entity.util.AutowireHelper;
import groovyjarjarpicocli.CommandLine;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AvatarUrlListener {

    private String prefix;
    private String patternFull;
    private String patternPart;

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
