package com.icthh.xm.ms.entity.util;

import com.icthh.xm.ms.entity.domain.spec.NextSpec;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@UtilityClass
public class TypeSpecTestUtils {

    public static List<StateSpec> newStateSpecs(List<String> states, List<String> nextStates, boolean filterSelf) {
        return states.stream().map(key -> {
            StateSpec stateSpec = new StateSpec();
            stateSpec.setKey(key);

            List<NextSpec> collect = newNextStates(key, nextStates, filterSelf);

            stateSpec.setNext(collect);
            return stateSpec;
        }).collect(Collectors.toList());
    }

    private static List<NextSpec> newNextStates(String currentState, List<String> next, boolean filterSelf) {
        return next.stream()
            .map(nextKey -> {
                if (filterSelf && currentState.equals(nextKey)) {
                    return null;
                }
                NextSpec nextSpec = new NextSpec();
                nextSpec.setStateKey(nextKey);
                return nextSpec;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

}
