package com.icthh.xm.ms.entity.repository.search.db;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.exceptions.BusinessException;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import java.util.Map;
import lombok.experimental.UtilityClass;

/** Binds named JPQL parameters from a map; a parameter without a value is a client error (400). */
@UtilityClass
public class QueryParams {

    public static void bind(Query query, Map<String, Object> params) {
        for (Parameter<?> parameter : query.getParameters()) {
            String name = parameter.getName();
            if (name == null) {
                continue;
            }
            if (!params.containsKey(name)) {
                throw new BusinessException(ERR_VALIDATION, "Template param is required: " + name);
            }
            query.setParameter(name, params.get(name));
        }
    }
}
