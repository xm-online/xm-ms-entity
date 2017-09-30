package com.icthh.xm.ms.entity.config.tenant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;

/**
 * Holds information about incoming user.
 */
@RequiredArgsConstructor
@Getter
public class TenantInfo {

    private final String tenant;
    private final String userLogin;
    private final String userKey;
}
