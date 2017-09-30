package com.icthh.xm.ms.entity.repository.util;

public interface MappingStrategy {

    <T> T map(String json, Class<T> clazz);
}
