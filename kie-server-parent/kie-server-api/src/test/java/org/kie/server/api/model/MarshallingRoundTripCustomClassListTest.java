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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.kie.server.api.marshalling.Marshaller;
import org.kie.server.api.marshalling.MarshallerFactory;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.marshalling.objects.PojoA;
import org.kie.server.api.marshalling.objects.PojoB;
import org.kie.server.api.marshalling.objects.PojoC;

public class MarshallingRoundTripCustomClassListTest {

    private static PojoA createTestObject() {
        PojoA pojoA = new PojoA("A");
        PojoB pojoB1 = new PojoB("B1");
        PojoB pojoB2 = new PojoB("B2");
        PojoC pojoC1 = new PojoC("C1");
        PojoC pojoC2 = new PojoC("C2");
        PojoC pojoC3 = new PojoC("C3");

        List<PojoC> pojoCList1 = new ArrayList<PojoC>();
        pojoCList1.add(pojoC1);
        pojoCList1.add(pojoC2);
        pojoB1.setPojoCList(pojoCList1);

        List<PojoC> pojoCList2 = new ArrayList<PojoC>();
        pojoCList2.add(pojoC3);
        pojoB2.setPojoCList(pojoCList2);

        List<PojoB> pojoBList = new ArrayList<PojoB>();
        pojoBList.add(pojoB1);
        pojoBList.add(pojoB2);
        pojoA.setPojoBList(pojoBList);

        List<String> stringList = new ArrayList<String>();
        stringList.add("Hello");
        stringList.add("Bye");
        pojoA.setStringList(stringList);

        return pojoA;
    }

    private Set<Class<?>> getCustomClasses() {
        HashSet<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(PojoA.class);
        classes.add(PojoB.class);
        classes.add(PojoC.class);
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
        Object testObjectAfterMarshallingTurnAround = marshaller.unmarshall(rawContent, inputObject.getClass());
        Assertions.assertThat(testObjectAfterMarshallingTurnAround).isEqualTo(inputObject);
    }

    @Test
    public void testJSONTypeInfoTopLevelOnly() {
        Marshaller marshaller = MarshallerFactory.getMarshaller(getCustomClasses(), MarshallingFormat.JSON, getClass().getClassLoader());
        String rawContent = "{\"org.kie.server.api.marshalling.objects.PojoA\": "
                + "{\"name\": \"A\","
                + " \"pojoBList\":"
                + " [{\"name\": \"B1\","
                + "   \"pojoCList\":"
                + "    ["
                + "      {\"name\": \"C1\"}, "
                + "      {\"name\": \"C2\"}"
                + "    ]"
                + "  },"
                + "  {\"name\": \"B2\","
                + "   \"pojoCList\":"
                + "    ["
                + "     {\"name\": \"C3\"}"
                + "    ]"
                + "  }"
                + " ],"
                + " \"stringList\":"
                + "  [\"Hello\", \"Bye\"]"
                + "}}";

        Object unmarshalledObject = marshaller.unmarshall(rawContent, PojoA.class);
        Assertions.assertThat(unmarshalledObject).isEqualTo(createTestObject());
    }

