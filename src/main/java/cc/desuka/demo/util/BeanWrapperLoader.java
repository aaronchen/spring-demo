package cc.desuka.demo.util;

import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/** Loads a key-value map into a typed POJO via Spring's {@link BeanWrapper}. */
public final class BeanWrapperLoader {

    private BeanWrapperLoader() {}

    /**
     * Creates a new instance of the target type and sets properties from the given key-value map.
     * Keys that don't match a writable property are silently ignored. Missing keys fall back to the
     * defaults defined in the POJO.
     */
    public static <T> T load(Map<String, String> keyValues, Supplier<T> factory) {
        T target = factory.get();
        BeanWrapper wrapper = new BeanWrapperImpl(target);
        keyValues.forEach(
                (key, value) -> {
                    if (wrapper.isWritableProperty(key)) {
                        wrapper.setPropertyValue(key, value);
                    }
                });
        return target;
    }
}
