package com.icthh.xm.ms.entity.domain.ext;

import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;


@RequiredArgsConstructor
@Getter
public enum TypeSpecParameter {
    ACCESS("access", TypeSpec::getAccess),
    ATTACHMENTS("attachments", TypeSpec::getAttachments),
    FUNCTIONS("functions", TypeSpec::getFunctions),
    CALENDARS("calendars", TypeSpec::getCalendars),
    LINKS("links", TypeSpec::getLinks),
    LOCATIONS("locations", TypeSpec::getLocations),
    RATINGS("ratings", TypeSpec::getRatings),
    STATES("states", TypeSpec::getStates),
    TAGS("tags", TypeSpec::getTags);

    private final String type;
    private final Function<TypeSpec, ?> parameterResolver;


}
