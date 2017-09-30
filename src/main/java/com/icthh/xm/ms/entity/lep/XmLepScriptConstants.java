package com.icthh.xm.ms.entity.lep;

/**
 * The {@link XmLepScriptConstants} class.
 */
public final class XmLepScriptConstants {

    public static final String BINDING_KEY_TENANT = "tenant";
    public static final String BINDING_KEY_TENANT_NAME = "tenantName";
    public static final String BINDING_KEY_IN_ARGS = "inArgs";
    public static final String BINDING_KEY_LEP = "lep";

    public static final String BINDING_KEY_SERVICES = "services";
    public static final String BINDING_SUB_KEY_SERVICE_XM_ENTITY = "xmEntity";
    public static final String BINDING_SUB_KEY_SERVICE_XM_TENANT_LC = "xmTenantLifeCycle";
    public static final String BINDING_KEY_REPOSITORIES = "repositories";
    public static final String BINDING_SUB_KEY_REPOSITORY_XM_ENTITY = "xmEntity";

    public static final String BINDING_VAR_LEP_SCRIPT_CONTEXT = "lepContext";

    private XmLepScriptConstants() {
        throw new UnsupportedOperationException("Prevent creation for constructor utils class");
    }

}
