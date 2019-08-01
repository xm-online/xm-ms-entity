package com.icthh.xm.ms.entity.domain.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.Map;

/**
 * XM Application fast search configuration item.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "query", "name", "code" })
@Data
public class FastSearchSpec {

    /** Elastic search query string */
    @JsonProperty("query")
    private String query;

    /** Localized action name via map where key is ISO 639-1 code */
    @JsonProperty("name")
    private Map<String, String> name;

    /** Property will be used for UI routing f.e. app/ACCOUNT?filterState=%code%. Parameter is optional and if not defined, UI will generate own runtime code*/
    @JsonProperty("code")
    private String code;

}
