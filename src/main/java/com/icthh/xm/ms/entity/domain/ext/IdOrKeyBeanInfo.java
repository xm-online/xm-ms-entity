package com.icthh.xm.ms.entity.domain.ext;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

public class IdOrKeyBeanInfo extends SimpleBeanInfo {

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            // 1. Force the 'key' property to strictly map to getKey(), with NO setter (null)
            PropertyDescriptor keyProp = new PropertyDescriptor("key", IdOrKey.class, "getKey", null);

            // 2. Map the 'id' property so idOrKey.id still works
            PropertyDescriptor idProp = new PropertyDescriptor("id", IdOrKey.class, "getId", null);

            // 3. Map 'self' so idOrKey.self works (maps to isSelf())
            PropertyDescriptor selfProp = new PropertyDescriptor("self", IdOrKey.class, "isSelf", null);

            // Return only the properties we want Groovy/Java to recognize via dot-notation
            return new PropertyDescriptor[] { keyProp, idProp, selfProp };

        } catch (IntrospectionException e) {
            // If something goes wrong, fallback to default behavior
            return super.getPropertyDescriptors();
        }
    }
}
