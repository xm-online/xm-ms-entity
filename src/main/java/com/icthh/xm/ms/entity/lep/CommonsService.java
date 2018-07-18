package com.icthh.xm.ms.entity.lep;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.ms.entity.lep.keyresolver.FunctionLepKeyResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class CommonsService {

    @LogicExtensionPoint(value = "Commons", resolver = FunctionLepKeyResolver.class)
    public Object execute(String group, String name, Object args) {
        throw new NotImplementedException("Commons in package:" + group + " with name: Commons$$" + name + "$$around.groovy not found");
    }

}
