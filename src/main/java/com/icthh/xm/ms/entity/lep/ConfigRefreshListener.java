package com.icthh.xm.ms.entity.lep;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@Component
@LepService(group = "service.config")
public class ConfigRefreshListener implements RefreshableConfiguration {

    @Override
    @LogicExtensionPoint(value = "OnRefresh")
    public void onRefresh(String updatedKey, String config) {
        // only for lep
    }

    @Override
    @LogicExtensionPoint(value = "IsListeningConfiguration")
    public boolean isListeningConfiguration(String updatedKey) {
        return false;
    }

    @Override
    @LogicExtensionPoint(value = "RefreshFinished")
    public void refreshFinished(Collection<String> paths) {
        RefreshableConfiguration.super.refreshFinished(paths);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        onRefresh(configKey, configValue);
    }
}
