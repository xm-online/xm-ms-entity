package com.icthh.xm.ms.entity.service.swagger.model;

import com.icthh.xm.commons.swagger.model.SwaggerFunction;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class XmEntitySwaggerFunction extends SwaggerFunction {

    /**
     type: string, format: binary =>
     responseBody:
         content:
             custom/content-type:
                 schema:
                     type: string
                     format: binary
     */
    private String customBinaryDataType;

}
