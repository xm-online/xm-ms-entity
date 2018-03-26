package com.icthh.xm.ms.entity.repository.search.translator;

import com.icthh.xm.commons.permission.access.subject.Subject;
import com.icthh.xm.commons.permission.service.translator.SpelTranslator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpelToElasticTranslator implements SpelTranslator {

    @Override
    public String translate(String spel, Subject subject) {
        if (StringUtils.isNotBlank(spel)) {

            String dsl = StringUtils.replaceAll(spel, "#returnObject.", "");
            dsl = replaceOperators(dsl);
            dsl = SpelTranslator.applySubject(dsl, subject);

            log.debug("SpEL was translated to Elastic DSL for permission filtering: [{}] --> [{}]", spel, dsl);
            return dsl;
        }
        return spel;
    }

    /**
     * Replace SPEL ==, &&, || to SQL =, and, or .
     * @param spel the spring expression
     * @return sql expression
     */
    private static String replaceOperators(String spel) {
        if (StringUtils.isBlank(spel)) {
            return spel;
        }
        return spel.replaceAll("==", ":")
            .replaceAll("&&", " and ")
            .replaceAll("\\|\\|", " or ");
    }
}
