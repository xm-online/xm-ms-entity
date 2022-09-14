package com.icthh.xm.ms.entity.service.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jackson.JsonLoader;
import com.icthh.xm.ms.entity.service.JsonListenerService;
import lombok.SneakyThrows;
import org.springframework.util.AntPathMatcher;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class SpecProcessor {

    protected static final String REF = "$ref";
    protected final AntPathMatcher matcher;
    protected final ObjectMapper mapper;
    protected final JsonListenerService jsonListenerService;

    public SpecProcessor(JsonListenerService jsonListenerService) {
        this.jsonListenerService = jsonListenerService;
        this.matcher = new AntPathMatcher();
        this.mapper = new ObjectMapper(new YAMLFactory());
    }

    public abstract void processTypeSpec(String tenant, Consumer<String> setter, Supplier<String> getter);

    @SneakyThrows
    protected Set<String> findDataSpecReferencesByPattern(String dataSpec, String refPattern) {
        return JsonLoader.fromString(dataSpec)
            .findValuesAsText(REF)
            .stream()
            .filter(value -> matcher.matchStart(refPattern, value))
            .collect(Collectors.toSet());
    }
}
