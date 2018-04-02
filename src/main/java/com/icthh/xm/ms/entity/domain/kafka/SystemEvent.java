package com.icthh.xm.ms.entity.domain.kafka;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Data
@ToString(of = {"eventType", "messageSource", "eventId"})
public class SystemEvent {

    private String eventId;
    private String messageSource;
    private String tenantKey;
    private String userLogin;
    private String eventType;
    @JsonIgnore
    private Instant startDate = Instant.now();
    private Object data;

    // FIXME this conversion should be on JSON marshaller level only
    @JsonProperty("startDate")
    public String getStartDate() {
        return startDate.toString();
    }

    // FIXME this conversion should be on JSON marshaller level only
    public void setStartDate(String startDate) {
        if (StringUtils.isNotBlank(startDate)) {
            this.startDate = Instant.parse(startDate);
        }
    }

    /**
     * Get data as Map.
     * @return map with data
     */
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDataMap() {
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        return Collections.emptyMap();
    }
}
