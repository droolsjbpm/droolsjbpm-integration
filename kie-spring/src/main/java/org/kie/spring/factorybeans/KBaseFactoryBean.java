/*
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

import org.kie.api.KieBase;
import org.kie.api.builder.ReleaseId;
import org.kie.spring.KieObjectsResolver;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class KBaseFactoryBean
        implements
        FactoryBean<KieBase>,
        InitializingBean {

    private String id;
    private String kBaseName;
    private KieBase kBase;
    private ReleaseId releaseId;


    public ReleaseId getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(ReleaseId releaseId) {
        this.releaseId = releaseId;
    }

    public String getKBaseName() {
        return kBaseName;
    }

    public void setKBaseName(String name) {
        this.kBaseName = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public KieBase getObject() throws Exception {
        return kBase;
    }

    public Class<? extends KieBase> getObjectType() {
        return KieBase.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() throws Exception {
        KieObjectsResolver kieObjectsResolver = new KieObjectsResolver();
        kBase = kieObjectsResolver.resolveKBase(kBaseName, releaseId);
    }

}