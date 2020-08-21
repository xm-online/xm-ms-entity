package com.icthh.xm.ms.entity.service.privileges.custom;

import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;

@Slf4j
@Component
public class ApplicationCustomPrivilegesExtractor implements CustomPrivilegesExtractor {

    private static final String APPLICATION_SECTION_NAME = "applications";
    private static final String APPLICATION_PRIVILEGE_PREFIX = "APPLICATION.";

    @Override
    public String getSectionName() {
        return APPLICATION_SECTION_NAME;
    }

    @Override
    public String getPrivilegePrefix() {
        return APPLICATION_PRIVILEGE_PREFIX;
    }

    @Override
    public List<String> toPrivilegesList(Map<String, TypeSpec> specs) {
        return specs.values().stream().filter(it -> TRUE.equals(it.getIsApp()))
                                      .map(TypeSpec::getKey)
                                      .collect(toList());
    }

}
