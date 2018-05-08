package com.icthh.xm.ms.entity.domain.spec;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Test;
import org.mockito.internal.util.Primitives;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
public class XmEntitySpecUnitTest {

    private static final Path SPEC_PATH = Paths.get("./src/test/resources/config/specs/xmentityspec-test.yml");

    private static final Set<String> SPEC_MAIN_FILES = loadListSpecificationFiles();

    private MutableInt countFields = new MutableInt();

    @SneakyThrows
    private static Set<String> loadListSpecificationFiles() {
        Reflections reflections = new Reflections("config.specs", new ResourcesScanner());
        Set<String> fileNames = reflections.getResources(Pattern.compile(".*\\.yml"));
        return fileNames;
    }

    @Test
    public void testParseXmEntitySpecFromYml() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        XmEntitySpec xmEntitySpec = mapper.readValue(new File(SPEC_PATH.toString()), XmEntitySpec.class);

        assertNotNull(xmEntitySpec);
        assertNotNull(xmEntitySpec.getTypes());
        assertEquals(5, xmEntitySpec.getTypes().size());
        assertEquals("TYPE1", xmEntitySpec.getTypes().get(0).getKey());
        assertEquals("TYPE2", xmEntitySpec.getTypes().get(1).getKey());
        assertEquals("TYPE1.SUBTYPE1", xmEntitySpec.getTypes().get(2).getKey());
        assertEquals("TYPE1-OTHER", xmEntitySpec.getTypes().get(3).getKey());
        assertEquals(1, xmEntitySpec.getTypes().get(0).getName().size());
        assertEquals(1, xmEntitySpec.getTypes().get(1).getName().size());
        assertEquals(1, xmEntitySpec.getTypes().get(1).getPluralName().size());
        assertEquals(2, xmEntitySpec.getTypes().get(2).getName().size());
        assertEquals(2, xmEntitySpec.getTypes().get(3).getName().size());
        assertNotNull(xmEntitySpec.getTypes().get(0).getDataSpec());
        assertNotNull(xmEntitySpec.getTypes().get(0).getDataForm());
        assertNotNull(xmEntitySpec.getTypes().get(0).getFastSearch());
        assertEquals(1, xmEntitySpec.getTypes().get(0).getFastSearch().size());
        assertEquals("typeKey:TYPE1*", xmEntitySpec.getTypes().get(0).getFastSearch().get(0).getQuery());
        assertFalse(xmEntitySpec.getTypes().get(0).getFastSearch().get(0).getName().isEmpty());
        assertEquals("NEW", xmEntitySpec.getTypes().get(0).getLinks().get(0).getBuilderType());
        assertEquals("Link to me", xmEntitySpec.getTypes().get(0).getLinks().get(0).getBackName().get("en"));
        assertNotNull("NEW", xmEntitySpec.getTypes().get(0).getLinks().get(0).getIcon());
        assertEquals("5STARS", xmEntitySpec.getTypes().get(2).getRatings().get(0).getStyle());

