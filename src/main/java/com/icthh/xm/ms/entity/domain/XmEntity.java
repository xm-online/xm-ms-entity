package com.icthh.xm.ms.entity.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.icthh.xm.ms.entity.domain.converter.MapToStringConverter;
import com.icthh.xm.ms.entity.domain.listener.AvatarUrlListener;
import com.icthh.xm.ms.entity.validator.JsonData;
import com.icthh.xm.ms.entity.validator.NotNullTenantAware;
import com.icthh.xm.ms.entity.validator.StateKey;
import com.icthh.xm.ms.entity.validator.TypeKey;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.elasticsearch.annotations.Document;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;

import static javax.persistence.CascadeType.PERSIST;

/**
 * Represents any XM entity like Account, Product Offering, Product, Order, Handling,
 * Resource, Task, Agreement, Contact, Organization, Price, Channel, Segment and other.
 */
@TypeKey
@StateKey
@JsonData
@ApiModel(description = "Represents any XM entity like Account, Product Offering, Product, Order, Handling, Resource, Task, Agreement, Contact, Organization, Price, Channel, Segment and other.")
@Entity
@Table(name = "xm_entity")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Document(indexName = "#{@indexName.prefix}xmentity")
// load tags and locations eagerly in repository queries hinted by @EntityGraph("xmEntityGraph")
@NamedEntityGraph(name = "xmEntityGraph",
    attributeNodes = {@NamedAttributeNode("tags"), @NamedAttributeNode("locations"),
        @NamedAttributeNode("attachments"), @NamedAttributeNode("targets"), @NamedAttributeNode("comments"),
        @NamedAttributeNode("ratings"), @NamedAttributeNode("functions") })
