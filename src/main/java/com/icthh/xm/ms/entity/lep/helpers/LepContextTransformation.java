package com.icthh.xm.ms.entity.lep.helpers;

import com.icthh.xm.ms.entity.lep.LepContext;
import org.apache.commons.lang.mutable.MutableLong;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.stream;
import static org.codehaus.groovy.control.CompilePhase.CANONICALIZATION;

@GroovyASTTransformation(phase = CANONICALIZATION)
public class LepContextTransformation extends AbstractASTTransformation {

    public final static Map<String, List<String>> LEP_CONTEXT_FIELDS;

    static {
        Map<String, List<String>> fields = getFields("", LepContext.class);
        fields.put(LepContext.class.getCanonicalName(), List.of("with{it}"));
        LEP_CONTEXT_FIELDS = Map.copyOf(fields);
    }

    private static Map<String, List<String>> getFields(String basePath, Class<?> type) {
        Set<Class<?>> memberClasses = new HashSet<>(List.of(type.getNestMembers()));
        Map<String, List<String>> fields = new HashMap<>();
        stream(type.getDeclaredFields())
                .filter(field -> !field.getType().isAssignableFrom(Object.class))
                .filter(field -> !memberClasses.contains(field.getType()))
                .forEach(field -> {
                    fields.putIfAbsent(field.getType().getCanonicalName(), new ArrayList<>());
                    fields.get(field.getType().getCanonicalName()).add(basePath + field.getName());
                });
        stream(type.getDeclaredFields())
                .filter(field -> memberClasses.contains(field.getType()))
                .forEach(field -> {
                    fields.putAll(getFields(field.getName() + ".", field.getType()));
                });
        return fields;
    }

    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode classNode = (ClassNode) nodes[1];
        StringBuilder body = new StringBuilder();
        MutableLong serviceIndex = new MutableLong(0);
        classNode.getFields().forEach(field -> {
            if (field.isFinal() && isInLepContext(field)) {
                String lepContextFieldName = getFieldName(field);
                body.append("this.").append(field.getName())
                        .append(" = lepContext.").append(lepContextFieldName)
                        .append("\n");
            } else if (field.isFinal() && isLepService(field)) {
                serviceIndex.increment();
                String serviceVarName = "service_" + serviceIndex.intValue();
                body.append("String ").append(serviceVarName).append(" = ")
                        .append("'").append(field.getType().getName()).append("'\n");
                body.append("this.").append(field.getName())
                        .append(" = Class.forName(").append(serviceVarName).append(").getDeclaredConstructor(Object.class).newInstance(lepContext)")
                        .append("\n");
            }
        });

        String mockClass = "class A { public A(Object lepContext) { \n " + body + " \n }  }";
        var ast = new AstBuilder().buildFromString(CANONICALIZATION, false, mockClass);
        classNode.addConstructor(((ClassNode) ast.get(1)).getDeclaredConstructors().get(0));
    }

    private boolean isLepService(FieldNode field) {
        return field.getType().getAnnotations().stream().anyMatch(it -> it.getClassNode().getName().equals(LepContextConstructor.class.getCanonicalName()));
    }

    private boolean isInLepContext(FieldNode field) {
        return LEP_CONTEXT_FIELDS.containsKey(field.getType().getName());
    }

    private String getFieldName(FieldNode field) {
        List<String> candidates = LEP_CONTEXT_FIELDS.get(field.getType().getName());
        String lepContextFieldName = candidates.get(0);
        if (candidates.size() > 1) {
            var nameMacheCandidates = candidates.stream()
                    .filter(name -> name.endsWith("." + field.getName()) || name.equals(field.getName()))
                    .findFirst();
            if (nameMacheCandidates.isPresent()) {
                lepContextFieldName = nameMacheCandidates.get();
            }
        }
        return lepContextFieldName;
    }

}
