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

package net.pmml.tester.mining;

import java.util.Arrays;
import java.util.Collection;

import net.pmml.tester.AbstractPMMLRuntimeProvider;
import net.pmml.tester.AbstractPMMLTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class PredicatesMiningTest extends AbstractPMMLTest {

    public static final String MODEL_NAME = "PredicatesMining";
    private static final String FILE_NAME = "MiningModel_Predicates.pmml";
    private static final String FULL_FILE_PATH = "predicatesmining/" + FILE_NAME;

    public static final Collection<Object[]> INPUT_DATA = Arrays.asList(new Object[][]{
            {"AP", true, "ASTRONAUT", "classA", "red", 6.6, 25.0, 1.381666666666666},
            {"KN", true, "PROGRAMMER", "classA", "blue", 9.12, 2.3, -0.10000000000000053},
            {"TN", false, "INSTRUCTOR", "classC", "yellow", 333.12, 33.56, null},
            {"KN", true, "ASTRONAUT", "classB", "orange", 1.23, 30.12, 22.3725},
            {"TN", false, "TEACHER", "classC", "green", 12.34, 22.12, 32.9},
            {"AP", false, "INSTRUCTOR", "classB", "green", 2.2, 11.33, 12.899999999999999},
            {"KN", true, "SKYDIVER", "classB", "orange", 9.12, 42.2, 11.448333333333332},
            {"AP", false, "TEACHER", "classA", "yellow", 11.2, 12.1, -103.35},
    });

    public static final Collection<Object[]> DATA = getDATA(INPUT_DATA, FILE_NAME, FULL_FILE_PATH);

    public PredicatesMiningTest(String residenceState,
                                boolean validLicense,
                                String occupation,
                                String categoricalY,
                                String categoricalX,
                                double variable,
                                double age,
                                Double expectedResult,
                                AbstractPMMLRuntimeProvider abstractPMMLRuntimeProvider,
                                String fileName) {
        super(abstractPMMLRuntimeProvider.getPMMLRuntime(MODEL_NAME, fileName));
        abstractPMMLExecutor = new PredicatesMiningExecutor(residenceState,
                                                            validLicense,
                                                            occupation,
                                                            categoricalY,
                                                            categoricalX,
                                                            variable,
                                                            age,
                                                            expectedResult);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return DATA;
    }
}
