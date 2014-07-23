package org.ambraproject.rhino.util;

import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.FieldFilter;

import java.lang.reflect.Field;

import static org.springframework.util.ReflectionUtils.doWithFields;

/**
 * Created by jkrzemien on 7/15/14.
 */


public class TestReflectionUtils {

    public static void setField(final Object clazzInstance, final String fieldName, final Object newValue) {
        doWithFields(clazzInstance.getClass(),
                new FieldCallback() {
                    @Override
                    public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                        field.setAccessible(true);
                        field.set(clazzInstance, newValue);
                        field.setAccessible(false);
                    }
                }, new FieldFilter() {
                    @Override
                    public boolean matches(final Field field) {
                        return fieldName.equals(field.getName());
                    }
                });
    }

}
