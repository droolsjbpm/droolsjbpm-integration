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

package org.kie.server.services.taskassigning.planning.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractStringListValueAttributeMapValueExtractor<M extends Map<String, ?>, T>
        extends AbstractAttributeMapValueLabelValueExtractor<M, T> {

    public static final String COMMA_SEPARATOR = ",";

    protected final String separator;

    protected AbstractStringListValueAttributeMapValueExtractor(String attributeName, String separator, Class<T> type, String labelName, int priority) {
        super(attributeName, type, labelName, priority);
        this.separator = separator;
    }

    @Override
    protected Set<Object> extractFromAttribute(Object attributeValue) {
        if (attributeValue == null) {
            return new HashSet<>();
        } else {
            final String[] valueSplit = attributeValue.toString().split(separator);
            return Stream.of(valueSplit)
                    .map(StringUtils::trim)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toSet());
        }
    }
}
