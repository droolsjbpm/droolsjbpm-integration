/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.api.model;

import java.util.HashSet;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.kie.server.api.marshalling.Marshaller;
import org.kie.server.api.marshalling.MarshallerFactory;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.marshalling.objects.Pojo1;
import org.kie.server.api.marshalling.objects.Pojo2;
import org.kie.server.api.marshalling.objects.Pojo3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarshallingRoundTripNestedClassesTest {

    private static final Logger logger = LoggerFactory.getLogger(MarshallingRoundTripNestedClassesTest.class);

    private static Pojo1 createTestObject() {
        Pojo3 pojo3 = new Pojo3("A");
        Pojo2 pojo2 = new Pojo2("B", true, pojo3);
        Pojo1 pojo1 = new Pojo1("C", pojo2);

        return pojo1;
    }

    private Set<Class<?>> getCustomClasses() {
        HashSet<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(Pojo1.class);
        classes.add(Pojo2.class);
        classes.add(Pojo3.class);
        return classes;
    }

    @Test
    public void testJaxb() {
        Marshaller marshaller = MarshallerFactory.getMarshaller(getCustomClasses(), MarshallingFormat.JAXB, getClass().getClassLoader());
        verifyMarshallingRoundTrip(marshaller, createTestObject());
    }

    @Test
    public void testXStream() {
        Marshaller marshaller = MarshallerFactory.getMarshaller(getCustomClasses(), MarshallingFormat.XSTREAM, getClass().getClassLoader());
        verifyMarshallingRoundTrip(marshaller, createTestObject());
    }

    @Test
    public void testJSON() {
        Marshaller marshaller = MarshallerFactory.getMarshaller(getCustomClasses(), MarshallingFormat.JSON, getClass().getClassLoader());
        verifyMarshallingRoundTrip(marshaller, createTestObject());
    }

    private void verifyMarshallingRoundTrip(Marshaller marshaller, Object inputObject) {
        String rawContent = marshaller.marshall(inputObject);
        logger.info(rawContent);
        Object testObjectAfterMarshallingTurnAround = marshaller.unmarshall(rawContent, inputObject.getClass());
        Assertions.assertThat(testObjectAfterMarshallingTurnAround).isEqualTo(inputObject);
    }

    @Test
    public void testJSONRawPayload() {
        // This payload is equivalent to testJSON()
        Marshaller marshaller = MarshallerFactory.getMarshaller(getCustomClasses(), MarshallingFormat.JSON, getClass().getClassLoader());
        String rawContent = "{\n" +
                            "  \"org.kie.server.api.marshalling.objects.Pojo1\" : {\n" +
                            "    \"desc\" : \"C\",\n" +
                            "    \"pojo2\" : {\n" +
                            "      \"org.kie.server.api.marshalling.objects.Pojo2\" : {\n" +
                            "        \"desc2\" : \"B\",\n" +
                            "        \"primitiveBoolean\" : true,\n" +
                            "        \"pojo3\" : {\n" +
                            "          \"org.kie.server.api.marshalling.objects.Pojo3\" : {\n" +
                            "            \"desc3\" : \"A\"\n" +
                            "          }\n" +
                            "        }\n" +
                            "      }\n" +
                            "    }\n" +
                            "  }\n" +
                            "}";

        Object unmarshalledObject = marshaller.unmarshall(rawContent, Pojo1.class);
        Assertions.assertThat(unmarshalledObject).isEqualTo(createTestObject());
    }

    @Test
    public void testJSONSimpleClassName() {
        Marshaller marshaller = MarshallerFactory.getMarshaller(getCustomClasses(), MarshallingFormat.JSON, getClass().getClassLoader());
        String rawContent = "{\n" +
                            "  \"Pojo1\" : {\n" +
                            "    \"desc\" : \"C\",\n" +
                            "    \"pojo2\" : {\n" +
                            "      \"Pojo2\" : {\n" +
                            "        \"desc2\" : \"B\",\n" +
                            "        \"primitiveBoolean\" : true,\n" +
                            "        \"pojo3\" : {\n" +
                            "          \"Pojo3\" : {\n" +
                            "            \"desc3\" : \"A\"\n" +
                            "          }\n" +
                            "        }\n" +
                            "      }\n" +
                            "    }\n" +
                            "  }\n" +
                            "}";

        Object unmarshalledObject = marshaller.unmarshall(rawContent, Pojo1.class);
        Assertions.assertThat(unmarshalledObject).isEqualTo(createTestObject());
    }

    @Test
    public void testJSONSimpleClassNameTopLevelOnly() {
        Marshaller marshaller = MarshallerFactory.getMarshaller(getCustomClasses(), MarshallingFormat.JSON, getClass().getClassLoader());
        String rawContent = "{\n" +
                            "  \"Pojo1\" : {\n" +
                            "    \"desc\" : \"C\",\n" +
                            "    \"pojo2\" : {\n" +
                            "        \"desc2\" : \"B\",\n" +
                            "        \"primitiveBoolean\" : true,\n" +
                            "        \"pojo3\" : {\n" +
                            "            \"desc3\" : \"A\"\n" +
                            "          }\n" +
                            "        }\n" +
                            "      }\n" +
                            "    }\n" +
                            "  }\n" +
                            "}";

        Object unmarshalledObject = marshaller.unmarshall(rawContent, Pojo1.class);
        Assertions.assertThat(unmarshalledObject).isEqualTo(createTestObject());
    }
}
