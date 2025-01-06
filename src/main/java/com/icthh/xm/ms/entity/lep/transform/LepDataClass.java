package com.icthh.xm.ms.entity.lep.transform;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.codehaus.groovy.transform.GroovyASTTransformationClass;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@GroovyASTTransformationClass("com.icthh.xm.ms.entity.lep.transform.LepDataClassTransformation")
public @interface LepDataClass {}
