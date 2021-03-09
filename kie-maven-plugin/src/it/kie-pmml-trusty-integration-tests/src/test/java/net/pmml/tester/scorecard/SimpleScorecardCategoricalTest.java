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

package net.pmml.tester.scorecard;

import java.util.Arrays;
import java.util.Collection;

import net.pmml.tester.AbstractPMMLRuntimeProvider;
import net.pmml.tester.AbstractPMMLTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SimpleScorecardCategoricalTest extends AbstractPMMLTest {

    public static final String MODEL_NAME = "SimpleScorecardCategorical";
    public static final String FILE_NAME = "SimpleScorecardCategorical.pmml";
    public static final String FULL_FILE_PATH = "simplescorecardcategorical/" + FILE_NAME;


    private static final Collection<Object[]> INPUT_DATA = Arrays.asList(new Object[][]{
            {"classA", "classB", 25.0, "Input1ReasonCode", null},
            {"classA", "classA", -15.0, "Input1ReasonCode", "Input2ReasonCode"},
            {"classB", "classB", 87.0, null, null},
            {"classB", "classA", 47.0, "Input2ReasonCode", null},
    });

    public static final Collection<Object[]> DATA = getDATA(INPUT_DATA, FILE_NAME, FULL_FILE_PATH);

    public SimpleScorecardCategoricalTest(String input1, String input2, double score, String reasonCode1, String reasonCode2,
                                          AbstractPMMLRuntimeProvider abstractPMMLRuntimeProvider,
                                          String fileName) {
        super(abstractPMMLRuntimeProvider.getPMMLRuntime(MODEL_NAME, fileName));
        abstractPMMLExecutor = new SimpleScorecardCategoricalExecutor(input1, input2, score, reasonCode1, reasonCode2);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return DATA;
    }
}
