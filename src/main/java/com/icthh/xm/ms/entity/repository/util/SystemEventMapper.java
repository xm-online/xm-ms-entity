package com.icthh.xm.ms.entity.repository.util;

import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.domain.EntityState;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.kafka.SystemEvent;

import java.time.Instant;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class SystemEventMapper {

    /**
     * Mapping system event to profile.
     * @param event the system event.
     * @param profile the user profile.
     */
    public static void toProfile(SystemEvent event, Profile profile) {
        if (profile.getXmentity() == null) {
            profile.setXmentity(new XmEntity());
        }
        XmEntity entity = profile.getXmentity();
        entity.setTypeKey(Constants.ACCOUNT_TYPE_KEY);
        profile.setXmentity(entity);
        Map<String, Object> data = event.getData();

        entity.setKey(entity.getTypeKey() + "-" + event.getData().get(Constants.ID));
        entity.setName(data.get(Constants.FIRST_NAME) + " " + data.get(Constants.LAST_NAME));
        entity.setAvatarUrl(String.valueOf(data.get(Constants.IMAGE_URL)));
        if (StringUtils.isBlank(entity.getStateKey())) {
            entity.setStateKey(EntityState.NEW.name());
        }
        String startDate = String.valueOf(data.get(Constants.CREATED_DATE));
        if (StringUtils.isNotBlank(startDate)) {
            entity.setStartDate(Instant.parse(startDate));
        } else {
            entity.setStartDate(Instant.now());
        }
        String updateDate = String.valueOf(data.get(Constants.LAST_MODIFIED_DATE));
        if (StringUtils.isNotBlank(updateDate)) {
            entity.setUpdateDate(Instant.parse(updateDate));
        } else {
            entity.setUpdateDate(Instant.now());
        }
        profile.setUserKey(String.valueOf(data.get(Constants.USER_KEY)));

        entity.setData((Map<String, Object>) event.getData().get(Constants.DATA));
    }
}
