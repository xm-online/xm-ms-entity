package com.icthh.xm.ms.entity.lep;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@LepService(group = "service.config")
public class ConfigRefreshListener implements RefreshableConfiguration {

    @Override
    @LogicExtensionPoint(value = "OnRefresh")
    public void onRefresh(String updatedKey, String config) {
        // only for lep
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return true;
    }

    @Override
    public void onInit(String configKey, String configValue) {
        onRefresh(configKey, configValue);
    }
}
