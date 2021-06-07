/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.server.api.marshalling.json;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;
import com.fasterxml.jackson.databind.type.TypeParser;
import com.fasterxml.jackson.databind.util.ArrayBuilders;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.LookupCache;

public class FallbackableTypeFactory extends TypeFactory {

    private static final long serialVersionUID = 1L;

    protected final transient ClassLoader fallbackClassLoader;

    protected static final FallbackableTypeFactory instance = new FallbackableTypeFactory();

    private FallbackableTypeFactory() {
        super((LookupCache<Object,JavaType>) null);
        this.fallbackClassLoader = null;
    }

    protected FallbackableTypeFactory(LookupCache<Object, JavaType> typeCache) {
        super(typeCache);
        this.fallbackClassLoader = null;
    }

    protected FallbackableTypeFactory(LookupCache<Object, JavaType> typeCache, TypeParser p,
                                      TypeModifier[] mods, ClassLoader classLoader) {
        super(typeCache, p, mods, classLoader);
        this.fallbackClassLoader = null;
    }

    protected FallbackableTypeFactory(LookupCache<Object, JavaType> typeCache, TypeParser p,
                                      TypeModifier[] mods, ClassLoader classLoader, ClassLoader fallbackClassLoader) {
        super(typeCache, p, mods, classLoader);
        this.fallbackClassLoader = fallbackClassLoader;
    }

    /**
     * "Mutant factory" method which will construct a new instance with specified fallback
     * {@link ClassLoader} to use by {@link #findClass} only when main classloader or thread context classloader can not resolve.
     */
    public FallbackableTypeFactory withFallbackClassLoader(ClassLoader fallbackClassLoader) {
        return new FallbackableTypeFactory(_typeCache, _parser, _modifiers, _classLoader, fallbackClassLoader);
    }

    /**
     * "Mutant factory" method which will construct a new instance with specified
     * {@link TypeModifier} added as the first modifier to call (in case there
     * are multiple registered).
     */
    @Override
    public FallbackableTypeFactory withModifier(TypeModifier mod) {
        LookupCache<Object, JavaType> typeCache = _typeCache;
        TypeModifier[] mods;
        if (mod == null) { // mostly for unit tests
            mods = null;
            // 30-Jun-2016, tatu: for some reason expected semantics are to clear cache
            //    in this case; can't recall why, but keeping the same
            typeCache = null;
        } else if (_modifiers == null) {
            mods = new TypeModifier[]{mod};
            // 29-Jul-2019, tatu: Actually I think we better clear cache in this case
            //    as well to ensure no leakage occurs (see [databind#2395])
            typeCache = null;
        } else {
            // but may keep existing cache otherwise
            mods = ArrayBuilders.insertInListNoDup(_modifiers, mod);
        }
        return new FallbackableTypeFactory(typeCache, _parser, mods, _classLoader, fallbackClassLoader);
    }

    /**
     * "Mutant factory" method which will construct a new instance with specified
     * {@link ClassLoader} to use by {@link #findClass}.
     */
    @Override
    public FallbackableTypeFactory withClassLoader(ClassLoader classLoader) {
        return new FallbackableTypeFactory(_typeCache, _parser, _modifiers, classLoader, fallbackClassLoader);
    }

    /**
     * Mutant factory method that will construct new {@link TypeFactory} with
     * identical settings except for different cache; most likely one with
     * bigger maximum size.
     *
     * @since 2.12
     */
    @Override
    public FallbackableTypeFactory withCache(LookupCache<Object, JavaType> cache) {
        return new FallbackableTypeFactory(cache, _parser, _modifiers, _classLoader, fallbackClassLoader);
    }

    public static FallbackableTypeFactory defaultInstance() {
        return instance;
    }

    public ClassLoader getFallbackClassLoader() {
        return fallbackClassLoader;
    }

    /**
     * If TypeFactory.findClass() cannot find a Class, try with fallback classloader.
     * Main use case: Thread context classloader is KieURLClassLoader (dependency jar)
     *   but @class attribute (generated by @JsonTypeInfo(use = Id.CLASS)) may refers to a Class which exists in a different jar/kjar
     *   so ProjectClassLoader (or client application classloader) could be a fallback classloader
     */
    @Override
    public Class<?> findClass(String className) throws ClassNotFoundException {
        try {
            return super.findClass(className);
        } catch (ClassNotFoundException e) {
            if (fallbackClassLoader != null) {
                Throwable prob = null;
                try {
                    return classForName(className, true, fallbackClassLoader);
                } catch (Exception e1) {
                    prob = ClassUtil.getRootCause(e1);
                    throw new ClassNotFoundException(prob.getMessage(), prob);
                }
            }
            throw e;
        }
    }
}
