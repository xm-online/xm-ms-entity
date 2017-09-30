package com.icthh.xm.ms.entity.domain.kafka;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.icthh.xm.ms.entity.config.tenant.TenantInfo;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

@Data
public class SystemEvent {

    private String eventId;
    private String messageSource;
    private TenantInfo tenantInfo;
    private String eventType;
    @JsonIgnore
    private Instant startDate = Instant.now();
    private Map<String, Object> data = new HashMap<>();

    @JsonProperty("startDate")
    public String getStartDate() {
        return startDate.toString();
    }

    public void setStartDate(String startDate) {
        if (StringUtils.isNotBlank(startDate)) {
            this.startDate = Instant.parse(startDate);
        }
    }

}
