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

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.UriParam;
import org.kie.server.client.KieServicesConfiguration;

public class KieEndpoint extends DefaultEndpoint {

    private KieServicesConfiguration configuration;

    @UriParam
    private String username;

    @UriParam
    private String password;

    public KieEndpoint(String uri, KieComponent component, KieServicesConfiguration configuration ) throws URISyntaxException, MalformedURLException {
        super(uri, component);
        this.configuration = configuration;
    }

    public KieServicesConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public Producer createProducer() throws Exception {
        return new KieProducer(this);
    }

    @Override
    public Consumer createConsumer( Processor processor ) throws Exception {
        throw new UnsupportedOperationException("Consumer not supported for " + getClass().getSimpleName() + " endpoint");
    }

    @Override
    public boolean isSingleton() {
        return true;
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
}
