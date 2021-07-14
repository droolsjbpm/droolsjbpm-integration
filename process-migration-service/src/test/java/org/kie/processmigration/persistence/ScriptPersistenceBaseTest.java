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
package org.kie.processmigration.persistence;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.hibernate.Version;
import org.jbpm.test.persistence.scripts.DatabaseType;
import org.jbpm.test.persistence.scripts.PersistenceUnit;
import org.jbpm.test.persistence.scripts.ScriptsBase;
import org.jbpm.test.persistence.scripts.TestPersistenceContextBase;
import org.jbpm.test.persistence.scripts.util.ScriptFilter;
import org.jbpm.test.persistence.scripts.util.TestsUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.kie.processmigration.service.AbstractPersistenceTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jbpm.test.persistence.scripts.TestPersistenceContextBase.createAndInitContext;
import static org.junit.Assume.assumeTrue;

public abstract class ScriptPersistenceBaseTest extends AbstractPersistenceTest {
    protected static final String DB_DDL_SCRIPTS_RESOURCE_PATH = "/db/ddl-scripts";
    protected static ScriptFilter createScript = ScriptFilter.init(false, true);
    protected static ScriptFilter dropScript = ScriptFilter.init(false, false);

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptPersistenceBaseTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            LOGGER.info(">>>> Starting test: " + description.getMethodName());
        }
    };

    @BeforeClass
    public static void prepare() throws IOException, SQLException {
        LOGGER.info("Running with Hibernate " + Version.getVersionString());
        TestsUtil.clearSchema();
        executeScriptRunner(DB_DDL_SCRIPTS_RESOURCE_PATH, ScriptFilter.init(false, false));

        TestPersistenceContextBase scriptRunnerContext = createAndInitContext(PersistenceUnit.SCRIPT_RUNNER);
        DatabaseType dbType = scriptRunnerContext.getDatabaseType();
        assumeTrue("Scripts test not supported this database " + dbType + ": " + createScript.getSupportedDatabase(), createScript.isSupportedDatabase(dbType));

        executeScriptRunner(DB_DDL_SCRIPTS_RESOURCE_PATH, createScript);
    }

    @AfterClass
    public static void tearDown() throws SQLException, IOException {
        executeScriptRunner(DB_DDL_SCRIPTS_RESOURCE_PATH, dropScript);
    }

    public static void executeScriptRunner(String resourcePath, ScriptFilter scriptFilter) throws IOException, SQLException {
        final TestPersistenceContextBase scriptRunnerContext = createAndInitContext(PersistenceUnit.SCRIPT_RUNNER);
        try {
            scriptRunnerContext.executeScripts(new File(ScriptsBase.class.getResource(resourcePath).getFile()), scriptFilter);
        } finally {
            scriptRunnerContext.clean();
        }
    }
}
