/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.kie.camel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.RuntimeCamelException;
import org.apache.camel.spi.UriParam;

public class KieConfiguration implements Cloneable {

    @UriParam(label = "security", secret = true)
    private String username;

    @UriParam(label = "security", secret = true)
    private String password;

    private KieServicesConfigurationCustomizer kieServicesConfigurationCustomizer;

    private Map<String, String> bodyParams = new HashMap<>();

    public KieConfiguration() {
        initBodyParams();
    }

    private void initBodyParams() {
        setBodyParam( "process", "signal", "event" );
        setBodyParam( "dmn", "evaluateAll", "dmnContext" );
        setBodyParam( "dmn", "evaluateDecisionByName", "dmnContext" );
        setBodyParam( "dmn", "evaluateDecisionById", "dmnContext" );
    }

    public void configure(String remaining) {
        String userInfo = null;
        try {
            userInfo = new URI(remaining).getUserInfo();
        } catch (URISyntaxException e) {
            throw new RuntimeException( e );
        }
        if (userInfo != null) {
            String[] parts = userInfo.split(":");
            if (parts.length == 2) {
                setUsername(parts[0]);
                setPassword(parts[1]);
            } else {
                setUsername(userInfo);
            }
        }
    }

    public KieConfiguration copy() {
        try {
            KieConfiguration copy = (KieConfiguration) clone();
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeCamelException(e);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername( String username ) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword( String password ) {
        this.password = password;
    }

    public KieServicesConfigurationCustomizer getKieServicesConfigurationCustomizer() {
        return kieServicesConfigurationCustomizer;
    }

    public void setKieServicesConfigurationCustomizer(KieServicesConfigurationCustomizer kieServicesConfigurationCustomizer) {
        this.kieServicesConfigurationCustomizer = kieServicesConfigurationCustomizer;
    }

    public KieConfiguration setBodyParam(String serviceName, String methodName, String paramName) {
        bodyParams.put(serviceName + "." + methodName, paramName);
        return this;
    }

    public String getBodyParam(String serviceName, String methodName) {
        return bodyParams.get(serviceName + "." + methodName);
    }

    public KieConfiguration clearBodyParams() {
        bodyParams.clear();
        return this;
    }
}
