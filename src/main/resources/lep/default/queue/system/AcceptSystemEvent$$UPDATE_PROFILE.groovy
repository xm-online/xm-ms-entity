import com.icthh.xm.ms.entity.domain.XmEntity
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory

import java.time.Instant


def log = LoggerFactory.getLogger(getClass())

def data = lepContext.inArgs.event.data

log.info("Start to update profile for userKey='{}'", data.userKey);

def profile = lepContext.services.profileService.getProfile(data.userKey);

if (profile == null) {
    log.error("Failed to update profile. Profile with userKey='{}' not exists.", data.userKey);
    return;
}
XmEntity entity = profile.getXmentity();

entity.setName("$data.firstName $data.lastName");
if (data.imageUrl) {
    entity.setAvatarUrl(data.imageUrl?.toString());
}

String startDate = data.createdDate?.toString();
if (StringUtils.isNotBlank(startDate)) {
    entity.setStartDate(Instant.parse(startDate));
}
String updateDate = data.lastModifiedDate?.toString()
if (StringUtils.isNotBlank(updateDate)) {
    entity.setUpdateDate(Instant.parse(updateDate));
}

lepContext.services.profileService.save(profile);


