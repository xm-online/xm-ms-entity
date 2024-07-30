package com.icthh.xm.ms.entity.domain.listener;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.util.AutowireHelper;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class AvatarUrlListener {

    private String prefix;
    private String patternFull;
    private String patternPart;

    @Autowired
    private ApplicationProperties applicationProperties;

    @PrePersist
    @PreUpdate
    public void prePersist(XmEntity obj) {
        String avatarUrl = obj.getAvatarUrlRelative();
        if (StringUtils.isNoneBlank(avatarUrl)) {
            if (isUrlMatchesPattern(avatarUrl, getPatternFull())) {
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
            if (isUrlMatchesPattern(avatarUrl, getPatternPart())) {
                obj.setAvatarUrlFull(getPrefix() + avatarUrl);
            } else {
                obj.setAvatarUrlFull(avatarUrl);
            }
        }
    }

    private String getPrefix() {
        if (prefix == null) {
            ApplicationProperties applicationProperties = getApplicationProperties();
            prefix = String.format(applicationProperties.getAmazon().getAws().getTemplate(),
                applicationProperties.getAmazon().getS3().getBucket());
        }
        return prefix;
    }

    private boolean isUrlMatchesPattern(String avatarUrl, String pattern) {
        return avatarUrl != null && pattern != null && avatarUrl.matches(pattern);
    }

    private String getPatternFull(){
        if(patternFull == null){
            patternFull = getApplicationProperties().getAmazon().getAvatar().getPrePersistUrlFullPattern();
        }
        return patternFull;
    }

    private String getPatternPart(){
        if(patternPart == null){
            patternPart = getApplicationProperties().getAmazon().getAvatar().getPostLoadUrlPartPattern();
        }
        return patternPart;
    }

    private ApplicationProperties getApplicationProperties() {
        AutowireHelper.autowire(this, applicationProperties);
        return applicationProperties;
    }
}
