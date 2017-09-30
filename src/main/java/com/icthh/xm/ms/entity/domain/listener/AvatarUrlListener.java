package com.icthh.xm.ms.entity.domain.listener;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.util.ApplicationContextHolder;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

@Slf4j
public class AvatarUrlListener {

    private static final String PATTERN_FULL = "^http:\\/\\/[a-zA-Z0-9-]+[.]rgw[.]icthh[.][a-z]+(:\\d+)?/[a-zA-Z0-9-.]+$";
    private static final String PATTERN_PART = "^[a-zA-Z0-9-.]+$";
    private String prefix;

    @PrePersist
    @PreUpdate
    public void prePersist(XmEntity obj) {
        String avatarUrl = obj.getAvatarUrl();
        if (StringUtils.isNoneBlank(avatarUrl)) {
            if (avatarUrl.matches(PATTERN_FULL)) {
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
            if (avatarUrl.matches(PATTERN_PART)) {
                obj.setAvatarUrl(getPrefix() + avatarUrl);
            } else {
                obj.setAvatarUrl(null);
            }
        }
    }

    private String getPrefix() {
        if (prefix == null) {
            ApplicationContext ctx = ApplicationContextHolder.getInstance().getApplicationContext();
            ApplicationProperties applicationProperties = ctx
                .getBean("applicationProperties", ApplicationProperties.class);
            prefix = String.format(applicationProperties.getAmazon().getAws().getTemplate(),
                applicationProperties.getAmazon().getS3().getBucket());
        }
        return prefix;
    }
}
