/*
 * Copyright 2013 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.spring.factorybeans;

import org.drools.core.util.StringUtils;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import javax.enterprise.inject.spi.BeanManager;

@Deprecated
public class KModuleFactoryBean
        implements
        FactoryBean,
        InitializingBean {

    private String mode;
    private KieContainer kContainer;

    public static BeanManager beanManager;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Object getObject() throws Exception {
        return kContainer;
    }

    public Class<? extends KieContainer> getObjectType() {
        return KieContainer.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() throws Exception {
        if (StringUtils.isEmpty(mode)) {
            throw new Exception("KModule Mode is missing");
        }
        if ("API".equals(mode)) {
            KieServices ks = KieServices.Factory.get();
            kContainer = ks.getKieClasspathContainer();
        } else if ("CDI".equalsIgnoreCase(mode)) {
            System.out.println("beanManager from KModuleFactoryBean == " + beanManager);
        }
    }
}
