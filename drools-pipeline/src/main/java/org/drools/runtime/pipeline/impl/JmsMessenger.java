/*
 * Copyright 2010 JBoss Inc
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

package org.drools.runtime.pipeline.impl;

import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.naming.InitialContext;

import org.drools.runtime.pipeline.Pipeline;
import org.drools.runtime.pipeline.ResultHandlerFactory;
import org.drools.runtime.pipeline.Service;

public class JmsMessenger extends BaseService
    implements
    Service {
    private ConnectionFactory    connectionFactory;
    private Destination          destination;
    private MessageConsumer      consumer;
    private Connection           connection;
    private Session              session;

    private ResultHandlerFactory resultHandlerFactory;
    private Pipeline             pipeline;

    private Thread               thread;

    private JmsMessengerRunner   jmsFeederRunner;
    
    private String connectionPrincipal;
    private String connectionCredentials;

    public JmsMessenger(Pipeline pipeline,
                        Properties properties,
                        String destinationName,
                        ResultHandlerFactory resultHandlerFactory) {
    	this(pipeline, properties, "ConnectionFactory", false, destinationName, resultHandlerFactory);
    }
    
    public JmsMessenger(Pipeline pipeline,
                Properties properties,
                String connectionFactoryName,
                boolean useSecurityPrincipalForConnection,
                String destinationName,
                ResultHandlerFactory resultHandlerFactory) {
        super();
        this.pipeline = pipeline;
        this.resultHandlerFactory = resultHandlerFactory;
        if( useSecurityPrincipalForConnection ){
        	this.connectionPrincipal = properties.getProperty("java.naming.security.principal");
        	this.connectionCredentials = properties.getProperty("java.naming.security.credentials");
        }

        try {
            InitialContext jndiContext = new InitialContext( properties );
            this.connectionFactory = (ConnectionFactory) jndiContext.lookup( connectionFactoryName );
            this.destination = (Destination) jndiContext.lookup( destinationName );
        } catch ( Exception e ) {
            throw new RuntimeException( "Unable to instantiate JmsFeeder",
                                        e );
        }
    }

    public void start() {
        try {
        	if( connectionPrincipal != null ){
        		this.connection = this.connectionFactory.createConnection(connectionPrincipal, connectionCredentials);
        	} else {
        		this.connection = this.connectionFactory.createConnection();
        	}
            this.session = this.connection.createSession( false,
                                                          Session.AUTO_ACKNOWLEDGE );
            this.consumer = this.session.createConsumer( destination );

            this.connection.start();
        } catch ( Exception e ) {
            handleException( this,
                             null,
                             e );
        }
        this.jmsFeederRunner = new JmsMessengerRunner( this,
                                                       this.consumer,
                                                       this.pipeline,
                                                       this.resultHandlerFactory );
        this.jmsFeederRunner.setRun( true );
        this.thread = new Thread( this.jmsFeederRunner );
        this.thread.start();
    }

    public void stop() {
        try {
            this.jmsFeederRunner.setRun( false );
            // this will interrupt the receive()
            this.consumer.close();
            this.connection.stop();
        } catch ( JMSException e ) {
            handleException( this,
                             null,
                             e );
        }
    }

    //    public void run() {
    //        while ( this.run ) {
    //            Message msg = null;
    //            try {
    //                msg = this.consumer.receive();
    //                System.out.println( "msg received : " + msg );
    //                //                emit( msg,
    //                //                      new EntryPointPipelineContext( this.entryPoint ) );
    //            } catch ( JMSException e ) {
    //                handleException( this,
    //                                 msg,
    //                                 e );
    //            }
    //        }
    //    }

    public static class JmsMessengerRunner
        implements
        Runnable {
        private JmsMessenger         feeder;
        private MessageConsumer      consumer;
        private Pipeline             pipeline;
        private ResultHandlerFactory resultHandlerFactory;
        private volatile boolean     run;

        public JmsMessengerRunner(JmsMessenger feeder,
                                  MessageConsumer consumer,
                                  Pipeline pipeline,
                                  ResultHandlerFactory resultHandlerFactory) {
            super();
            this.feeder = feeder;
            this.consumer = consumer;
            this.pipeline = pipeline;
            this.resultHandlerFactory = resultHandlerFactory;
        }

        public void run() {
            while ( this.run ) {
                Message msg = null;
                try {
                    msg = this.consumer.receive();
                    if (  msg != null ) {
                        if ( this.resultHandlerFactory != null ) {
                            pipeline.insert( msg,
                                             this.resultHandlerFactory.newResultHandler() );
                        } else {
                            pipeline.insert( msg,
                                             null );
                        }
                    }
                } catch ( JMSException e ) {
                    this.feeder.handleException( this.feeder,
                                                 msg,
                                                 e );
                }
            }
        }

        public void setRun(boolean run) {
            this.run = run;
        }

    }
}
