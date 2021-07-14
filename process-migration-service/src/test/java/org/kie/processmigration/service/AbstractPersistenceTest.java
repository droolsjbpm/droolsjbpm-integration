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

package org.kie.processmigration.service;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.kie.test.util.db.DataSourceFactory;
import org.kie.test.util.db.PersistenceUtil;
import org.kie.test.util.db.PoolingDataSourceWrapper;

public abstract class AbstractPersistenceTest extends AbstractBeanBasedTest {

    private PoolingDataSourceWrapper ds;
    private static final String DATASOURCE = "jdbc/testDS1";
    private static final Properties DS_PROPS = PersistenceUtil.getDatasourceProperties();

    protected String getDatasourceName() {
        return DATASOURCE;
    }

    @Before
    public void setupPersistence() {
        ds = DataSourceFactory.setupPoolingDataSource(getDatasourceName(), DS_PROPS);
    }

    @After
    public void closeResources() {
        if(ds != null) {
            ds.close();
        }
    }
}