        FunctionSpec functionSpec = xmEntitySpec.getTypes().get(1).getFunctions().get(0);
        assertEquals("FUNCTION1", functionSpec.getKey());
        assertEquals("Function 1", functionSpec.getName().get("en"));
        assertEquals("Function Button 1", functionSpec.getActionName().get("en"));
        assertEquals(2, functionSpec.getAllowedStateKeys().size());
        assertEquals("{}", functionSpec.getInputSpec().trim());
        assertEquals("[]", functionSpec.getInputForm().trim());
        assertEquals(false, functionSpec.getWithEntityId());
        FunctionSpec functionSpec2 = xmEntitySpec.getTypes().get(1).getFunctions().get(1);
        assertEquals("FUNCTION2", functionSpec2.getKey());
        assertEquals("Function 2", functionSpec2.getName().get("en"));
        assertEquals("Function Button 2", functionSpec2.getActionName().get("en"));
        assertEquals(2, functionSpec2.getAllowedStateKeys().size());
        assertEquals(true, functionSpec2.getWithEntityId());
    }

    @Test
    @SneakyThrows
    public void testEquals() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        for(val fileName: SPEC_MAIN_FILES) {

            XmEntitySpec xmEntitySpecFirst = mapper.readValue(XmEntitySpecUnitTest.class.getClassLoader().getResourceAsStream(fileName), XmEntitySpec.class);
            XmEntitySpec xmEntitySpecSecond = mapper.readValue(XmEntitySpecUnitTest.class.getClassLoader().getResourceAsStream(fileName), XmEntitySpec.class);
            assertTrue(xmEntitySpecFirst.equals(xmEntitySpecSecond));

            countFields.setValue(0);
            xmEntitySpecFirst = (XmEntitySpec) wrapToNullFieldProxy(xmEntitySpecFirst);
            boolean hasFalseEquals = true;
            for (int i = 0; i < countFields.getValue(); i++) {
                hasFalseEquals = hasFalseEquals && xmEntitySpecFirst.equals(xmEntitySpecSecond);
            }
            assertFalse(hasFalseEquals);

            countFields.setValue(0);
            xmEntitySpecFirst = mapper.readValue(XmEntitySpecUnitTest.class.getClassLoader().getResourceAsStream(fileName), XmEntitySpec.class);
            xmEntitySpecFirst = (XmEntitySpec) wrapToNullFieldProxy(xmEntitySpecFirst);
            hasFalseEquals = true;
            for (int i = 0; i < countFields.getValue(); i++) {
                hasFalseEquals = hasFalseEquals && xmEntitySpecFirst.hashCode() == xmEntitySpecSecond.hashCode();
            }
            assertFalse(hasFalseEquals);
        }
    }


    @SneakyThrows
    private Object wrapToNullFieldProxy(Object object) {
        if (object == null) {
            return object;
        }

        if (Primitives.isPrimitiveOrWrapper(object.getClass()) || object instanceof String) {
            return object;
        }

        if (object instanceof Collection) {
            List list = new ArrayList<>();
            Collection collection = (Collection) object;
            list.addAll(collection);
            collection.clear();
            list.stream().map(this::wrapToNullFieldProxy).forEach(e -> collection.add(e));
            return collection;
        }

        if (object instanceof Map) {
            HashMap hashMap = new HashMap<>();
            Map map = (Map) object;
            hashMap.putAll(map);
            map.clear();
            hashMap.forEach((k, v) -> map.put(wrapToNullFieldProxy(k), wrapToNullFieldProxy(v)));
            return map;
        }

        for (val field: object.getClass().getDeclaredFields()) {
            if (Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            field.set(object, wrapToNullFieldProxy(field.get(object)));
            countFields.increment();
        }
        try {
            return Enhancer.create(object.getClass(), new Handler(object));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }


    static class Handler implements MethodInterceptor {
        private final Object object;
        private boolean firstCall = true;

        public Handler(Object object) {
            this.object = object;
        }

        public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            if(firstCall) {
                firstCall = false;
                return null;
            }
            return method.invoke(object, args);
        }
    }

    @Test
    public void testParseXmEntitySpecFromYmlForSeveralTenants() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        String[] tenants = {"test", "resinttest"};
        String configName;

        for (String tenant : tenants) {
            configName = "config/specs/xmentityspec-" + tenant.toLowerCase() + ".yml";
            InputStream cfgInputStream = new ClassPathResource(configName).getInputStream();
            XmEntitySpec xmEntitySpec = mapper.readValue(cfgInputStream, XmEntitySpec.class);
            assertNotNull(xmEntitySpec);
            xmEntitySpec.getTypes().forEach(typeSpec -> {
                if ("RESOURCE.CAR".equals(typeSpec.getKey())) {
                    verifyRating(typeSpec);
                } else if ("RESOURCE.CHARGING-STATION".equals(typeSpec.getKey())) {
                    verifyRating(typeSpec);
                }
            });
        }
    }
    private void verifyRating(TypeSpec typeSpec) {
        assertNotNull(typeSpec.getRatings());
        assertNotNull(typeSpec.getRatings().get(0).getKey());
        assertNotNull(typeSpec.getRatings().get(0).getName());
        assertNotNull(typeSpec.getRatings().get(0).getStyle());
        assertNotNull(typeSpec.getRatings().get(0).getVotes());
    }
}
