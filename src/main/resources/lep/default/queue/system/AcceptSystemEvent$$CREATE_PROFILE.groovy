import com.icthh.xm.ms.entity.domain.EntityState
import com.icthh.xm.ms.entity.domain.Profile
import com.icthh.xm.ms.entity.domain.XmEntity
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory

import java.time.Instant


def log = LoggerFactory.getLogger(getClass())

def data = lepContext.inArgs.event.data

log.info("Start to create profile for userKey='{}'", data.userKey);

def profile = lepContext.services.profileService.getProfile(data.userKey);

if (profile != null) {
    log.error("Failed to create profile. Profile with userKey='{}' already exists.", userKey);
    return;
}

profile = new Profile();
profile.setXmentity(new XmEntity());

XmEntity entity = profile.getXmentity();
entity.setTypeKey("ACCOUNT.USER");
profile.setXmentity(entity);

entity.setKey("${entity.getTypeKey()}-$data.id");
entity.setName("$data.firstName $data.lastName");
entity.setAvatarUrl(data.imageUrl?.toString());
entity.setStateKey(EntityState.NEW.name());

String startDate = data.createdDate?.toString();
if (StringUtils.isNotBlank(startDate)) {
    entity.setStartDate(Instant.parse(startDate));
} else {
    entity.setStartDate(Instant.now());
}
String updateDate = data.lastModifiedDate?.toString()
if (StringUtils.isNotBlank(updateDate)) {
    entity.setUpdateDate(Instant.parse(updateDate));
} else {
    entity.setUpdateDate(Instant.now());
}
profile.setUserKey(data.userKey?.toString());

entity.setData(data.data);
entity.setCreatedBy(data.userKey?.toString());

def savedProfile = lepContext.services.profileService.save(profile);
log.info("Profile {} saved", savedProfile)

