package com.icthh.xm.ms.entity.domain.function;

import com.icthh.xm.commons.domain.FunctionSpecWithFileName;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FunctionSpecDto extends FunctionSpecWithFileName<FunctionSpec> {

    private String entityTypeKey;

}
