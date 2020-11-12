/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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
package org.kie.camel.container.module;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.kie.camel.KieServicesConfigurationCustomizer;
import org.kie.camel.container.api.model.Person;
import org.kie.camel.container.api.model.cloudbalance.CloudBalance;
import org.kie.camel.container.api.model.cloudbalance.CloudComputer;
import org.kie.camel.container.api.model.cloudbalance.CloudProcess;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.KieServicesConfiguration;


public class CamelKieServiceConfigurationCustomizer implements KieServicesConfigurationCustomizer {

    private static final long KIE_SERVER_CLIENT_TIMEOUT = 60000L;

    @Override
    public KieServicesConfiguration apply(KieServicesConfiguration configuration) {
        final Set<Class<?>> additionalClasses = new HashSet<>();
        additionalClasses.add(Person.class);
        additionalClasses.add(CloudBalance.class);
        additionalClasses.add(CloudComputer.class);
        additionalClasses.add(CloudProcess.class);

        final KieServicesConfiguration conf = configuration.clone();
        conf.addExtraClasses(additionalClasses);
        conf.setTimeout(KIE_SERVER_CLIENT_TIMEOUT);
        conf.setMarshallingFormat(MarshallingFormat.JSON);
        return conf;
    }

}
