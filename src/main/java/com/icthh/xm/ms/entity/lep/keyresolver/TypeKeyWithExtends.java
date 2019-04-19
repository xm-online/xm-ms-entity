package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.commons.lep.XmExtensionService;
import com.icthh.xm.commons.lep.XmGroovyExecutionStrategy;
import com.icthh.xm.lep.api.LepKey;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.lep.api.commons.GroupMode;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.lep.api.commons.UrlLepResourceKey;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TypeKeyWithExtends {

    private final XmGroovyExecutionStrategy xmGroovyExecutionStrategy;
    private final XmExtensionService xmExtensionService;
    private final LepManager lepManager;

    public LepKey resolveWithTypeKeyInheritance(String typeKey, Function<String, LepKey> transform) {

        for (String currentTypeKey: generateTypeKeys(typeKey)) {
            LepKey lepKey = transform.apply(currentTypeKey);
            if (isLepKeyExists(lepKey)) {
                return lepKey;
            }
        }

        return transform.apply(typeKey);
    }

    public String[] resolveWithTypeKeyInheritance(SeparatorSegmentedLepKey baseKey, String typeKey, Function<String, String[]> transform) {
        GroupMode groupMode = new GroupMode.Builder().prefixAndIdIncludeGroup(baseKey.getGroupSegmentsSize()).build();

        for (String currentTypeKey: generateTypeKeys(typeKey)) {
            String[] segments = transform.apply(currentTypeKey);
            if (isLepKeyExists(baseKey.append(segments, groupMode))) {
                return segments;
            }
        }

        return transform.apply(typeKey);
    }

    private List<String> generateTypeKeys(String typeKey) {
        List<String> typeKeys = new ArrayList<>();
        typeKeys.add(typeKey);

        int index = 0;
        String currentTypeKey = typeKey;
        while((index = currentTypeKey.lastIndexOf('.')) >= 0 ) {
            currentTypeKey = currentTypeKey.substring(0, index);
            typeKeys.add(currentTypeKey);
        }

        return typeKeys;
    }

    private boolean isLepKeyExists(LepKey lepKey) {
        UrlLepResourceKey resourceKey = xmExtensionService.getResourceKey(lepKey, null);
        return !xmGroovyExecutionStrategy.getAvailableAtomicResourceKeys(resourceKey, lepManager).isEmpty();
    }
}
