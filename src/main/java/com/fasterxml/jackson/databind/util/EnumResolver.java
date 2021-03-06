package com.fasterxml.jackson.databind.util;

import com.fasterxml.jackson.databind.AnnotationIntrospector;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Helper class used to resolve String values (either JSON Object field
 * names or regular String values) into Java Enum instances.
 */
public class EnumResolver<T extends Enum<T>>
{
    protected final Class<T> _enumClass;

    protected final T[] _enums;

    protected final HashMap<String, T> _enumsById;

    protected EnumResolver(Class<T> enumClass, T[] enums, HashMap<String, T> map)
    {
        _enumClass = enumClass;
        _enums = enums;
        _enumsById = map;
    }

    /**
     * Factory method for constructing resolver that maps from Enum.name() into
     * Enum value
     */
    public static <ET extends Enum<ET>> EnumResolver<ET> constructFor(Class<ET> enumCls, AnnotationIntrospector ai)
    {
        ET[] enumValues = enumCls.getEnumConstants();
        if (enumValues == null) {
            throw new IllegalArgumentException("No enum constants for class "+enumCls.getName());
        }
        HashMap<String, ET> map = new HashMap<String, ET>();
        for (ET e : enumValues) {
            map.put(ai.findEnumValue(e), e);
        }
        return new EnumResolver<ET>(enumCls, enumValues, map);
    }

    /**
     * Factory method for constructing resolver that maps from Enum.toString() into
     * Enum value
     */
    public static <ET extends Enum<ET>> EnumResolver<ET> constructUsingToString(Class<ET> enumCls)
    {
        ET[] enumValues = enumCls.getEnumConstants();
        HashMap<String, ET> map = new HashMap<String, ET>();
        // from last to first, so that in case of duplicate values, first wins
        for (int i = enumValues.length; --i >= 0; ) {
            ET e = enumValues[i];
            map.put(e.toString(), e);
        }
        return new EnumResolver<ET>(enumCls, enumValues, map);
    }    

    public static <ET extends Enum<ET>> EnumResolver<ET> constructUsingMethod(Class<ET> enumCls,
            Method accessor)
    {
        ET[] enumValues = enumCls.getEnumConstants();
        HashMap<String, ET> map = new HashMap<String, ET>();
        // from last to first, so that in case of duplicate values, first wins
        for (int i = enumValues.length; --i >= 0; ) {
            ET en = enumValues[i];
            try {
                Object o = accessor.invoke(en);
                if (o != null) {
                    map.put(o.toString(), en);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to access @JsonValue of Enum value "+en+": "+e.getMessage());
            }
        }
        return new EnumResolver<ET>(enumCls, enumValues, map);
    }    
    
    /**
     * This method is needed because of the dynamic nature of constructing Enum
     * resolvers.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static EnumResolver<?> constructUnsafe(Class<?> rawEnumCls, AnnotationIntrospector ai)
    {            
        /* This is oh so wrong... but at least ugliness is mostly hidden in just
         * this one place.
         */
        Class<Enum> enumCls = (Class<Enum>) rawEnumCls;
        return constructFor(enumCls, ai);
    }

    /**
     * Method that needs to be used instead of {@link #constructUsingToString}
     * if static type of enum is not known.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static EnumResolver<?> constructUnsafeUsingToString(Class<?> rawEnumCls)
    {            
        // oh so wrong... not much that can be done tho
        Class<Enum> enumCls = (Class<Enum>) rawEnumCls;
        return constructUsingToString(enumCls);
    }

    /**
     * Method used when actual String serialization is indicated using @JsonValue
     * on a method.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static EnumResolver<?> constructUnsafeUsingMethod(Class<?> rawEnumCls, Method accessor)
    {            
        // wrong as ever but:
        Class<Enum> enumCls = (Class<Enum>) rawEnumCls;
        return constructUsingMethod(enumCls, accessor);
    }
    
    public T findEnum(String key)
    {
        return _enumsById.get(key);
    }

    public T getEnum(int index)
    {
        if (index < 0 || index >= _enums.length) {
            return null;
        }
        return _enums[index];
    }

    public Class<T> getEnumClass() { return _enumClass; }

    public int lastValidIndex() { return _enums.length-1; }
}

