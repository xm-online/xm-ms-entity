package com.icthh.xm.ms.entity.lep.transform;


import static org.codehaus.groovy.ast.ClassHelper.STRING_TYPE;
import static org.codehaus.groovy.ast.tools.GeneralUtils.args;
import static org.codehaus.groovy.ast.tools.GeneralUtils.assignS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.block;
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.castX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.closureX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.ctorX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.hasDeclaredMethod;
import static org.codehaus.groovy.ast.tools.GeneralUtils.isInstanceOfX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.isNullX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.listX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.param;
import static org.codehaus.groovy.ast.tools.GeneralUtils.params;
import static org.codehaus.groovy.ast.tools.GeneralUtils.propX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.stmt;
import static org.codehaus.groovy.ast.tools.GeneralUtils.ternaryX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.apache.groovy.ast.tools.AnnotatedNodeUtils;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class LepDataClassTransformation extends AbstractASTTransformation {

    public static final String DOLLAR = "$";
    public static final ConstantExpression NULL_VALUE = ConstantExpression.NULL;

    private static final Logger log = LoggerFactory.getLogger(LepDataClassTransformation.class);

    public void visit(ASTNode[] nodes, SourceUnit source) {
        try {
            this.internalVisit(nodes);
        } catch (Throwable e) {
            log.error("Error during LepDataClassTransformation", e);
            throw e;
        }
    }

    private void internalVisit(ASTNode[] nodes) {

        if (nodes == null || nodes.length == 0
            || !(nodes[0] instanceof AnnotationNode annotation)
            || !(nodes[1] instanceof ClassNode annotatedClass)
            || annotatedClass.getFields() == null
            || !annotation.getClassNode().getNameWithoutPackage().equals("LepDataClass")
            || annotatedClass.isInterface()
            || annotatedClass.isEnum()
        ) {
            return;
        }

        addMapConstructor(annotatedClass);
    }

    @SneakyThrows
    private void addMapConstructor(ClassNode classNode) {
        boolean alreadyHasMapConstructor = hasMapTypeConstructor(classNode);
        if (alreadyHasMapConstructor) {
            throw new IllegalStateException("Class " + classNode.getName() + " already has a constructor with Map parameter");
        }


        Parameter mapParam = new Parameter(new ClassNode(Map.class), "data");
        BlockStatement body = new BlockStatement();

        for (FieldNode field : classNode.getFields()) {
            if (field.isStatic() || field.getName().contains(DOLLAR) || (field.getModifiers() & ACC_TRANSIENT) != 0) {
                continue;
            }

            ClassNode fieldType = field.getType();
            String fieldName = field.getName();
            PropertyExpression mapValue = propX(varX(mapParam), fieldName);
            Expression valueExp = createTransformToTypeExpression(fieldName, field, mapValue, fieldType);
            body.addStatement(
                assignS(propX(varX("this"), fieldName), valueExp)
            );
        }

        callMapCustomization(classNode, mapParam, body);

        classNode.addConstructor(
                MethodNode.ACC_PUBLIC,
                new Parameter[]{mapParam},
                ClassNode.EMPTY_ARRAY,
                body
        );
    }

    private void callMapCustomization(ClassNode classNode, Parameter mapParam, BlockStatement body) {
        if (classNode.hasMethod("fromMapCustomization", new Parameter[]{mapParam})) {
            body.addStatement(stmt(callX(varX("this"), "fromMapCustomization", args("data"))));
        }
    }

    private static boolean hasMapTypeConstructor(ClassNode classNode) {
        ClassNode mapType = ClassHelper.makeWithoutCaching(Map.class);
        return classNode.getDeclaredConstructors().stream().anyMatch(c ->
            c.getParameters() != null && c.getParameters().length == 1 && mapType.equals(c.getParameters()[0].getType()));
    }


    private Expression createTransformToTypeExpression(
        String fieldName,
        FieldNode fieldNode,
        Expression mapValue,
        ClassNode fieldType
    ) {

        // Java time
        if (isJavaTimeClass(fieldType)) {
            return transform(fieldName, fieldType, mapValue, "parse");
        }

        if (fieldType.isEnum()) {
            return transform(fieldName, fieldType, mapValue, "valueOf");
        }

        if (fieldType.isArray() && isLepDataClass(fieldType.getComponentType())) {
            return transformArray(fieldName, fieldNode, mapValue, fieldType);
        }

        ClassNode collection = ClassHelper.make(Iterable.class);
        if ((fieldType.implementsInterface(collection) || fieldType.equals(collection))) {
            ClassNode componentType = getGenericType(fieldNode, fieldType, "value", 0);
            if (componentType != null && isLepDataClass(componentType)) {
                return transformCollection(fieldName, fieldNode, mapValue, fieldType, componentType);
            }
        }

        ClassNode map = ClassHelper.make(Map.class);
        if ((fieldType.implementsInterface(map) || fieldType.equals(map))) {
            ClassNode keyType = getGenericType(fieldNode, fieldType, "key", 0);
            ClassNode valueType = getGenericType(fieldNode, fieldType, "value", 1);
            if (keyType != null && isLepDataClass(keyType) || valueType != null && isLepDataClass(valueType)) {
                return transformMap(fieldName, fieldNode, mapValue, fieldType, keyType, valueType);
            }
        }

        if (isLepDataClass(fieldType)) {
            return ternaryX(isNullX(mapValue), NULL_VALUE, castX(
                    fieldType,
                    ctorX(fieldType, args(mapValue))
                )
            );
        }

        return ternaryX(
            isInstanceOfX(mapValue, fieldType),
            mapValue,
            castX(fieldType, mapValue)
        );

    }

    private Expression transformMap(String fieldName, FieldNode fieldNode, Expression mapValue, ClassNode fieldType, ClassNode keyType, ClassNode valueType) {
        Parameter keyParam = param(ClassHelper.OBJECT_TYPE, "key");
        Parameter valueParam = param(ClassHelper.OBJECT_TYPE, "value");

        var keyMapper = createTransformToTypeExpression(fieldName, fieldNode, varX("key"), keyType);
        var valueMapper = createTransformToTypeExpression(fieldName, fieldNode, varX("value"), valueType);
        BlockStatement closureBody = block(returnS(listX(List.of(keyMapper, valueMapper))));
        closureBody.setVariableScope(new VariableScope());
        ClosureExpression closure = closureX(params(keyParam, valueParam), closureBody);
        closure.setVariableScope(new VariableScope());
        Expression resultMap = callX(mapValue, "collectEntries", args(closure));

        if (fieldType.isInterface() && fieldType.equals(ClassHelper.make(Map.class))) {
            return ctorX(ClassHelper.make(LinkedHashMap.class), args(resultMap));
        }
        return ctorX(fieldType, args(resultMap));
    }

    @Nullable
    private ClassNode getGenericType(FieldNode fieldNode, ClassNode fieldType, String annotationMember, int genericTypeNumber) {
        var annotated = AnnotatedNodeUtils.hasAnnotation(fieldNode, ClassHelper.make(LepDataClassField.class));
        if (annotated) {
            Expression value = fieldNode.getAnnotations(ClassHelper.make(LepDataClassField.class)).getFirst().getMember(annotationMember);
            return value.getType();
        }

        return Optional.of(fieldType)
            .map(ClassNode::getGenericsTypes)
            .filter(it -> it.length > genericTypeNumber)
            .map(it -> it[genericTypeNumber])
            .map(GenericsType::getType)
            .orElse(null);
    }

    private Expression transformCollection(String fieldName, FieldNode fieldNode, Expression mapValue, ClassNode fieldType, ClassNode componentType) {
        Expression mapped = mapIterable(fieldName, fieldNode, mapValue, componentType);
        if (fieldType.isInterface()) {
            if (fieldType.equals(ClassHelper.make(Set.class))) {
                return callX(mapped, "toSet");
            }
            return callX(mapped, "toList");
        }
        return ctorX(fieldType, args(mapped));
    }

    private Expression mapIterable(String fieldName, FieldNode fieldNode, Expression mapValue, ClassNode componentType) {
        Parameter itParam = param(ClassHelper.OBJECT_TYPE, "input");
        Statement mapper = stmt(
            createTransformToTypeExpression(fieldName, fieldNode, varX("input"), componentType)
        );
        BlockStatement closureBody = block(mapper);
        closureBody.setVariableScope(new VariableScope());
        ClosureExpression closure = closureX(params(itParam), closureBody);
        closure.setVariableScope(new VariableScope());
        return callX(mapValue, "collect", args(closure));
    }

    private static boolean isLepDataClass(ClassNode fieldType) {
        return !fieldType.getAnnotations(ClassHelper.make(LepDataClass.class)).isEmpty();
    }

    private Expression transformArray(String fieldName,
                                      FieldNode fieldNode,
                                      Expression mapValue,
                                      ClassNode fieldType) {
        Expression mapped = mapIterable(fieldName, fieldNode, mapValue, fieldType.getComponentType());
        return callX(mapped, "toArray");
    }

    private TernaryExpression transform(String fieldName, ClassNode fieldType, Expression mapValue, String builderMethod) {
        return ternaryX(isNullX(mapValue), propX(varX("this"), fieldName), ternaryX(
                isInstanceOfX(mapValue, STRING_TYPE),
                callX(fieldType, builderMethod, mapValue),
                ternaryX(isInstanceOfX(mapValue, fieldType), mapValue, castX(fieldType, mapValue))
            )
        );
    }

    private boolean isJavaTimeClass(ClassNode type) {
        if (!hasDeclaredMethod(type, "parse", 1)) {
            return false;
        }
        return type.getPackageName().startsWith("java.time");
    }


}
