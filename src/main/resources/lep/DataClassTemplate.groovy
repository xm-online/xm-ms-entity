
class DataClassTemplate implements Map {

    protected transient def __data__ = [:]

    DataClassTemplate() {}

    DataClassTemplate(Map map) {
        this.__data__ = map
    }

    Object getProperty(String propertyName) {
        if (propertyName == '__data__') {
            return __data__
        } else {
            return __data__[propertyName]
        }
    }

    void setProperty(String propertyName, Object newValue) {
        if (propertyName == '__data__') {
            this.__data__ = newValue
        } else {
            __data__[propertyName] = newValue
        }
    }

    @Override
    int size() {
        return __data__.size()
    }

    @Override
    boolean isEmpty() {
        return __data__.isEmpty()
    }

    @Override
    boolean containsKey(Object key) {
        return __data__.containsKey(key)
    }

    @Override
    boolean containsValue(Object value) {
        return __data__.containsValue(value)
    }

    @Override
    Object get(Object key) {
        return __data__.get(key)
    }

    @Override
    Object put(Object key, Object value) {
        return __data__.put(key, value)
    }

    @Override
    Object remove(Object key) {
        return __data__.remove(key)
    }

    @Override
    void putAll(Map m) {
        __data__.putAll(m)
    }

    @Override
    void clear() {
        __data__.clear()
    }

    @Override
    Set keySet() {
        return __data__.keySet()
    }

    @Override
    Collection values() {
        return __data__.values()
    }

    @Override
    Set<Entry> entrySet() {
        return __data__.entrySet()
    }

}
