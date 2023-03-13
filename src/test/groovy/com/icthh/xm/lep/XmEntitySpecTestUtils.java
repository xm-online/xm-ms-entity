package com.icthh.xm.lep;

import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.json.JsonConfigurationListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.util.Pair;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.springframework.core.io.support.ResourcePatternUtils.getResourcePatternResolver;

@Slf4j
@Component
@RequiredArgsConstructor
public class XmEntitySpecTestUtils {
    private final XmEntitySpecService xmEntitySpecService;
    private final JsonConfigurationListener jsonConfigurationListener;
    private final ResourceLoader resourceLoader;

    @SneakyThrows
    public void refresh(String tenant) {
        String basePath = String.format("/config/tenants/%s/entity/", tenant);
        ResourcePatternResolver resourcePatternResolver = getResourcePatternResolver(resourceLoader);

        loadDefinitions(tenant, basePath, resourcePatternResolver);

        loadEntitySpec(tenant, basePath, resourcePatternResolver);
    }

    private void loadEntitySpec(String tenant, String basePath, ResourcePatternResolver resourcePatternResolver) throws IOException {
        String pattern = String.format("classpath*:config/tenants/%s/entity/xmentityspec/*.yml", tenant);
        Resource[] resources = resourcePatternResolver.getResources(pattern);
        List<Pair<String, String>> entitySpec = new ArrayList<>();
        for (var resource: resources) {
            walk(basePath + "xmentityspec/", entitySpec, resource.getFile());
        }

        String mainSpecPattern = String.format("classpath*:config/tenants/%s/entity/xmentityspec.yml", tenant);
        Resource[] mainSpecs = resourcePatternResolver.getResources(mainSpecPattern);
        for (var resource: mainSpecs) {
            entitySpec.add(Pair.create(basePath + "specs/xmentityspecs.yml", readFile(resource.getFile())));
        }

        System.out.println(entitySpec);

        entitySpec.forEach(f -> xmEntitySpecService.onRefresh(f.getKey(), f.getValue()));
        xmEntitySpecService.refreshFinished(entitySpec.stream().map(Pair::getKey).collect(toList()));
    }

    private void loadDefinitions(String tenant, String basePath, ResourcePatternResolver resourcePatternResolver) throws IOException {
        String pattern = String.format("classpath*:config/tenants/%s/entity/xmentityspec/*.json", tenant);
        Resource[] resources = resourcePatternResolver.getResources(pattern);
        List<Pair<String, String>> definitionsSpec = new ArrayList<>();
        for (var resource: resources) {
            walk(basePath + "xmentityspec/", definitionsSpec, resource.getFile());
        }
        definitionsSpec.forEach(f -> jsonConfigurationListener.onRefresh(f.getKey(), f.getValue()));
        xmEntitySpecService.refreshFinished(definitionsSpec.stream().map(Pair::getKey).collect(toList()));
    }

    @SneakyThrows
    private static void walk(String basePath, List<Pair<String, String>> entitySpec, File file) {
        if (file.isDirectory() && file.listFiles() != null) {
            Arrays.stream(file.listFiles()).forEach(it -> walk(basePath + file.getName() + "/", entitySpec, it));
        } else if (!file.isDirectory()) {
            entitySpec.add(Pair.create(basePath + file.getName(), readFile(file)));
        }
    }

    private static String readFile(File file) throws IOException {
        return IOUtils.toString(new FileInputStream(file), UTF_8);
    }

}
