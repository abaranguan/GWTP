/*
 * Copyright 2013 ArcBees Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gwtplatform.dispatch.rest.processors.resolvers.parameters;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.ServiceLoader;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.gwtplatform.dispatch.rest.shared.HttpParameter.Type;
import com.gwtplatform.processors.tools.logger.Logger;
import com.gwtplatform.processors.tools.utils.Utils;

import static com.google.auto.common.MoreElements.getAnnotationMirror;
import static com.google.auto.common.MoreElements.isAnnotationPresent;
import static com.google.common.collect.Maps.filterKeys;

public abstract class HttpParamValueResolver {
    private static ServiceLoader<HttpParamValueResolver> serviceLoader;

    protected Utils utils;
    protected Logger logger;

    private boolean initialized;

    public static Iterable<HttpParamValueResolver> getResolvers(final Logger logger, final Utils utils) {
        if (serviceLoader == null) {
            Class<HttpParamValueResolver> clazz = HttpParamValueResolver.class;
            serviceLoader = ServiceLoader.load(clazz, clazz.getClassLoader());
        }

        // The FluentIterable will lazily initialize resolvers
        return FluentIterable.from(serviceLoader)
                .transform(resolver -> {
                    if (!resolver.initialized) {
                        resolver.init(utils, logger);
                    }
                    return resolver;
                });
    }

    private void init(Utils utils, Logger logger) {
        this.utils = utils;
        this.logger = logger;
        this.initialized = true;

        init();
    }

    protected void init() {
    }

    public abstract Class<? extends Annotation> getAssociatedClass();

    public abstract Type getAssociatedType();

    public boolean isPresent(VariableElement element) {
        return isAnnotationPresent(element, getAssociatedClass());
    }

    public String resolve(VariableElement element) {
        AnnotationMirror annotationMirror = getAnnotationMirror(element, getAssociatedClass()).get();
        Collection<? extends AnnotationValue> values =
                filterKeys(annotationMirror.getElementValues(), new Predicate<ExecutableElement>() {
                    @Override
                    public boolean apply(ExecutableElement executableElement) {
                        return executableElement.getSimpleName().contentEquals("value");
                    }
                }).values();

        return Iterables.getOnlyElement(values).getValue().toString();
    }
}
