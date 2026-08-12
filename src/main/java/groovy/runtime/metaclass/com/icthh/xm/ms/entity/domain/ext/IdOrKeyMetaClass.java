package groovy.runtime.metaclass.com.icthh.xm.ms.entity.domain.ext;

import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import groovy.lang.DelegatingMetaClass;
import groovy.lang.MetaBeanProperty;
import groovy.lang.MetaClass;
import groovy.lang.MetaMethod;
import groovy.lang.MetaProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps {@code idOrKey.id} / {@code idOrKey.key} in groovy leps resolving to {@link IdOrKey#getId()} /
 * {@link IdOrKey#getKey()} the way they did on groovy 2.
 *
 * <p>{@link IdOrKey} exposes two getters per property name - {@code getId()}/{@code isId()} and
 * {@code getKey()}/{@code isKey()}. When a class has both, groovy 2 bound the property to the
 * {@code getX()} getter, groovy 5 binds it to the boolean {@code isX()} one, so every tenant lep that
 * reads {@code idOrKey.id} silently started to get {@code true}/{@code false} instead of the id value.
 * The method signatures are part of the public java api used all over the services and cannot be
 * renamed, so the property binding is fixed on the groovy side instead.
 *
 * <p>No wiring is needed: groovy looks up {@code groovy.runtime.metaclass.<fqcn>MetaClass} while creating
 * the metaclass of a class (see {@code MetaClassRegistry.MetaClassCreationHandle#createWithCustomLookup}),
 * so this class is picked up by its name and package alone, for every lep engine and every classloader.
 *
 * <p>Note that this covers the dynamic groovy dispatch only - a {@code @CompileStatic} lep resolves the
 * property at compile time and still binds to {@code isId()}.
 */
public class IdOrKeyMetaClass extends DelegatingMetaClass {

    public IdOrKeyMetaClass(MetaClass delegate) {
        super(delegate);
        initialize();
    }

    @Override
    public Object getProperty(Object object, String property) {
        if (object instanceof IdOrKey idOrKey) {
            if ("id".equals(property)) {
                return idOrKey.getId();
            }
            if ("key".equals(property)) {
                return idOrKey.getKey();
            }
        }
        return super.getProperty(object, property);
    }

    @Override
    public MetaProperty getMetaProperty(String name) {
        MetaProperty getterProperty = valueProperty(name);
        return getterProperty != null ? getterProperty : super.getMetaProperty(name);
    }

    @Override
    public List<MetaProperty> getProperties() {
        List<MetaProperty> properties = new ArrayList<>(super.getProperties());
        properties.replaceAll(property -> {
            MetaProperty getterProperty = valueProperty(property.getName());
            return getterProperty != null ? getterProperty : property;
        });
        return properties;
    }

    /**
     * Property bound to the {@code getX()} getter for the names shadowed by an {@code isX()} one.
     */
    private MetaProperty valueProperty(String name) {
        if (!"id".equals(name) && !"key".equals(name)) {
            return null;
        }
        String getterName = "id".equals(name) ? "getId" : "getKey";
        MetaMethod getter = super.getMetaMethod(getterName, new Object[0]);
        return getter == null ? null : new MetaBeanProperty(name, getter.getReturnType(), getter, null);
    }
}
