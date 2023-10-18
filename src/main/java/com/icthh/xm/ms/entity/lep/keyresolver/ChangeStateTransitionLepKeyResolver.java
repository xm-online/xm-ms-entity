package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The {@link ChangeStateTransitionLepKeyResolver} class.
 */
@Component
public class ChangeStateTransitionLepKeyResolver implements LepKeyResolver {

    /**
     * LEP extension key specification:<br>
     * {@code ChangeState$$<xm-entity-type-key>$$<next-state-key>}
     * <p>
     * Add to method name {@code <xm-entity-type-key} and {@code <state-key>} to the end.
     *
     * @param method         method data on what LEP call occurs
     * @return dynamic value of LEP base key
     */

    @Override
    public List<String> segments(LepMethod method) {
        return List.of(
            method.getParameter("xmEntityTypeKey", String.class),
            method.getParameter("prevStateKey", String.class),
            method.getParameter("nextStateKey", String.class)
        );
    }
}
