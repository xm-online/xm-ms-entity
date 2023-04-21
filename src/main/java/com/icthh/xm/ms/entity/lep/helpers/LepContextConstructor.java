package com.icthh.xm.ms.entity.lep.helpers;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@GroovyASTTransformationClass("com.icthh.xm.ms.entity.lep.helpers.LepContextTransformation")
public @interface LepContextConstructor {
    boolean useLepFactory() default false;
}
