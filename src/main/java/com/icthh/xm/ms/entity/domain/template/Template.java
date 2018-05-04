package com.icthh.xm.ms.entity.domain.template;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "key")
@JsonPropertyOrder( {"key", "query"})
public class Template implements Comparable<Template> {

    private String key;
    private String query;

    @Override
    public int compareTo(Template o) {
        return key.compareTo(o.getKey());
    }
}
