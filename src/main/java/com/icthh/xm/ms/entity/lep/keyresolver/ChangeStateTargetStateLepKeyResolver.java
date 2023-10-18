package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The {@link ChangeStateTargetStateLepKeyResolver} class.
 */
@Component
public class ChangeStateTargetStateLepKeyResolver implements LepKeyResolver {

    /**
     * Method parameter name for {@code stateKey}.
     */
    private static final String PARAM_NEXT_STATE_KEY = "nextStateKey";

    /**
     * Method parameter name for {@code xmEntityTypeKey}.
     */
    private static final String PARAM_TYPE_KEY = "xmEntityTypeKey";


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
            method.getParameter(PARAM_TYPE_KEY, String.class),
            method.getParameter(PARAM_NEXT_STATE_KEY, String.class)
        );
    }
}
