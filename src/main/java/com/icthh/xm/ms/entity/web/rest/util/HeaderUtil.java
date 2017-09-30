package com.icthh.xm.ms.entity.web.rest.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

/**
 * Utility class for HTTP headers creation.
 */
public final class HeaderUtil {

    private static final Logger log = LoggerFactory.getLogger(HeaderUtil.class);

    private static final String APPLICATION_NAME = "entityApp";

    private HeaderUtil() {
    }

    public static HttpHeaders createAlert(String message, String param) {
        HttpHeaders headers = new HttpHeaders();
        // FIXME @amedvedchuk "X-" for custom headers is deprecated https://tools.ietf.org/html/rfc6648
        // See: com.icthh.xm.ms.entity.web.rest.XmRestApiConstants.XM_HEADER_CONTENT_NAME
        headers.add("X-entityApp-alert", message);
        headers.add("X-entityApp-params", param);
        return headers;
    }

    public static HttpHeaders createEntityCreationAlert(String entityName, String param) {
        return createAlert(APPLICATION_NAME + "." + entityName + ".created", param);
    }

    public static HttpHeaders createEntityUpdateAlert(String entityName, String param) {
        return createAlert(APPLICATION_NAME + "." + entityName + ".updated", param);
    }

    public static HttpHeaders createEntityDeletionAlert(String entityName, String param) {
        return createAlert(APPLICATION_NAME + "." + entityName + ".deleted", param);
    }

    public static HttpHeaders createFailureAlert(String entityName, String errorKey, String defaultMessage) {
        log.error("Entity processing failed, {}", defaultMessage);
        HttpHeaders headers = new HttpHeaders();
        // FIXME @amedvedchuk "X-" for custom headers is deprecated https://tools.ietf.org/html/rfc6648
        // See: com.icthh.xm.ms.entity.web.rest.XmRestApiConstants.XM_HEADER_CONTENT_NAME
        headers.add("X-entityApp-error", "error." + errorKey);
        headers.add("X-entityApp-params", entityName);
        return headers;
    }
}
