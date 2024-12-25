package com.icthh.xm.ms.entity.lep.transform;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LepDataClassField {
    Class<?> key() default String.class;
    Class<?> value();
}
