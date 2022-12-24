package com.icthh.xm.ms.entity.lep.helpers;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import static com.icthh.xm.ms.entity.util.ResourceUtils.getResourceAsStr;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.codehaus.groovy.control.CompilePhase.CANONICALIZATION;
import static org.codehaus.groovy.control.CompilePhase.SEMANTIC_ANALYSIS;

@GroovyASTTransformation(phase = SEMANTIC_ANALYSIS)
public class LepDataClassTransformation extends AbstractASTTransformation {

    @Override
    @SneakyThrows
    public void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode classNode = (ClassNode) nodes[1];

        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("lep/DataClassTemplate.groovy");
        String mockClass = IOUtils.toString(resourceAsStream, UTF_8);
        var ast = new AstBuilder().buildFromString(CANONICALIZATION, false, mockClass);
        ClassNode mockClassNode = (ClassNode) ast.get(1);
        mockClassNode.getFields().forEach(classNode::addField);
        mockClassNode.getMethods().forEach(classNode::addMethod);
        mockClassNode.getDeclaredConstructors().forEach(classNode::addConstructor);
        Arrays.stream(mockClassNode.getInterfaces()).forEach(classNode::addInterface);
    }

}
