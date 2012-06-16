/*
 * Copyright 2012 JBoss Inc
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

package org.drools.container.spring;

import org.drools.Person;
import org.drools.audit.ThreadedWorkingMemoryFileLogger;
import org.drools.audit.WorkingMemoryConsoleLogger;
import org.drools.audit.WorkingMemoryFileLogger;
import org.drools.container.spring.beans.KnowledgeLoggerAdaptor;
import org.drools.impl.StatefulKnowledgeSessionImpl;
import org.drools.impl.StatelessKnowledgeSessionImpl;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.StatelessKnowledgeSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SpringDroolsLoggersTest {

    static ClassPathXmlApplicationContext context = null;
    @BeforeClass
    public static void runBeforeClass() {
        context = new ClassPathXmlApplicationContext( "org/drools/container/spring/loggers.xml" );
    }

    @AfterClass
    public static void runAfterClass() {
        context.close();
    }

    @Test
    public void testStatefulKnowledgeConsoleLogger() throws Exception {
        StatefulKnowledgeSession statefulSession = (StatefulKnowledgeSession) context.getBean( "ConsoleLogger-statefulSession" );
        StatefulKnowledgeSessionImpl impl = (StatefulKnowledgeSessionImpl)statefulSession;
        for ( Object listener : impl.session.getWorkingMemoryEventListeners()) {
            assertTrue(listener instanceof WorkingMemoryConsoleLogger);
        }
//        assertNotNull(statefulSession.getGlobals().get("list"));
//        statefulSession.insert(new Person("Darth", "Cheddar", 50));
//        statefulSession.fireAllRules();
        statefulSession.dispose();
    }

    @Test
    public void testStatefulKnowledgeFileLogger() throws Exception {
        StatefulKnowledgeSession statefulSession = (StatefulKnowledgeSession) context.getBean( "FileLogger-statefulSession" );
        assertNotNull(statefulSession.getGlobals().get("list"));
        StatefulKnowledgeSessionImpl impl = (StatefulKnowledgeSessionImpl)statefulSession;
        for ( Object listener : impl.session.getWorkingMemoryEventListeners()) {
            assertTrue(listener instanceof WorkingMemoryFileLogger);
        }
//        statefulSession.insert(new Person("Darth", "Cheddar", 50));
//        statefulSession.fireAllRules();

        KnowledgeLoggerAdaptor adaptor = (KnowledgeLoggerAdaptor) context.getBean("sf_fl_logger");
        assertNotNull(adaptor);
        assertNotNull(adaptor.getRuntimeLogger());

        statefulSession.dispose();
    }

    @Test
    public void testStatefulKnowledgeThreadedFileLogger() throws Exception {
        StatefulKnowledgeSession statefulSession = (StatefulKnowledgeSession) context.getBean( "ThreadedFileLogger-statefulSession" );
        assertNotNull(statefulSession.getGlobals().get("list"));
        StatefulKnowledgeSessionImpl impl = (StatefulKnowledgeSessionImpl)statefulSession;
        for ( Object listener : impl.session.getWorkingMemoryEventListeners()) {
            assertTrue(listener instanceof ThreadedWorkingMemoryFileLogger);
        }
//        statefulSession.insert(new Person("Darth", "Cheddar", 50));
//        statefulSession.fireAllRules();
        KnowledgeLoggerAdaptor adaptor = (KnowledgeLoggerAdaptor) context.getBean("sf_tfl_logger");
        assertNotNull(adaptor);
        assertNotNull(adaptor.getRuntimeLogger());
        statefulSession.dispose();
    }

    @Test
    public void testStatelessKnowledgeConsoleLogger() throws Exception {
        StatelessKnowledgeSession statelessKnowledgeSession = (StatelessKnowledgeSession) context.getBean( "ConsoleLogger-statelessSession" );
        StatelessKnowledgeSessionImpl impl = (StatelessKnowledgeSessionImpl)statelessKnowledgeSession;
        for ( Object listener : impl.workingMemoryEventSupport.getEventListeners()){
            assertTrue(listener instanceof WorkingMemoryConsoleLogger);
        }
//        List list = new ArrayList();
//        statelessKnowledgeSession.setGlobal("list", list);
//        assertNotNull(statelessKnowledgeSession.getGlobals().get("list"));
//        statelessKnowledgeSession.execute(new Person("Darth", "Cheddar", 50));
    }

    @Test
    public void testStatelessKnowledgeFileLogger() throws Exception {
        StatelessKnowledgeSession statelessKnowledgeSession = (StatelessKnowledgeSession) context.getBean( "FileLogger-statelessSession" );
        StatelessKnowledgeSessionImpl impl = (StatelessKnowledgeSessionImpl)statelessKnowledgeSession;
        for ( Object listener : impl.workingMemoryEventSupport.getEventListeners()){
            assertTrue(listener instanceof WorkingMemoryFileLogger);
        }
//        List list = new ArrayList();
//        statelessKnowledgeSession.setGlobal("list", list);
//        assertNotNull(statelessKnowledgeSession.getGlobals().get("list"));
//        statelessKnowledgeSession.execute(new Person("Darth", "Cheddar", 50));
        KnowledgeLoggerAdaptor adaptor = (KnowledgeLoggerAdaptor) context.getBean("ss_fl_logger");
        assertNotNull(adaptor);
        assertNotNull(adaptor.getRuntimeLogger());
    }

    @Test
    public void testStatelessKnowledgeThreadedFileLogger() throws Exception {
        StatelessKnowledgeSession statelessKnowledgeSession = (StatelessKnowledgeSession) context.getBean( "ThreadedFileLogger-statelessSession" );
        StatelessKnowledgeSessionImpl impl = (StatelessKnowledgeSessionImpl)statelessKnowledgeSession;
        for ( Object listener : impl.workingMemoryEventSupport.getEventListeners()){
            assertTrue(listener instanceof ThreadedWorkingMemoryFileLogger);
        }
//        List list = new ArrayList();
//        statelessKnowledgeSession.setGlobal("list", list);
//        assertNotNull(statelessKnowledgeSession.getGlobals().get("list"));
//        statelessKnowledgeSession.execute(new Person("Darth", "Cheddar", 50));
        KnowledgeLoggerAdaptor loggerAdaptor = (KnowledgeLoggerAdaptor) context.getBean("ss_tfl_logger");
        assertNotNull(loggerAdaptor);
        assertNotNull(loggerAdaptor.getRuntimeLogger());
        loggerAdaptor.close();
    }

    @Test
    public void testKSessionLoggersFromGroupAndNested() throws Exception {
        StatelessKnowledgeSession statelessKnowledgeSession = (StatelessKnowledgeSession) context.getBean( "k1" );
        StatelessKnowledgeSessionImpl impl = (StatelessKnowledgeSessionImpl)statelessKnowledgeSession;
        assertEquals(2, impl.workingMemoryEventSupport.getEventListeners().size());

        List list = new ArrayList();
        statelessKnowledgeSession.setGlobal("list", list);
        assertNotNull(statelessKnowledgeSession.getGlobals().get("list"));
        statelessKnowledgeSession.execute(new Person("Darth", "Cheddar", 50));

        KnowledgeLoggerAdaptor adaptor = (KnowledgeLoggerAdaptor) context.getBean("k1_logger");
        assertNotNull(adaptor);
        assertNotNull(adaptor.getRuntimeLogger());
        adaptor.close();

        adaptor = (KnowledgeLoggerAdaptor) context.getBean("k1_console_logger");
        assertNotNull(adaptor);
        assertNotNull(adaptor.getRuntimeLogger());
    }

    @Test
    public void testStatelessNoNameFileLogger() throws Exception {
        StatelessKnowledgeSession statelessKnowledgeSession = (StatelessKnowledgeSession) context.getBean( "FileLogger-statelessSession-noNameLogger" );
        StatelessKnowledgeSessionImpl impl = (StatelessKnowledgeSessionImpl)statelessKnowledgeSession;
        for ( Object listener : impl.workingMemoryEventSupport.getEventListeners()){
            assertTrue(listener instanceof WorkingMemoryFileLogger);
        }
    }
}
