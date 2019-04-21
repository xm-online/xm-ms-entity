package com.icthh.xm.ms.entity.lep.keyresolver;

import static java.lang.Boolean.TRUE;

import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TypeKeyWithExtends {

    private final XmEntityTenantConfigService tenantConfigService;

    public boolean doInheritance(String typeKey) {
        return TRUE.equals(tenantConfigService.getXmEntityTenantConfig().getLep().getEnableInheritanceTypeKey())
               && typeKey.lastIndexOf('.') > 0;
    }

    public String nextTypeKey(String typeKey) {
        int index = typeKey.lastIndexOf('.');
        return index < 0 ? typeKey : typeKey.substring(0, index);
    }
}
