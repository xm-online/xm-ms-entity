package com.icthh.xm.ms.entity.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.google.common.collect.Sets;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.UniqueFieldSpec;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.fge.jackson.NodeType.OBJECT;
import static com.github.fge.jackson.NodeType.getNodeType;

@Slf4j
public class DataSpecAnalyzer {
    private static final String XM_ENTITY_DEFINITION = "xmEntityDefinition";
    private static final String REF = "$ref";
    private static final String REF_PATTERN = "#/xmEntityDefinition/**/*";
    private AntPathMatcher matcher = new AntPathMatcher();

}
