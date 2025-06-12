package org.github.anarchael.requestplugin.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ClassManipulationUtils {

    private ClassManipulationUtils() {
        throw new UnsupportedOperationException();
    }

    public static <T> List<T> castList(Class<? extends T> clazz, Collection<?> rawCollection)
            throws ClassCastException {
        List<T> result;
        if (rawCollection == null) {
            result = new ArrayList<>();
        } else {
            result = new ArrayList<>(rawCollection.size());
            for (Object o : rawCollection) {
                result.add(clazz.cast(o));
            }
        }
        return result;
    }
}
