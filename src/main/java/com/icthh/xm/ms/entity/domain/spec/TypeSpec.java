package com.icthh.xm.ms.entity.domain.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder( {"key", "name", "isApp", "isAbstract", "fastSearch", "icon", "dataSpec", "dataForm", "functions",
    "access", "attachments", "calendars", "links", "locations", "ratings", "states", "tags"})
@Data
public class TypeSpec {

    @JsonProperty("key")
    private String key;
    @JsonProperty("name")
    private Map<String, String> name;
    @JsonProperty("isApp")
    private Boolean isApp;
    @JsonProperty("isAbstract")
    private Boolean isAbstract;
    @JsonProperty("fastSearch")
    private List<FastSearchSpec> fastSearch = null;
    @JsonProperty("icon")
    private String icon;
    @JsonProperty("dataSpec")
    private String dataSpec = null;
    @JsonProperty("dataForm")
    private String dataForm = null;
    @JsonProperty("functions")
    private List<FunctionSpec> functions;
    @JsonProperty("access")
    private List<String> access = null;
    @JsonProperty("attachments")
    private List<AttachmentSpec> attachments = null;
    @JsonProperty("calendars")
    private List<CalendarSpec> calendars = null;
    @JsonProperty("links")
    private List<LinkSpec> links = null;
    @JsonProperty("locations")
    private List<LocationSpec> locations = null;
    @JsonProperty("ratings")
    private List<RatingSpec> ratings = null;
    @JsonProperty("states")
    private List<StateSpec> states = null;
    @JsonProperty("tags")
    private List<TagSpec> tags = null;
    @JsonProperty("comments")
    private List<CommentSpec> comments = null;

}
