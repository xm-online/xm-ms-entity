package com.icthh.xm.ms.entity.domain.spec;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DataSchema {
    private String key;
    private String schema;
    private String type; // using on ui: entity | definition
}