    @Test
    public void testJSONRawPayload() {
        // This payload is equivalent to testJSON()
        Marshaller marshaller = MarshallerFactory.getMarshaller(getCustomClasses(), MarshallingFormat.JSON, getClass().getClassLoader());
        String rawContent = "{\n" +
                            "  \"org.kie.server.api.marshalling.objects.PojoA\" : {\n" +
                            "    \"name\" : \"A\",\n" +
                            "    \"pojoBList\" : [ {\n" +
                            "      \"org.kie.server.api.marshalling.objects.PojoB\" : {\n" +
                            "        \"name\" : \"B1\",\n" +
                            "        \"pojoCList\" : [ {\n" +
                            "          \"org.kie.server.api.marshalling.objects.PojoC\" : {\n" +
                            "            \"name\" : \"C1\"\n" +
                            "          }\n" +
                            "        }, {\n" +
                            "          \"org.kie.server.api.marshalling.objects.PojoC\" : {\n" +
                            "            \"name\" : \"C2\"\n" +
                            "          }\n" +
                            "        } ]\n" +
                            "      }\n" +
                            "    }, {\n" +
                            "      \"org.kie.server.api.marshalling.objects.PojoB\" : {\n" +
                            "        \"name\" : \"B2\",\n" +
                            "        \"pojoCList\" : [ {\n" +
                            "          \"org.kie.server.api.marshalling.objects.PojoC\" : {\n" +
                            "            \"name\" : \"C3\"\n" +
                            "          }\n" +
                            "        } ]\n" +
                            "      }\n" +
                            "    } ],\n" +
                            "    \"stringList\" : [ \"Hello\", \"Bye\" ]\n" +
                            "  }\n" +
                            "}";

        Object unmarshalledObject = marshaller.unmarshall(rawContent, PojoA.class);
        Assertions.assertThat(unmarshalledObject).isEqualTo(createTestObject());
    }

    @Test
    public void testJSONSimpleClassName() {
        Marshaller marshaller = MarshallerFactory.getMarshaller(getCustomClasses(), MarshallingFormat.JSON, getClass().getClassLoader());
        String rawContent = "{\n" +
                            "  \"PojoA\" : {\n" +
                            "    \"name\" : \"A\",\n" +
                            "    \"pojoBList\" : [ {\n" +
                            "      \"PojoB\" : {\n" +
                            "        \"name\" : \"B1\",\n" +
                            "        \"pojoCList\" : [ {\n" +
                            "          \"PojoC\" : {\n" +
                            "            \"name\" : \"C1\"\n" +
                            "          }\n" +
                            "        }, {\n" +
                            "          \"PojoC\" : {\n" +
                            "            \"name\" : \"C2\"\n" +
                            "          }\n" +
                            "        } ]\n" +
                            "      }\n" +
                            "    }, {\n" +
                            "      \"PojoB\" : {\n" +
                            "        \"name\" : \"B2\",\n" +
                            "        \"pojoCList\" : [ {\n" +
                            "          \"PojoC\" : {\n" +
                            "            \"name\" : \"C3\"\n" +
                            "          }\n" +
                            "        } ]\n" +
                            "      }\n" +
                            "    } ],\n" +
                            "    \"stringList\" : [ \"Hello\", \"Bye\" ]\n" +
                            "  }\n" +
                            "}";

        Object unmarshalledObject = marshaller.unmarshall(rawContent, PojoA.class);
        Assertions.assertThat(unmarshalledObject).isEqualTo(createTestObject());
    }

    @Test
    public void testJSONSimpleClassNameTopLevelOnly() {
        Marshaller marshaller = MarshallerFactory.getMarshaller(getCustomClasses(), MarshallingFormat.JSON, getClass().getClassLoader());
        String rawContent = "{\n" +
                            "  \"PojoA\":{\n" +
                            "    \"name\":\"A\",\n" +
                            "    \"pojoBList\":[\n" +
                            "      {\n" +
                            "        \"name\":\"B1\",\n" +
                            "        \"pojoCList\":[\n" +
                            "          {\n" +
                            "            \"name\":\"C1\"\n" +
                            "          },\n" +
                            "          {\n" +
                            "            \"name\":\"C2\"\n" +
                            "          }\n" +
                            "        ]\n" +
                            "      },\n" +
                            "      {\n" +
                            "        \"name\":\"B2\",\n" +
                            "        \"pojoCList\":[\n" +
                            "          {\n" +
                            "            \"name\":\"C3\"\n" +
                            "          }\n" +
                            "        ]\n" +
                            "      }\n" +
                            "    ],\n" +
                            "    \"stringList\":[\n" +
                            "      \"Hello\",\n" +
                            "      \"Bye\"\n" +
                            "    ]\n" +
                            "  }\n" +
                            "}";

        Object unmarshalledObject = marshaller.unmarshall(rawContent, PojoA.class);
        Assertions.assertThat(unmarshalledObject).isEqualTo(createTestObject());
    }
}
