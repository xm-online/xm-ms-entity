package com.icthh.xm.ms.entity.domain.listener;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.util.ApplicationContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

@Slf4j
public class AvatarUrlListener {

    private String prefix;
    private String patternFull;
    private String patternPart;

    @PrePersist
    @PreUpdate
    public void prePersist(XmEntity obj) {
        String avatarUrl = obj.getAvatarUrl();
        if (StringUtils.isNoneBlank(avatarUrl)) {
            if (isUrlMatchesPattern(avatarUrl, getPatternFull())) {
                obj.setAvatarUrl(FilenameUtils.getName(avatarUrl));
            } else {
                obj.setAvatarUrl(null);
            }
        }
    }

    @PostLoad
    public void postLoad(XmEntity obj) {
        String avatarUrl = obj.getAvatarUrl();
        if (StringUtils.isNoneBlank(avatarUrl)) {
            if (isUrlMatchesPattern(avatarUrl, getPatternPart())) {
                obj.setAvatarUrl(getPrefix() + avatarUrl);
            } else {
                obj.setAvatarUrl(null);
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
        ApplicationContext ctx = ApplicationContextHolder.getInstance().getApplicationContext();
        return ctx.getBean("applicationProperties", ApplicationProperties.class);
    }
}
