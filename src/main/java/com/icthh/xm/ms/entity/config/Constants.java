package com.icthh.xm.ms.entity.config;

/**
 * Application constants.
 */
public final class Constants {

    public static final String SYSTEM_ACCOUNT = "system";
    @SuppressWarnings("unused")
    public static final String ANONYMOUS_USER = "anonymoususer";
    public static final String AUTH_TENANT_KEY = "tenant";
    public static final String AUTH_USER_KEY = "user_key";
    public static final String AUTH_ROLE_KEY = "role_key";
    public static final String AUTH_AUTHORITIES = "authorities";
    public static final String AUTH_LOGINS_KEY = "logins";

    public static final String DDL_CREATE_SCHEMA = "CREATE SCHEMA IF NOT EXISTS %s";
    public static final String DB_SCHEMA_CREATION_ENABLED = "db.schema.creation.enabled";
    public static final String CHANGE_LOG_PATH = "classpath:config/liquibase/master.xml";
    public static final String TENANT_XM = "XM";
    public static final String HEADER_CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    public static final String HEADER_CONTENT_ID = "Content-ID";
    public static final String DOMAIN_PACKAGE = "com.icthh.xm.ms.entity.domain";
    public static final String ACCOUNT_TYPE_KEY = "ACCOUNT.USER";
    public static final String TENANT_TYPE_KEY = "RESOURCE.XM-TENANT";

    public static final String CERTIFICATE = "X.509";
    public static final String PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----%n%s%n-----END PUBLIC KEY-----";

    public static final String ACTIVATE_PROFILE = "ACTIVATE_PROFILE";
    public static final String CREATE_PROFILE = "CREATE_PROFILE";
    public static final String CHANGE_PASSWORD = "CHANGE_PASSWORD";
    public static final String UPDATE_PROFILE = "UPDATE_PROFILE";
    public static final String UPDATE_ACCOUNT = "UPDATE_ACCOUNT";
    public static final String UPDATE_ROLE = "UPDATE_ROLE";
    public static final String AUTH_ADDITIONAL_DETAILS = "additionalDetails";

    //System event data properties
    public static final String ID = "id";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String IMAGE_URL = "imageUrl";
    public static final String ACTIVATED = "activated";
    public static final String CREATED_DATE = "createdDate";
    public static final String LAST_MODIFIED_DATE = "lastModifiedDate";
    public static final String USER_KEY = "userKey";
    public static final String DATA = "data";

    public static final String ENTITY_CONFIG_PATH = "config/specs/default-xmentityspec.yml";
    public static final String WEBAPP_CONFIG_PATH = "config/webapp/default-settings-public.yml";

    public static final String PATH_SELF = "self";

    public static final String TRANSLATION_KEY = "trKey";

    public static final String FUNCTION_WITH_XM_ENTITY_PREFIX = "FunctionWithXmEntity";
    public static final String FUNCTION_PREFIX = "Function";

    private Constants() {
    }
}
