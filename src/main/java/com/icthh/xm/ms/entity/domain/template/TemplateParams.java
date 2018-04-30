package com.icthh.xm.ms.entity.domain.template;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class TemplateParams {
    private Map<String, String> templateParams = new HashMap<>();
}
