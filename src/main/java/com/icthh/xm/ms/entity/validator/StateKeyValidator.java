package com.icthh.xm.ms.entity.validator;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Slf4j
public class StateKeyValidator implements ConstraintValidator<StateKey, XmEntity> {

    @Autowired
    private XmEntitySpecService xmEntitySpecService;

    @Override
    public void initialize(StateKey constraintAnnotation) {
        log.trace("State key validator initialized");
    }

    @Override
    public boolean isValid(XmEntity value, ConstraintValidatorContext context) {

        if (value.getStateKey() == null) {
            return true;
        }

        List<StateSpec> stateSpecs = xmEntitySpecService.getTypeSpecByKey(value.getTypeKey())
            .map(TypeSpec::getStates)
            .orElse(List.of());

        if (stateSpecs.isEmpty()) {
            return true;
        }

        Set<String> stateKeys = stateSpecs.stream().map(StateSpec::getKey).collect(toSet());
        log.debug("Type specification states {}, checked state {}", stateKeys, value.getStateKey());
        return stateKeys.contains(value.getStateKey());
    }

}
