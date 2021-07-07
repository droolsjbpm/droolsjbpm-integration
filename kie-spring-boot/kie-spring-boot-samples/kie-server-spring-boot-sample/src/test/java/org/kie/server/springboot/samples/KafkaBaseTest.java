/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.springboot.samples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.kie.internal.runtime.conf.RuntimeStrategy.SINGLETON;
import static org.kie.internal.runtime.conf.RuntimeStrategy.PER_PROCESS_INSTANCE;
import static org.kie.internal.runtime.conf.RuntimeStrategy.PER_REQUEST;

@RunWith(Parameterized.class)
public abstract class KafkaBaseTest {

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return new ArrayList<Object[]>(Arrays.asList(new Object[][]
          {
            {SINGLETON.name()},
            {PER_PROCESS_INSTANCE.name()}, 
            {PER_REQUEST.name()}
          }
        ));
    }
    
    @Parameterized.Parameter(0)
    public String strategy;
}
