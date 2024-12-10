package com.icthh.xm.ms.entity.service.processor;

import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.commons.processor.impl.FormSpecProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * This class extends exiting DataFormProcessor to support inverse relationship
 */
@Slf4j
@Component
public class XmEntityDataFormSpecProcessor extends FormSpecProcessor {

    public XmEntityDataFormSpecProcessor(JsonListenerService jsonListenerService) {
        super(jsonListenerService);
    }

    @Override
    public String getSectionName() {
        return "xmEntityForm";
    }
}
