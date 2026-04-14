package com.icthh.xm.ms.entity.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;
import com.icthh.xm.ms.entity.domain.WithId;
import com.icthh.xm.ms.entity.domain.EntityBaseFields;
import com.icthh.xm.ms.entity.domain.WithTypeKey;
import com.icthh.xm.ms.entity.validator.JsonData;
import com.icthh.xm.ms.entity.validator.NotNull;
import static com.icthh.xm.ms.entity.validator.NotNullBySpecField.KEY;
import static com.icthh.xm.ms.entity.validator.NotNullBySpecField.NAME;
import com.icthh.xm.ms.entity.validator.StateKey;
import com.icthh.xm.ms.entity.validator.TypeKey;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@TypeKey
@StateKey
@JsonData
@ApiModel(description = "Represents any XM entity like Account, Product Offering, Product, Order, Handling, "
    + "Resource, Task, Agreement, Contact, Organization, Price, Channel, Segment and other.")
@NotNull(field = NAME)
@NotNull(field = KEY)
@Getter
@Setter
public class XmEntityDto implements Serializable, WithId, WithTypeKey, EntityBaseFields {

    private static final long serialVersionUID = 1L;

    private Long id;

    @ApiModelProperty(value = "Additional lateral identification for this entity could be defined by template Example: PO-1 for Product Offering, e-Mail or MSISDN for account, external Id for Order etc). Key is not full unique entity identification. Few entities could have one key, but only one entity could be addressable at one time, as other should be ended by endDate property.", required = true)
    private String key;

    @jakarta.validation.constraints.NotNull
    @ApiModelProperty(value = "Key reference to the configured Entity Type. Convention is capital letter with dash '-'. Example: ACCOUNT, PRODUCT-OFFERING, PRICE etc. Entity Sub Types could be separated by dot '.'. Convention is same as for Type. Example: ACCOUNT.ADMIN, ACCOUNT.USER, ACCOUNT.PARTNER for type ACCOUNT or PRODUCT-OFFERING.CAMPAIGN, PRODUCT-OFFERING.RATE-PLAN etc.", required = true)
    private String typeKey;

    @ApiModelProperty(value = "Key reference to the configured Entity State. Entity State matrix related to the pair of Entity Type. Convention is same as for type (ACTIVE, ORDERED, PRODUCTION, CANCELED, CANCELED.MANUAL etc).")
    private String stateKey;

    @ApiModelProperty(value = "This is i18n name of Entity. TODO: change data type", required = true)
    private String name;

    @ApiModelProperty(value = "Start date.")
    private Instant startDate;

    @ApiModelProperty(value = "Update date.")
    private Instant updateDate;

    @ApiModelProperty(value = "End date.")
    private Instant endDate;

    @ApiModelProperty(value = "Link to the avatar image.")
    private String avatarUrl;

    @ApiModelProperty(value = "This field describe instance of entity and is not i18n. It could be a big text. Markdown or HTML format should be supported.")
    private String description;

    @ApiModelProperty(value = "Data property represents entity fields as JSON structure. Fields specified by Formly and could use them for form building.")
    @JsonDeserialize(using = UntypedObjectDeserializer.class)
    private Map<String, Object> data = new HashMap<>();

    @ApiModelProperty(value = "Field is used to mark entities as deleted without physical deletion.")
    private Boolean removed;

    @ApiModelProperty(value = "Created by user key.")
    private String createdBy;

    @ApiModelProperty(value = "Updated by user key.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String updatedBy;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer version;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<AttachmentDto> attachments = new HashSet<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<CalendarDto> calendars = new HashSet<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<LocationDto> locations = new HashSet<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<RatingDto> ratings = new HashSet<>();

    @Valid
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<TagDto> tags = new HashSet<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<CommentDto> comments = new HashSet<>();

    @JsonIgnore
    private Set<VoteDto> votes = new HashSet<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<LinkDto> sources = new HashSet<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<LinkDto> targets = new HashSet<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<FunctionContextDto> functionContexts = new HashSet<>();

    @JsonIgnore
    private Set<EventDto> events = new HashSet<>();

    public Set<LinkDto> getSources() {
        return sources;
    }

    @JsonProperty
    public void setSources(Set<LinkDto> sources) {
        this.sources = sources;
    }

    public Boolean isRemoved() {
        return removed;
    }
}
