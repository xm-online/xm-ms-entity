package com.icthh.xm.ms.entity.domain.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "key", "events" })
@Data
public class CalendarSpec {

    @JsonProperty("key")
    private String key;
    @JsonProperty("name")
    private Map<String, String> name;
    @JsonProperty("events")
    private List<EventSpec> events = null;

    /**
     * Calendar timeZone display strategy on interface. It could be:
     * STANDARD - default,
     * SUBJECT - timeZoneId for display is saved to Calendar.timeZoneId
     * and default timeZoneId is taken from XmEntity data by path timeZoneDataRef
     */
    @JsonProperty("timeZoneStrategy")
    private String timeZoneStrategy;
    @JsonProperty("timeZoneDataRef")
    private String timeZoneDataRef;

    @JsonProperty("readonly")
    private Boolean readonly;
}
