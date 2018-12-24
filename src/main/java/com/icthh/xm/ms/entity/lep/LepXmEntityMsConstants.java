package com.icthh.xm.ms.entity.lep;

/**
 * The {@link LepXmEntityMsConstants} class.
 */
public final class LepXmEntityMsConstants {

    public static final String BINDING_KEY_COMMONS = "commons";
    public static final String BINDING_KEY_SERVICES = "services";
    public static final String BINDING_SUB_KEY_SERVICE_XM_ENTITY = "xmEntity";
    public static final String BINDING_SUB_KEY_SERVICE_XM_TENANT_LC = "xmTenantLifeCycle";
    public static final String BINDING_SUB_KEY_SERVICE_PROFILE = "profileService";
    public static final String BINDING_SUB_KEY_SERVICE_LINK = "linkService";
    public static final String BINDING_SUB_KEY_SERVICE_ATTACHMENT = "attachmentService";
    public static final String BINDING_SUB_KEY_SERVICE_MAIL_SERVICE = "mailService";
    public static final String BINDING_SUB_KEY_SERVICE_TENANT_CONFIG_SERICE = "tenantConfigService";
    public static final String BINDING_SUB_KEY_SERVICE_LOCATION_SERVICE = "locationService";
    public static final String BINDING_SUB_KEY_SERVICE_TAG_SERVICE = "tagService";
    public static final String BINDING_SUB_KEY_PROFILE_EVENT_PRODUCER_SERVICE = "profileEventProducer";
    public static final String BINDING_SUB_KEY_SYSTEM_TOPIC_EVENT_PRODUCER_SERVICE = "systemTopicEventProducer";
    public static final String BINDING_SUB_KEY_COMMENT_SERVICE = "commentService";
    public static final String BINDING_SUB_KEY_PERMISSION_SERVICE = "permissionService";
    public static final String BINDING_SUB_KEY_SERVICE_EVENT_SERVICE = "eventService";
    public static final String BINDING_SUB_KEY_SERVICE_CALENDAR_SERVICE = "calendarService";
    public static final String BINDING_SUB_KEY_SERVICE_LEP_RESOURCE = "lepResource";

    public static final String BINDING_KEY_REPOSITORIES = "repositories";
    public static final String BINDING_SUB_KEY_REPOSITORY_XM_ENTITY = "xmEntity";
    public static final String BINDING_SUB_KEY_REPOSITORY_SEARCH = "xmEntitySearch";

    public static final String BINDING_KEY_TEMPLATES = "templates";
    public static final String BINDING_SUB_KEY_TEMPLATE_REST = "rest";

    private LepXmEntityMsConstants() {
        throw new UnsupportedOperationException("Prevent creation for constructor utils class");
    }

}