@EntityListeners(AvatarUrlListener.class)
public class XmEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    /**
     * Additional lateral identification for this entity could be defined by template
     * Example: PO-1 for Product Offering, e-Mail or MSISDN for account, external Id
     * for Order etc).
     * Key is not full unique entity identification. Few entities could have one key,
     * but only one entity could be addressable at one time, as other should be ended
     * by endDate property.
     */
    @NotNullTenantAware
    @ApiModelProperty(value = "Additional lateral identification for this entity could be defined by template Example: PO-1 for Product Offering, e-Mail or MSISDN for account, external Id for Order etc). Key is not full unique entity identification. Few entities could have one key, but only one entity could be addressable at one time, as other should be ended by endDate property.", required = true)
    @Column(name = "jhi_key", nullable = false)
    private String key;

    /**
     * Key reference to the configured Entity Type. Convention is capital letter with
     * dash '-'.
     * Example: ACCOUNT, PRODUCT-OFFERING, PRICE etc.
     * Entity Sub Types could be separated by dot '.'. Convention is same as for Type.
     * Example: ACCOUNT.ADMIN, ACCOUNT.USER, ACCOUNT.PARTNER for type ACCOUNT or
     * PRODUCT-OFFERING.CAMPAIGN, PRODUCT-OFFERING.RATE-PLAN etc.
     */
    @NotNull
    @ApiModelProperty(value = "Key reference to the configured Entity Type. Convention is capital letter with dash '-'. Example: ACCOUNT, PRODUCT-OFFERING, PRICE etc. Entity Sub Types could be separated by dot '.'. Convention is same as for Type. Example: ACCOUNT.ADMIN, ACCOUNT.USER, ACCOUNT.PARTNER for type ACCOUNT or PRODUCT-OFFERING.CAMPAIGN, PRODUCT-OFFERING.RATE-PLAN etc.", required = true)
    @Column(name = "type_key", nullable = false)
    private String typeKey;

    /**
     * Key reference to the configured Entity State. Entity State matrix related to the
     * pair of Entity Type. Convention is same as for type (ACTIVE, ORDERED, PRODUCTION,
     * CANCELED, CANCELED.MANUAL etc).
     */
    @ApiModelProperty(value = "Key reference to the configured Entity State. Entity State matrix related to the pair of Entity Type. Convention is same as for type (ACTIVE, ORDERED, PRODUCTION, CANCELED, CANCELED.MANUAL etc).")
    @Column(name = "state_key")
    private String stateKey;

    /**
     * This is i18n name of Entity.
     * TODO: change data type
     */
    @NotNullTenantAware
    @ApiModelProperty(value = "This is i18n name of Entity. TODO: change data type", required = true)
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Start date.
     */
    @NotNullTenantAware
    @ApiModelProperty(value = "Start date.", required = true)
    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    /**
     * Update date.
     */
    @NotNullTenantAware
    @ApiModelProperty(value = "Update date.", required = true)
    @Column(name = "update_date", nullable = false)
    private Instant updateDate;

    /**
     * End date.
     */
    @ApiModelProperty(value = "End date.")
    @Column(name = "end_date")
    private Instant endDate;

    /**
     * Relative link to the avatar image. It should support to use additional parameters
     * to identify avatar size for different views.
     */
    @ApiModelProperty(value = "Relative link to the avatar image. It should support to use additional parameters to identify avatar size for different views.")
    @Column(name = "avatar_url")
    private String avatarUrl;

    /**
     * This field describe instance of entity and is not i18n. It could be a big text.
     * Markdown or HTML format should be supported.
     */
    @ApiModelProperty(value = "This field describe instance of entity and is not i18n. It could be a big text. Markdown or HTML format should be supported.")
    @Column(name = "description")
    private String description;

    /**
     * Data property represents entity fields as JSON structure. Fields specified by
     * Formly and could use them for form building.
     */
    @ApiModelProperty(value = "Data property represents entity fields as JSON structure. Fields specified by Formly and could use them for form building.")
    @Convert(converter = MapToStringConverter.class)
    @Column(name = "data")
    private Map<String, Object> data = new HashMap<>();

    /**
     * Field is used to mark entities as deleted without physical deletion.
     */
    @ApiModelProperty(value = "Field is used to mark entities as deleted without physical deletion.")
    @Column(name = "removed")
    private Boolean removed;

    @OneToMany(mappedBy = "xmEntity", cascade = PERSIST)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Attachment> attachments = new HashSet<>();

    @OneToMany(mappedBy = "xmEntity", cascade = PERSIST)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Calendar> calendars = new HashSet<>();

    @OneToMany(mappedBy = "xmEntity", cascade = PERSIST)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Location> locations = new HashSet<>();

    @OneToMany(mappedBy = "xmEntity")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Rating> ratings = new HashSet<>();

    @Valid
    @OneToMany(mappedBy = "xmEntity", cascade = PERSIST)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "xmEntity")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Comment> comments = new HashSet<>();

    @OneToMany(mappedBy = "xmEntity")
    @JsonIgnore
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Vote> votes = new HashSet<>();

    @OneToMany(mappedBy = "target", cascade = PERSIST)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Link> sources = new HashSet<>();

    @OneToMany(mappedBy = "source", cascade = PERSIST)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Link> targets = new HashSet<>();

    @OneToMany(mappedBy = "xmEntity")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<XmFunction> functions = new HashSet<>();

    @OneToMany(mappedBy = "assigned")
    @JsonIgnore
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Event> events = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public XmEntity key(String key) {
        this.key = key;
        return this;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public XmEntity typeKey(String typeKey) {
        this.typeKey = typeKey;
        return this;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public String getStateKey() {
        return stateKey;
    }

    public XmEntity stateKey(String stateKey) {
        this.stateKey = stateKey;
        return this;
    }

    public void setStateKey(String stateKey) {
        this.stateKey = stateKey;
    }

    public String getName() {
        return name;
    }

    public XmEntity name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public XmEntity startDate(Instant startDate) {
        this.startDate = startDate;
        return this;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getUpdateDate() {
        return updateDate;
    }

    public XmEntity updateDate(Instant updateDate) {
        this.updateDate = updateDate;
        return this;
    }

    public void setUpdateDate(Instant updateDate) {
        this.updateDate = updateDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public XmEntity endDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public XmEntity avatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getDescription() {
        return description;
    }

    public XmEntity description(String description) {
        this.description = description;
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public XmEntity data(Map<String, Object> data) {
        this.data = data;
        return this;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Boolean isRemoved() {
        return removed;
    }

    public XmEntity removed(Boolean removed) {
        this.removed = removed;
        return this;
    }

    public void setRemoved(Boolean removed) {
        this.removed = removed;
    }

    public Set<Attachment> getAttachments() {
        return attachments;
    }

    public XmEntity attachments(Set<Attachment> attachments) {
        this.attachments = attachments;
        return this;
    }

    public XmEntity addAttachments(Attachment attachment) {
        this.attachments.add(attachment);
        attachment.setXmEntity(this);
        return this;
    }

    public XmEntity removeAttachments(Attachment attachment) {
        this.attachments.remove(attachment);
        attachment.setXmEntity(null);
        return this;
    }

    public void setAttachments(Set<Attachment> attachments) {
        this.attachments = attachments;
    }

    public Set<Calendar> getCalendars() {
        return calendars;
    }

    public XmEntity calendars(Set<Calendar> calendars) {
        this.calendars = calendars;
        return this;
    }

    public XmEntity addCalendars(Calendar calendar) {
        this.calendars.add(calendar);
        calendar.setXmEntity(this);
        return this;
    }

    public XmEntity removeCalendars(Calendar calendar) {
        this.calendars.remove(calendar);
        calendar.setXmEntity(null);
        return this;
    }

    public void setCalendars(Set<Calendar> calendars) {
        this.calendars = calendars;
    }

    public Set<Location> getLocations() {
        return locations;
    }

    public XmEntity locations(Set<Location> locations) {
        this.locations = locations;
        return this;
    }

    public XmEntity addLocations(Location location) {
        this.locations.add(location);
        location.setXmEntity(this);
        return this;
    }

    public XmEntity removeLocations(Location location) {
        this.locations.remove(location);
        location.setXmEntity(null);
        return this;
    }

    public void setLocations(Set<Location> locations) {
        this.locations = locations;
    }

    public Set<Rating> getRatings() {
        return ratings;
    }

    public XmEntity ratings(Set<Rating> ratings) {
        this.ratings = ratings;
        return this;
    }

    public XmEntity addRatings(Rating rating) {
        this.ratings.add(rating);
        rating.setXmEntity(this);
        return this;
    }

    public XmEntity removeRatings(Rating rating) {
        this.ratings.remove(rating);
        rating.setXmEntity(null);
        return this;
    }

    public void setRatings(Set<Rating> ratings) {
        this.ratings = ratings;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public XmEntity tags(Set<Tag> tags) {
        this.tags = tags;
        return this;
    }

    public XmEntity addTags(Tag tag) {
        this.tags.add(tag);
        tag.setXmEntity(this);
        return this;
    }

    public XmEntity removeTags(Tag tag) {
        this.tags.remove(tag);
        tag.setXmEntity(null);
        return this;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public Set<Comment> getComments() {
        return comments;
    }

    public XmEntity comments(Set<Comment> comments) {
        this.comments = comments;
        return this;
    }

    public XmEntity addComments(Comment comment) {
        this.comments.add(comment);
        comment.setXmEntity(this);
        return this;
    }

    public XmEntity removeComments(Comment comment) {
        this.comments.remove(comment);
        comment.setXmEntity(null);
        return this;
    }

    public void setComments(Set<Comment> comments) {
        this.comments = comments;
    }

    public Set<Vote> getVotes() {
        return votes;
    }

    public XmEntity votes(Set<Vote> votes) {
        this.votes = votes;
        return this;
    }

    public XmEntity addVotes(Vote vote) {
        this.votes.add(vote);
        vote.setXmEntity(this);
        return this;
    }

    public XmEntity removeVotes(Vote vote) {
        this.votes.remove(vote);
        vote.setXmEntity(null);
        return this;
    }

    public void setVotes(Set<Vote> votes) {
        this.votes = votes;
    }

    @JsonIgnore
    public Set<Link> getSources() {
        return sources;
    }

    public XmEntity sources(Set<Link> links) {
        this.sources = links;
        return this;
    }

    public XmEntity addSources(Link link) {
        this.sources.add(link);
        link.setTarget(this);
        return this;
    }

    public XmEntity removeSources(Link link) {
        this.sources.remove(link);
        link.setTarget(null);
        return this;
    }

    @JsonProperty
    public void setSources(Set<Link> links) {
        this.sources = links;
    }

    public Set<Link> getTargets() {
        return targets;
    }

    public XmEntity targets(Set<Link> links) {
        this.targets = links;
        return this;
    }

    public XmEntity addTargets(Link link) {
        this.targets.add(link);
        link.setSource(this);
        return this;
    }

    public XmEntity removeTargets(Link link) {
        this.targets.remove(link);
        link.setSource(null);
        return this;
    }

    public void setTargets(Set<Link> links) {
        this.targets = links;
    }

    public Set<XmFunction> getFunctions() {
        return functions;
    }

    public XmEntity functions(Set<XmFunction> xmFunctions) {
        this.functions = xmFunctions;
        return this;
    }

    public XmEntity addFunctions(XmFunction xmFunction) {
        this.functions.add(xmFunction);
        xmFunction.setXmEntity(this);
        return this;
    }

    public XmEntity removeFunctions(XmFunction xmFunction) {
        this.functions.remove(xmFunction);
        xmFunction.setXmEntity(null);
        return this;
    }

    public void setFunctions(Set<XmFunction> xmFunctions) {
        this.functions = xmFunctions;
    }

    public Set<Event> getEvents() {
        return events;
    }

    public XmEntity events(Set<Event> events) {
        this.events = events;
        return this;
    }

    public XmEntity addEvent(Event event) {
        this.events.add(event);
        event.setAssigned(this);
        return this;
    }

    public XmEntity removeEvent(Event event) {
        this.events.remove(event);
        event.setAssigned(null);
        return this;
    }

    public void setEvents(Set<Event> events) {
        this.events = events;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        XmEntity xmEntity = (XmEntity) o;
        if (xmEntity.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), xmEntity.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "XmEntity{" +
            "id=" + getId() +
            ", key='" + getKey() + "'" +
            ", typeKey='" + getTypeKey() + "'" +
            ", stateKey='" + getStateKey() + "'" +
            ", name='" + getName() + "'" +
            ", startDate='" + getStartDate() + "'" +
            ", updateDate='" + getUpdateDate() + "'" +
            ", endDate='" + getEndDate() + "'" +
            ", avatarUrl='" + getAvatarUrl() + "'" +
            ", description='" + getDescription() + "'" +
            ", data='" + getData() + "'" +
            ", removed='" + isRemoved() + "'" +
            "}";
    }
}
