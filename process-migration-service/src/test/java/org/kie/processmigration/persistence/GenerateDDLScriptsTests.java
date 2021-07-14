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

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Persistence;

import org.hibernate.engine.jdbc.internal.DDLFormatterImpl;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;

/**
 * Utility class for generating DDL scripts (create and drop) please ignore it.
 */
@RunWith(Parameterized.class)
public class GenerateDDLScriptsTests {

    public static class ScriptFile {

        private final String dialect;
        private final String alias;
        private final String prefix;
        private final String subtype;
        private final String delimiter;

        public ScriptFile(String dialect, String alias, String prefix, String subtype, String delimiter) {
            this.dialect = dialect;
            this.alias = alias;
            this.prefix = prefix;
            this.subtype = subtype;
            this.delimiter = delimiter;
        }

        public ScriptFile(String dialect, String alias, String prefix, String subtype) {
            this(dialect, alias, prefix, subtype, ";");
        }

        public ScriptFile(String dialect, String alias, String prefix) {
            this(dialect, alias, prefix, "", ";");
        }

        public Path buildCreateFile(Path basePath) {
            return basePath.resolve(alias).resolve(prefix + "-" + (subtype.isEmpty() ? "" : subtype + "-") + "pim-schema.sql");
        }

        public Path buildDropFile(Path basePath) {
            return basePath.resolve(alias).resolve(prefix + "-" + (subtype.isEmpty() ? "" : subtype + "-") + "pim-drop-schema.sql");

        }

        public String getDialect() {
            return this.dialect;
        }

        public String getDelimiter() {
            return this.delimiter;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((dialect == null) ? 0 : dialect.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ScriptFile other = (ScriptFile) obj;
            if (dialect == null) {
                if (other.dialect != null)
                    return false;
            } else if (!dialect.equals(other.dialect))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return alias + "-" + subtype;
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<ScriptFile> dialect() {
        return asList(new ScriptFile("org.hibernate.dialect.DB2Dialect", "db2", "db2"),
                      new ScriptFile("org.hibernate.dialect.DerbyDialect", "derby", "derby"),
                      new ScriptFile("org.hibernate.dialect.H2Dialect", "h2", "h2"),
                      new ScriptFile("org.hibernate.dialect.HSQLDialect", "hsqldb", "hsqldb"),
                      new ScriptFile("org.hibernate.dialect.MySQL5Dialect", "mysql5", "mysql5"),
                      new ScriptFile("org.hibernate.dialect.MySQLInnoDBDialect", "mysqlinnodb", "mysql", "innodb"),
                      new ScriptFile("org.hibernate.dialect.Oracle10gDialect", "oracle", "oracle"),
                      new ScriptFile("org.hibernate.dialect.PostgreSQLDialect", "postgresql", "postgresql"),
                      new ScriptFile("org.hibernate.dialect.SQLServerDialect", "sqlserver", "sqlserver"),
                      new ScriptFile("org.hibernate.dialect.SQLServer2008Dialect", "sqlserver2008", "sqlserver2008"),
                      new ScriptFile("org.hibernate.dialect.SybaseDialect", "sybase", "sybase", ""," lock datarows\n" +
                              "    go"));
    }

    private final ScriptFile scriptFile;

    public GenerateDDLScriptsTests(ScriptFile scriptFile) {
        this.scriptFile = scriptFile;
    }

    @Test
    @Ignore
    public void generateDDL() throws Exception {

        Path basePath = Paths.get("src", "main", "resources", "db", "ddl-scripts");

        Path createFilePath = scriptFile.buildCreateFile(basePath);
        Path dropFilePath = scriptFile.buildDropFile(basePath);
        String delimiter = scriptFile.getDelimiter();

        Files.deleteIfExists(createFilePath);
        Files.deleteIfExists(dropFilePath);

        Map<String, Object> properties = new HashMap<>();
        StringWriter drop = new StringWriter();
        StringWriter create = new StringWriter();

        properties.put("hibernate.dialect", this.scriptFile.getDialect());
        properties.put("javax.persistence.schema-generation.scripts.action", "drop-and-create");
        properties.put("javax.persistence.schema-generation.scripts.drop-target", drop);
        properties.put("javax.persistence.schema-generation.scripts.create-target", create);
        Persistence.generateSchema("org.kie.test.persistence.generate-ddl-scripts", properties);

        try (FileWriter dropFile = new FileWriter(dropFilePath.toString());
             FileWriter createFile = new FileWriter(createFilePath.toString())) {
            dropFile.write(prettyFormatSQL(drop.toString(), delimiter));
            createFile.write(prettyFormatSQL(create.toString(), delimiter));
        }
    }

    private static String prettyFormatSQL(String unformatted, String delimiter)
    {
        BufferedReader reader = new BufferedReader(new StringReader(unformatted));
        DDLFormatterImpl formatter = new DDLFormatterImpl();
        StringBuffer buffer = new StringBuffer();

        reader.lines().forEach(x -> {
            String formatted = formatter.format(x + delimiter);
            buffer.append(formatted).append("\n");
        });
        return buffer.toString();
    }
}
