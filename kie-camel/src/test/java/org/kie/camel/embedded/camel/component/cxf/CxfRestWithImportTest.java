/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.camel.embedded.camel.component.cxf;

import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CxfRestWithImportTest extends CamelSpringTestSupport {

    private static final Logger logger = LoggerFactory.getLogger(CxfRestWithImportTest.class);

    @Override
    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/kie/camel/component/CxfRsSpringWithImport.xml");
    }

    @Test
    public void test1() throws Exception {
        KieSession kieSession = (KieSession)applicationContext.getBean("ksession1");
        kieSession.setGlobal("out", System.out);

        String cmd = "";
        cmd += "<batch-execution lookup=\"ksession1\">\n";
        cmd += "  <insert out-identifier=\"vkiran\">\n";
        cmd += "      <org.drools.example.api.namedkiesession.Message>\n";
        cmd += "         <name>HAL</name>\n";
        cmd += "         <text>Hello, HAL. Do you read me, HAL?</text>\n";
        cmd += "      </org.drools.example.api.namedkiesession.Message>\n";
        cmd += "   </insert>\n";
        cmd += "   <fire-all-rules/>\n";
        cmd += "</batch-execution>\n";

        Object object = this.context.createProducerTemplate().requestBody("direct://http", cmd);
        logger.debug(object.toString());
    }

}
