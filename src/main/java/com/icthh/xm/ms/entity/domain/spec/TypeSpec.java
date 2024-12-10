package com.icthh.xm.ms.entity.domain.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.icthh.xm.commons.domain.DataSpec;
import com.icthh.xm.commons.domain.SpecWithInputDataAndForm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"key", "name", "namePattern", "nameValidationPattern", "descriptionPattern", "pluralName", "isApp", "isAbstract",
    "isAvatarEnabled", "isKeyRequired", "isNameRequired", "fastSearch", "icon", "dataSpec", "dataForm", "functions", "access", "attachments", "calendars",
    "links", "locations", "ratings", "states", "tags", "dataSpecInheritance", "dataFormInheritance", "disablePersistentReferenceProcessingOnSave"})
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class TypeSpec implements DataSpec, SpecWithInputDataAndForm {

    @JsonProperty("key")
    private String key;
    @JsonProperty("name")
    private Map<String, String> name;
    @JsonProperty("namePattern")
    private String namePattern;
    @JsonProperty("nameValidationPattern")
    private String nameValidationPattern;
    @JsonProperty("descriptionPattern")
    private String descriptionPattern;
    @JsonProperty("pluralName")
    private Map<String, String> pluralName;
    @JsonProperty("isApp")
    private Boolean isApp;
    @JsonProperty("isAbstract")
    private Boolean isAbstract;
    @JsonProperty("isAvatarEnabled")
    private Boolean isAvatarEnabled;
    @Builder.Default
    @JsonProperty("isKeyRequired")
    private Boolean isKeyRequired = true;
    @Builder.Default
    @JsonProperty("isNameRequired")
    private Boolean isNameRequired = true;
    @Builder.Default
    @JsonProperty("indexAfterSaveEnabled")
    private Boolean indexAfterSaveEnabled = true;
    @Builder.Default
    @JsonProperty("indexAfterDeleteEnabled")
    private Boolean indexAfterDeleteEnabled = true;
    @Builder.Default
    @JsonProperty("fastSearch")
    private List<FastSearchSpec> fastSearch = null;
    @JsonProperty("icon")
    private String icon;
    @Builder.Default
    @JsonProperty("dataSpec")
    private String dataSpec = null;
    @Builder.Default
    @JsonProperty("dataForm")
    private String dataForm = null;
    @JsonProperty("functions")
    private List<FunctionSpec> functions;
    @Builder.Default
    @JsonProperty("access")
    private List<String> access = null;
    @Builder.Default
    @JsonProperty("attachments")
    private List<AttachmentSpec> attachments = null;
    @Builder.Default
    @JsonProperty("calendars")
    private List<CalendarSpec> calendars = null;
    @Builder.Default
    @JsonProperty("links")
    private List<LinkSpec> links = null;
    @Builder.Default
    @JsonProperty("locations")
    private List<LocationSpec> locations = null;
    @Builder.Default
    @JsonProperty("ratings")
    private List<RatingSpec> ratings = null;
    @Builder.Default
    @JsonProperty("states")
    private List<StateSpec> states = null;
    @Builder.Default
    @JsonProperty("tags")
    private List<TagSpec> tags = null;
    @Builder.Default
    @JsonProperty("comments")
    private List<CommentSpec> comments = null;
    @Builder.Default
    @JsonIgnore
    private Set<UniqueFieldSpec> uniqueFields = new HashSet<>();
    @Builder.Default
    @JsonProperty("ignoreInheritanceFor")
    private Set<String> ignoreInheritanceFor = new HashSet<>();
    @Builder.Default
    @JsonProperty("dataSpecInheritance")
    private Boolean dataSpecInheritance = null;
    @Builder.Default
    @JsonProperty("dataFormInheritance")
    private Boolean dataFormInheritance = null;
    @Builder.Default
    @JsonProperty("disablePersistentReferenceProcessingOnSave")
    private Boolean disablePersistentReferenceProcessingOnSave = null;

    public Optional<LinkSpec> findLinkSpec(String typeKey) {
        if (links == null) {
            return Optional.empty();
        }
        return links.stream().filter(linkSpec -> typeKey.equals(linkSpec.getKey())).findAny();
    }

}
