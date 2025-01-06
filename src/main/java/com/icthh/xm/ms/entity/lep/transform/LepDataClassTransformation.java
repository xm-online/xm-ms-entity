package com.icthh.xm.ms.entity.lep.transform;


import static org.codehaus.groovy.ast.ClassHelper.STRING_TYPE;
import static org.codehaus.groovy.ast.ClassHelper.make;
import static org.codehaus.groovy.ast.tools.GeneralUtils.args;
import static org.codehaus.groovy.ast.tools.GeneralUtils.assignS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.block;
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.castX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.closureX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.ctorX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.declS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.hasDeclaredMethod;
import static org.codehaus.groovy.ast.tools.GeneralUtils.ifS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.isInstanceOfX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.isNullX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.listX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.notX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.param;
import static org.codehaus.groovy.ast.tools.GeneralUtils.params;
import static org.codehaus.groovy.ast.tools.GeneralUtils.propX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.stmt;
import static org.codehaus.groovy.ast.tools.GeneralUtils.ternaryX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.apache.commons.lang3.function.TriFunction;
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
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
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
    public static final VariableExpression THIS = varX("this");

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
        addToMapMethod(annotatedClass);
    }


    @SneakyThrows
    private void addMapConstructor(ClassNode classNode) {
        boolean alreadyHasMapConstructor = hasMapTypeConstructor(classNode);
        if (alreadyHasMapConstructor) {
            log.error("Class {} already has a constructor with Map parameter", classNode.getName());
            throw new IllegalStateException("Class " + classNode.getName() + " already has a constructor with Map parameter");
        }

        if (!hasEmptyConstructor(classNode)) {
            classNode.addConstructor(MethodNode.ACC_PUBLIC, new Parameter[0], ClassNode.EMPTY_ARRAY, new BlockStatement());
        }

        Parameter mapParam = new Parameter(new ClassNode(Map.class), "data");
        BlockStatement body = new BlockStatement();

        // generate if in data == null then return
        body.addStatement(
            ifS(isNullX(varX("data")), new ReturnStatement(NULL_VALUE))
        );

        classNode.getFields().stream()
            .filter(this::isDataField)
            .forEach(field -> addFieldAssigment(field, mapParam, body));

        callMapCustomization(classNode, mapParam, body);

        classNode.addConstructor(
            MethodNode.ACC_PUBLIC,
            new Parameter[]{mapParam},
            ClassNode.EMPTY_ARRAY,
            body
        );
    }

    private void addFieldAssigment(FieldNode field, Parameter mapParam, BlockStatement body) {
        String fieldName = field.getName();
        PropertyExpression mapValue = propX(varX(mapParam), fieldName);
        Expression transformExp = createTransformToTypeExpression(field.getType(), mapValue, 0);
        Expression valueExp = ternaryX(isNullX(mapValue), fieldX(fieldName), transformExp); // support default value
        body.addStatement(
            assignS(fieldX(fieldName), valueExp)
        );
    }

    private void addToMapMethod(ClassNode annotatedClass) {
        if (annotatedClass.hasMethod("toMap", new Parameter[0])) {
            log.error("Class {} already has a method toMap", annotatedClass.getName());
            throw new IllegalStateException("Class " + annotatedClass.getName() + " already has a method toMap");
        }

        MethodNode method = new MethodNode(
            "toMap",
            ACC_PUBLIC,
            new ClassNode(Map.class),
            new Parameter[0],
            ClassNode.EMPTY_ARRAY,
            new BlockStatement()
        );

        BlockStatement body = (BlockStatement) method.getCode();
        body.addStatement(
            declS(varX("result"), ctorX(new ClassNode(LinkedHashMap.class)))
        );

        annotatedClass.getFields().stream()
            .filter(this::isDataField)
            .forEach(field -> addPutFieldToMapStatement(field, body));

        // map parameter
        if (annotatedClass.hasMethod("toMapCustomization", params(param(make(Map.class), "data")))) {
            body.addStatement(stmt(callX(THIS, "toMapCustomization")));
        }

        body.addStatement(returnS(varX("result")));

        annotatedClass.addMethod(method);
    }

    private boolean isDataField(FieldNode field) {
        return !field.isStatic() && !field.getName().contains(DOLLAR) && (field.getModifiers() & ACC_TRANSIENT) == 0 && !isIgnored(field);
    }

    private void addPutFieldToMapStatement(FieldNode field, BlockStatement body) {
        String fieldName = field.getName();
        body.addStatement(
            ifS(notX(isNullX(fieldX(fieldName))),
                assignS(
                    propX(varX("result"), fieldName),
                    transformFieldInToMap(field.getType(), fieldX(fieldName), 0)
                )
            )
        );
    }

    private Expression transformFieldInToMap(ClassNode fieldType, Expression fieldValue, int level) {
        return ternaryX(isNullX(fieldValue), NULL_VALUE, transformFieldInToMapInternal(fieldType, fieldValue, level + 1));
    }

    private Expression transformFieldInToMapInternal(ClassNode fieldType, Expression fieldValue, int level) {

        if (isJavaTimeClass(fieldType)) {
            return callX(fieldValue, "toString");
        }

        if (fieldType.isEnum()) {
            return callX(fieldValue, "name");
        }

        if (fieldType.isArray() && isLepDataClass(fieldType.getComponentType())) {
            return transformToMapArray(fieldType, fieldValue, level);
        }

        ClassNode collection = make(Iterable.class);
        if ((fieldType.implementsInterface(collection) || fieldType.equals(collection))) {
            ClassNode componentType = getGenericType(fieldType, 0);
            if (isLepDataClass(componentType)) {
                return transformCollectionInToMap(fieldType, fieldValue, componentType, level);
            }
        }

        ClassNode map = make(Map.class);
        if ((fieldType.implementsInterface(map) || fieldType.equals(map))) {
            ClassNode keyType = getGenericType(fieldType, 0);
            ClassNode valueType = getGenericType(fieldType, 1);
            if (isLepDataClass(keyType) || isLepDataClass(valueType)) {
                return transformMap(fieldType, fieldValue, keyType, valueType, level, this::transformFieldInToMap);
            }
        }

        if (isLepDataClass(fieldType)) {
            return callX(fieldValue, "toMap");
        }

        return fieldValue;
    }

    private Expression transformToMapArray(ClassNode fieldType, Expression fieldValue, int level) {
        Expression mapped = transformToMapIterable(fieldType.getComponentType(), fieldValue, level);
        return callX(mapped, "toList");
    }

    private Expression transformToMapIterable(ClassNode componentType, Expression fieldValue, int level) {
        String varName = "input" + level;
        return callX(fieldValue, "collect", args(builderClosure(stmt(
            ternaryX(
                isNullX(varX(varName)),
                NULL_VALUE,
                transformFieldInToMap(componentType, varX(varName), level)
            )
        ), varName)));
    }

    private PropertyExpression fieldX(String fieldName) {
        return propX(THIS, fieldName);
    }

    private boolean hasEmptyConstructor(ClassNode classNode) {
        return classNode.getDeclaredConstructors().stream().anyMatch(c -> c.getParameters() == null || c.getParameters().length == 0);
    }

    private void callMapCustomization(ClassNode classNode, Parameter mapParam, BlockStatement body) {
        if (classNode.hasMethod("fromMapCustomization", new Parameter[]{mapParam})) {
            body.addStatement(stmt(callX(THIS, "fromMapCustomization", args("data"))));
        }
    }

    private static boolean hasMapTypeConstructor(ClassNode classNode) {
        ClassNode mapType = ClassHelper.makeWithoutCaching(Map.class);
        return classNode.getDeclaredConstructors().stream().anyMatch(c ->
            c.getParameters() != null && c.getParameters().length == 1 && mapType.equals(c.getParameters()[0].getType()));
    }

    private Expression createTransformToTypeExpression(ClassNode fieldType, Expression mapValue, int level) {
        return ternaryX(isNullX(mapValue), mapValue, transformByType(mapValue, fieldType, level + 1));
    }

    private Expression transformByType(Expression mapValue, ClassNode fieldType, int level) {

        // Java time
        if (isJavaTimeClass(fieldType)) {
            return transform(fieldType, mapValue, "parse");
        }

        if (fieldType.isEnum()) {
            return transform(fieldType, mapValue, "valueOf");
        }

        if (fieldType.isArray() && isLepDataClass(fieldType.getComponentType())) {
            return transformArray(mapValue, fieldType.getComponentType(), level);
        }

        ClassNode collection = make(Iterable.class);
        if ((fieldType.implementsInterface(collection) || fieldType.equals(collection))) {
            ClassNode componentType = getGenericType(fieldType, 0);
            if (isLepDataClass(componentType)) {
                return transformCollection(fieldType, mapValue, componentType, level);
            }
        }

        ClassNode map = make(Map.class);
        if ((fieldType.implementsInterface(map) || fieldType.equals(map))) {
            ClassNode keyType = getGenericType(fieldType, 0);
            ClassNode valueType = getGenericType(fieldType, 1);
            if (isLepDataClass(keyType) || isLepDataClass(valueType)) {
                return transformMap(fieldType, mapValue, keyType, valueType, level, this::createTransformToTypeExpression);
            }
        }

        if (isLepDataClass(fieldType)) {
            return castX(fieldType, ctorX(fieldType, args(mapValue)));
        }

        return ternaryX(
            isInstanceOfX(mapValue, fieldType),
            mapValue,
            castX(fieldType, mapValue)
        );
    }

    private Expression transformMap(ClassNode fieldType, Expression mapValue, ClassNode keyType, ClassNode valueType, int level,
                                    TriFunction<ClassNode, Expression, Integer, Expression> mapper
                                    ) {
        String keyVariable = "key" + level;
        String valueVariable = "value" + level;

        Parameter keyParam = param(ClassHelper.OBJECT_TYPE, keyVariable);
        Parameter valueParam = param(ClassHelper.OBJECT_TYPE, valueVariable);

        var keyMapper = mapper.apply(keyType, varX(keyVariable), level);
        var valueMapper = mapper.apply(valueType, varX(valueVariable), level);
        BlockStatement closureBody = block(returnS(listX(List.of(keyMapper, valueMapper))));
        closureBody.setVariableScope(new VariableScope());
        ClosureExpression closure = closureX(params(keyParam, valueParam), closureBody);
        closure.setVariableScope(new VariableScope());
        Expression resultMap = callX(mapValue, "collectEntries", args(closure));

        if (fieldType.isInterface() && fieldType.equals(make(Map.class))) {
            return ctorX(make(LinkedHashMap.class), args(resultMap));
        }
        return ctorX(fieldType, args(resultMap));
    }

    @Nullable
    private ClassNode getGenericType(ClassNode fieldType, int genericTypeNumber) {
        return Optional.of(fieldType)
            .map(ClassNode::getGenericsTypes)
            .filter(it -> it.length > genericTypeNumber)
            .map(it -> it[genericTypeNumber])
            .map(GenericsType::getType)
            .orElse(null);
    }

    private Expression transformCollectionInToMap(ClassNode fieldType, Expression mapValue, ClassNode componentType, int level) {
        Expression mapped = transformToMapIterable(componentType, mapValue, level);
        return toCollection(fieldType, mapped);
    }

    private Expression transformCollection(ClassNode fieldType, Expression mapValue, ClassNode itemType, int level) {
        Expression mapped = mapIterable(mapValue, itemType, level);
        return toCollection(fieldType, mapped);
    }

    private static Expression toCollection(ClassNode fieldType, Expression mapped) {
        if (fieldType.isInterface()) {
            if (fieldType.equals(make(Set.class))) {
                return callX(mapped, "toSet");
            }
            return callX(mapped, "toList");
        }
        return ctorX(fieldType, args(mapped));
    }

    private Expression mapIterable(Expression fieldValue, ClassNode itemType, int level) {
        String varName = "input" + level;
        Statement mapper = stmt(
            createTransformToTypeExpression(itemType, varX(varName), level)
        );
        ClosureExpression closure = builderClosure(mapper, varName);
        return callX(fieldValue, "collect", args(closure));
    }

    private ClosureExpression builderClosure(Statement body, String inputName) {
        Parameter itParam = param(ClassHelper.OBJECT_TYPE, inputName);
        BlockStatement closureBody = block(body);
        closureBody.setVariableScope(new VariableScope());
        ClosureExpression closure = closureX(params(itParam), closureBody);
        closure.setVariableScope(new VariableScope());
        return closure;
    }

    private static boolean isLepDataClass(ClassNode fieldType) {
        return fieldType != null && !fieldType.getAnnotations(make(LepDataClass.class)).isEmpty();
    }

    private static boolean isIgnored(FieldNode field) {
        return !field.getAnnotations(make(LepDataClassIgnored.class)).isEmpty();
    }

    private Expression transformArray(Expression fieldValue, ClassNode itemType, int level) {
        Expression mapped = mapIterable(fieldValue, itemType, level);
        return callX(mapped, "toArray");
    }

    private TernaryExpression transform(ClassNode fieldType, Expression mapValue, String builderMethod) {
        return ternaryX(
                isInstanceOfX(mapValue, STRING_TYPE),
                callX(fieldType, builderMethod, mapValue),
                ternaryX(isInstanceOfX(mapValue, fieldType), mapValue, castX(fieldType, mapValue))
        );
    }

    private boolean isJavaTimeClass(ClassNode type) {
        if (!hasDeclaredMethod(type, "parse", 1)) {
            return false;
        }
        return type.getPackageName().startsWith("java.time");
    }


}
