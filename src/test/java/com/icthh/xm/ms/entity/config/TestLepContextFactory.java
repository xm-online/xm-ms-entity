package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepContextFactory;
import com.icthh.xm.commons.lep.processor.GroovyMap;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.ms.entity.lep.LepContext;
import com.icthh.xm.ms.entity.service.XmEntityService;
import org.springframework.beans.factory.annotation.Autowired;

public class TestLepContextFactory implements LepContextFactory {

    @Autowired
    private XmEntityService xmEntityService;

    @Override
    public BaseLepContext buildLepContext(LepMethod lepMethod) {
        TestLepContext testLepContext = new TestLepContext();
        testLepContext.services.xmEntity = xmEntityService;
        return testLepContext;
    }
}
