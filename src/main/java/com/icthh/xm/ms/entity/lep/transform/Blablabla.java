package com.icthh.xm.ms.entity.lep.transform;


import groovyjarjarasm.asm.Opcodes;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.SneakyThrows;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Blablabla extends AbstractASTTransformation {

    public static final String DOLLAR = "$";
    public static AtomicInteger counter = new AtomicInteger(0);

    private static final Logger log = LoggerFactory.getLogger(Blablabla.class);

    public void visit(ASTNode[] nodes, SourceUnit source) {
        try {
            this.internalVisit(nodes, source);
        } catch (Throwable e) {
            log.error("Error during LepDataClassTransformation", e);
            throw e;
        }
    }

    private void internalVisit(ASTNode[] nodes, SourceUnit source) {

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

        addMapConstructor(annotatedClass, source);
    }

    @SneakyThrows
    private void addMapConstructor(ClassNode classNode, SourceUnit source) {
        boolean alreadyHasMapConstructor = hasMapTypeConstructor(classNode);
        if (alreadyHasMapConstructor) {
            throw new IllegalStateException("Class " + classNode.getName() + " already has a constructor with Map parameter");
        }


        Parameter mapParam = new Parameter(new ClassNode(Map.class), "data");
        BlockStatement body = new BlockStatement();

        for (FieldNode field : classNode.getFields()) {
            if (field.isStatic() || field.getName().contains(DOLLAR) || (field.getModifiers() & Opcodes.ACC_TRANSIENT) != 0) {
                continue;
            }

            ClassNode fieldType = field.getType();
            String fieldName = field.getName();
            PropertyExpression mapValue = new PropertyExpression(new VariableExpression(mapParam), fieldName);
            body.addStatement(new ExpressionStatement(
                    new BinaryExpression(
                            new PropertyExpression(new VariableExpression("this"), fieldName),
                            Token.newSymbol("=", field.getLineNumber(), field.getColumnNumber()),
                            createTransformToTypeExpression(field, mapValue, fieldType, source)
                    )
            ));
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
            body.addStatement(new ExpressionStatement(
                    new MethodCallExpression(
                            new VariableExpression("this"),
                            "fromMapCustomization",
                            new ArgumentListExpression(new VariableExpression("data"))
                    )
            ));
        }
    }

    private static boolean hasMapTypeConstructor(ClassNode classNode) {
        ClassNode mapType = ClassHelper.makeWithoutCaching(Map.class);
        return classNode.getDeclaredConstructors().stream().anyMatch(c ->
            c.getParameters() != null && c.getParameters().length == 1 && mapType.equals(c.getParameters()[0].getType()));
    }


    private Expression createTransformToTypeExpression(
            FieldNode fieldNode,
            Expression mapValue,
            ClassNode fieldType,
            SourceUnit source
    ) {

        // Java time
        if (isJavaTimeClass(fieldType)) {
            return createTimeTransform(fieldType, mapValue);
        }


//        // Enum
        if (fieldType.isEnum()) {
            return createEnumTransform(fieldType, mapValue);
        }

        // Collection or array
        if (fieldType.implementsInterface(ClassHelper.make(java.util.Collection.class)) || fieldType.isArray()) {
            return createCollectionTransform(fieldNode, mapValue, fieldType);
        }

        // Map
        if (fieldType.implementsInterface(ClassHelper.make(java.util.Map.class))) {
            return createMapTransform(fieldNode, mapValue, fieldType);
        }

        // Field is annotated with LepDataClassField
        if (!fieldNode.getAnnotations(ClassHelper.make(LepDataClassField.class)).isEmpty()) {
            return new CastExpression(
                    fieldType,
                    new MethodCallExpression(
                            mapValue,
                            "as",
                            new ArgumentListExpression(new ClassExpression(fieldType))
                    )
            );
        }

        // Field is annotated with LepDataClass
        if (!fieldNode.getAnnotations(ClassHelper.make(LepDataClass.class)).isEmpty()) {
            return new CastExpression(
                fieldType,
                new MethodCallExpression(
                    mapValue,
                    "as",
                    new ArgumentListExpression(new ClassExpression(fieldType))
                )
            );
        }
        return null;

    }

    private Expression createTransformToMapExpression(
            FieldNode fieldNode,
            Expression fieldValue,
            ClassNode fieldType,
            SourceUnit source
    ) {
        // Enum -> toString
        if (fieldType.isEnum()) {
            return new TernaryExpression(
                    new BooleanExpression(fieldValue),
                    new MethodCallExpression(fieldValue, "toString", ArgumentListExpression.EMPTY_ARGUMENTS),
                    ConstantExpression.NULL
            );
        }

        // Java time -> toString
        if (isJavaTimeClass(fieldType)) {
            return new TernaryExpression(
                    new BooleanExpression(fieldValue),
                    new MethodCallExpression(fieldValue, "toString", ArgumentListExpression.EMPTY_ARGUMENTS),
                    ConstantExpression.NULL
            );
        }

        // Collection or array -> collect
        if (fieldType.implementsInterface(ClassHelper.make(java.util.Collection.class)) || fieldType.isArray()) {
            // Use 'collect' with a closure-like approach
            return new TernaryExpression(
                    new BooleanExpression(fieldValue),
                    new MethodCallExpression(
                            fieldValue,
                            "collect",
                            new ArgumentListExpression(
                                    new ClosureExpression(Parameter.EMPTY_ARRAY, new BlockStatement(
                                            new Statement[]{
                                                    new ReturnStatement(
                                                            new MethodCallExpression(
                                                                    new VariableExpression("it"),
                                                                    "asType",
                                                                    new ArgumentListExpression(new ClassExpression(ClassHelper.make(Map.class)))
                                                            )
                                                    )
                                            },
                                            new org.codehaus.groovy.ast.VariableScope()
                                    ))
                            )
                    ),
                    ConstantExpression.NULL
            );
        }

        // Map -> asType Map
        if (fieldType.implementsInterface(ClassHelper.make(Map.class))) {
            return new TernaryExpression(
                    new BooleanExpression(fieldValue),
                    new MethodCallExpression(
                            fieldValue,
                            "asType",
                            new ArgumentListExpression(new ClassExpression(ClassHelper.make(Map.class)))
                    ),
                    ConstantExpression.NULL
            );
        }

        // Field annotated with LepDataClassField
        if (!fieldNode.getAnnotations(ClassHelper.make(LepDataClassField.class)).isEmpty()) {
            return new TernaryExpression(
                    new BooleanExpression(fieldValue),
                    new MethodCallExpression(
                            fieldValue,
                            "asType",
                            new ArgumentListExpression(new ClassExpression(ClassHelper.make(Map.class)))
                    ),
                    ConstantExpression.NULL
            );
        }

        // Field annotated with LepDataClass
        if (!fieldNode.getAnnotations(ClassHelper.make(LepDataClass.class)).isEmpty()) {
            return new MethodCallExpression(
                fieldValue,
                "asType",
                new ArgumentListExpression(new ClassExpression(ClassHelper.make(Map.class)))
            );
        }

        return fieldValue;
    }

    private Expression createEnumTransform(ClassNode fieldType, Expression mapValue) {
        VariableExpression enumVar = new VariableExpression("enumCandidate");
        BlockStatement block = new BlockStatement();

        // enumCandidate = mapValue
        block.addStatement(new ExpressionStatement(
                new DeclarationExpression(
                        enumVar,
                        Token.newSymbol("=", 0, 0),
                        mapValue
                )
        ));

        // return (enumCandidate.class == fieldType) ? enumCandidate : fieldType.valueOf(enumCandidate)
        TernaryExpression returnExpr = new TernaryExpression(
                new BooleanExpression(
                        new BinaryExpression(
                                new MethodCallExpression(enumVar, "getClass", ArgumentListExpression.EMPTY_ARGUMENTS),
                                Token.newSymbol("==", 0, 0),
                                new ClassExpression(fieldType)
                        )
                ),
                enumVar,
                new TernaryExpression(
                        new BooleanExpression(enumVar),
                        new StaticMethodCallExpression(fieldType, "valueOf", enumVar),
                        ConstantExpression.NULL
                )
        );

        block.addStatement(new ReturnStatement(returnExpr));
        return new ClosureExpression(Parameter.EMPTY_ARRAY, block);
    }

    private Expression createTimeTransform(
        ClassNode fieldType,
        Expression mapValue
    ) {
        // timeCandidate will hold the raw map value
        VariableExpression timeVar = new VariableExpression("timeCandidate$$$" + counter.incrementAndGet());

        // We'll build a code block, then wrap that block in a closure
        BlockStatement block = new BlockStatement();

        // timeCandidate = mapValue
        block.addStatement(new ExpressionStatement(
            new DeclarationExpression(
                timeVar,
                Token.newSymbol("=", 0, 0),
                mapValue
            )
        ));

        // Return the entire logic wrapped in a closure
        return new TernaryExpression(
            new BooleanExpression(
                new BinaryExpression(
                    timeVar,
                    Token.newSymbol("==", 0, 0),
                    ConstantExpression.NULL
                )
            ),
            ConstantExpression.NULL,
            new TernaryExpression(
                new BooleanExpression(
                    new BinaryExpression(
                        timeVar,
                        Token.newSymbol("instanceof", 0, 0),
                        new ClassExpression(ClassHelper.STRING_TYPE)
                    )
                ),
                new StaticMethodCallExpression(fieldType, "parse", mapValue),
                new TernaryExpression(
                    new BooleanExpression(
                        new BinaryExpression(
                            timeVar,
                            Token.newSymbol("instanceof", 0, 0),
                            new ClassExpression(fieldType)
                        )
                    ),
                    timeVar,
                    new CastExpression(fieldType, timeVar)
                )
            )
        );
    }


    private Expression createCollectionTransform(
            FieldNode fieldNode,
            Expression mapValue,
            ClassNode fieldType
    ) {
        Parameter[] closureParams = {new Parameter(ClassHelper.OBJECT_TYPE, "colVal")};
        ClosureExpression closure = new ClosureExpression(closureParams, new BlockStatement());

        // Determine target type for items
        ClassNode itemType = fieldType.getComponentType();
        if (itemType == null && !fieldNode.getAnnotations(ClassHelper.make(LepDataClassField.class)).isEmpty()) {
            Expression annoValue = fieldNode
                .getAnnotations(ClassHelper.make(LepDataClassField.class))
                .get(0)
                .getMember("value");
            if (annoValue instanceof ClassExpression) {
                ClassExpression ce = (ClassExpression) annoValue;
                itemType = ce.getType();
            }
        }
        if (itemType == null) {
            itemType = ClassHelper.OBJECT_TYPE;
        }

        BlockStatement closureBody = new BlockStatement();
        closure.setCode(closureBody);

        // return createTransformToTypeExpression(..., colVal, itemType, null)
        closureBody.addStatement(
                new ReturnStatement(
                        createTransformToTypeExpression(fieldNode, new VariableExpression("colVal"), itemType, null)
                )
        );

        return new TernaryExpression(
                new BooleanExpression(mapValue),
                new MethodCallExpression(
                        new CastExpression(ClassHelper.make(java.util.List.class), mapValue),
                        "collect",
                        new ArgumentListExpression(closure)
                ),
                ConstantExpression.NULL
        );
    }

    private Expression createMapTransform(
            FieldNode fieldNode,
            Expression mapValue,
            ClassNode fieldType
    ) {
        Parameter[] closureParams = {new Parameter(ClassHelper.make(Map.Entry.class), "e")};
        ClosureExpression closure = new ClosureExpression(closureParams, new BlockStatement());
        BlockStatement closureBody = new BlockStatement();

        // key = e.key
        // val = e.value
        // transformVal = ...
        // return new AbstractMap.SimpleEntry(key, transformVal)
        PropertyExpression keyExpr = new PropertyExpression(new VariableExpression("e"), "key");
        PropertyExpression valExpr = new PropertyExpression(new VariableExpression("e"), "value");
        ClassNode valType = ClassHelper.OBJECT_TYPE;

        if (!fieldNode.getAnnotations(ClassHelper.make(LepDataClassField.class)).isEmpty()) {
            Expression annoValue = fieldNode
                .getAnnotations(ClassHelper.make(LepDataClassField.class))
                .get(0)
                .getMember("value");
            if (annoValue instanceof ClassExpression) {
                ClassExpression ce = (ClassExpression) annoValue;
                valType = ce.getType();
            }
        }

        Expression transformVal = createTransformToTypeExpression(
                fieldNode, valExpr, valType, null
        );

        Expression mapEntryCtor = new org.codehaus.groovy.ast.expr.ConstructorCallExpression(
                ClassHelper.make(AbstractMap.SimpleEntry.class),
                new ArgumentListExpression(keyExpr, transformVal)
        );

        closureBody.addStatement(new ReturnStatement(mapEntryCtor));
        closure.setCode(closureBody);

        return new TernaryExpression(
                new BooleanExpression(mapValue),
                new MethodCallExpression(
                        new StaticMethodCallExpression(
                                ClassHelper.make(ArrayList.class),
                                "new",
                                ArgumentListExpression.EMPTY_ARGUMENTS
                        ),
                        "collect",
                        new ArgumentListExpression(
                                new MethodCallExpression(mapValue, "entrySet", ArgumentListExpression.EMPTY_ARGUMENTS),
                                closure
                        )
                ),
                ConstantExpression.NULL
        );
    }


    private boolean isJavaTimeClass(ClassNode type) {
//        if (type.getMethod("parse", new Parameter[]{new Parameter(ClassHelper.STRING_TYPE, "text")}) == null) {
//            return false;
//        }
        return type.getPackageName().startsWith("java.time");
    }
}
