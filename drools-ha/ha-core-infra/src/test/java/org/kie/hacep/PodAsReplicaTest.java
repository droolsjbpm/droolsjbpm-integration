package org.kie.hacep;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.Assert;
import org.junit.Test;
import org.kie.hacep.core.Bootstrap;
import org.kie.hacep.core.infra.election.State;
import org.kie.hacep.sample.kjar.StockTickEvent;
import org.kie.remote.RemoteFactHandle;
import org.kie.remote.RemoteKieSession;
import org.kie.remote.TopicsConfig;
import org.kie.remote.command.FireUntilHaltCommand;
import org.kie.remote.command.InsertCommand;
import org.kie.remote.command.RemoteCommand;
import org.kie.remote.impl.consumer.Listener;
import org.kie.remote.impl.consumer.ListenerThread;
import org.kie.remote.message.ControlMessage;
import org.kie.remote.util.KafkaRemoteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;
import static org.kie.remote.CommonConfig.getTestProperties;
import static org.kie.remote.util.SerializationUtil.deserialize;

public class PodAsReplicaTest extends KafkaFullTopicsTests {

    private Logger logger = LoggerFactory.getLogger(PodAsReplicaTest.class);

    @Test(timeout = 600000L)
    public void processOneSentMessageAsLeaderAndThenReplicaTest() {
        Bootstrap.startEngine(envConfig);
        Bootstrap.getConsumerController().getCallback().updateStatus(State.LEADER);
        KafkaConsumer eventsConsumer = kafkaServerTest.getConsumer(envConfig.getEventsTopicName(),
                                                                   Config.getConsumerConfig("eventsConsumerProcessOneSentMessageAsLeaderTest"));
        KafkaConsumer controlConsumer = kafkaServerTest.getConsumer(envConfig.getControlTopicName(),
                                                                    Config.getConsumerConfig("controlConsumerProcessOneSentMessageAsLeaderTest"));

        KafkaConsumer<byte[], String> kafkaLogConsumer = kafkaServerTest.getStringConsumer(TEST_KAFKA_LOGGER_TOPIC);
        ListenerThread listenerThread = KafkaRemoteUtil.getListenerThread(TopicsConfig.getDefaultTopicsConfig(), envConfig.isLocal(), getTestProperties());
        Listener listener = new Listener(getTestProperties(), listenerThread);
        kafkaServerTest.insertBatchStockTicketEvent(1, topicsConfig, RemoteKieSession.class, listener);

        try {
            //EVENTS TOPIC
            logger.warn("Checks on Events topic");
            ConsumerRecords eventsRecords = eventsConsumer.poll(Duration.ofSeconds(2));

            AtomicReference<ConsumerRecord<String, byte[]>> lastEvent = new AtomicReference<>();
            final AtomicInteger index = new AtomicInteger(0);
            final AtomicInteger attempts = new AtomicInteger(0);
            while (index.get() < 2) {
                eventsRecords.forEach(o -> {
                    ConsumerRecord<String, byte[]> eventsRecord = (ConsumerRecord<String, byte[]>) o;
                    assertNotNull(eventsRecord);
                    Assert.assertEquals(eventsRecord.topic(), envConfig.getEventsTopicName());
                    RemoteCommand remoteCommand = deserialize(eventsRecord.value());
                    assertEquals(eventsRecord.offset(), index.get());
                    assertNotNull(remoteCommand.getId());

                    if (index.get() == 0) {
                        assertTrue(remoteCommand instanceof FireUntilHaltCommand);
                    }

                    if (index.get() == 1) {
                        assertTrue(remoteCommand instanceof InsertCommand);
                        InsertCommand insertCommand = (InsertCommand) remoteCommand;
                        assertEquals("DEFAULT", insertCommand.getEntryPoint());
                        assertNotNull(insertCommand.getId());
                        assertNotNull(insertCommand.getFactHandle());
                        RemoteFactHandle remoteFactHandle = insertCommand.getFactHandle();
                        StockTickEvent eventsTicket = (StockTickEvent) remoteFactHandle.getObject();
                        Assert.assertEquals("RHT", eventsTicket.getCompany());
                    }

                    index.incrementAndGet();

                    if (index.get() > 2) {
                        throw new RuntimeException("Found " + index.get() + " messages, more than the 2 expected.");
                    }
                    lastEvent.set(eventsRecord);
                });
                logger.warn("Attempt number:{}", attempts.incrementAndGet());
                if(attempts.get() == 10){
                    throw new RuntimeException("No Events message available after "+attempts + "attempts.");
                }
            }

            //CONTROL TOPIC
            logger.warn("Checks on Control topic");
            index.set(0);
            attempts.set(0);
            while (index.get() < 2 ) {
                ConsumerRecords controlRecords = controlConsumer.poll(Duration.ofSeconds(1));
                controlRecords.forEach(o -> {
                    ConsumerRecord<String, byte[]> control = (ConsumerRecord<String, byte[]>)o;
                    // FireUntilHalt command has no side effects
                    logger.warn("Control message found:{}", control);
                    Assert.assertEquals(control.topic(), envConfig.getControlTopicName());
                    ControlMessage controlMessage = deserialize(control.value());
                    assertEquals(control.offset(), index.get());
                    if(index.get()== 0) {
                        // FireUntilHalt command has no side effects
                        assertTrue(controlMessage.getSideEffects().isEmpty());
                    }
                    if(index.get()== 1) {
                        assertFalse(controlMessage.getSideEffects().isEmpty());
                        checkInsertSideEffects(lastEvent.get(), control);
                    }
                    index.incrementAndGet();
                });
                logger.warn("Attempt number:{}", attempts.incrementAndGet());
                if(attempts.get() == 10){
                    throw new RuntimeException("No Events message available after "+attempts + "attempts.");
                }
            }

            //no more msg to consume as a leader
            eventsRecords = eventsConsumer.poll(Duration.ofSeconds(2));
            assertEquals(0, eventsRecords.count());
            ConsumerRecords controlRecords = controlConsumer.poll(Duration.ofSeconds(2));
            assertEquals(0, controlRecords.count());

            // SWITCH AS a REPLICA
            logger.warn("Switch as a replica");
            Bootstrap.getConsumerController().getCallback().updateStatus(State.REPLICA);
            ConsumerRecords<byte[], String> recordsLog = kafkaLogConsumer.poll(Duration.ofSeconds(5));
            java.util.List<String> kafkaLoggerMsgs = new ArrayList<>();
            recordsLog.forEach(stringConsumerRecord -> {
                assertNotNull(stringConsumerRecord);
                kafkaLoggerMsgs.add(stringConsumerRecord.value());
                if(envConfig.isUnderTest()){
                    logger.warn("msg:{}", stringConsumerRecord.value());
                }
            });

            String sideEffectOnLeader = null;
            String sideEffectOnReplica = null;
            for (String item : kafkaLoggerMsgs) {
                if (item.startsWith("sideEffectOn")) {
                    if (item.endsWith(":null")) {
                        fail("SideEffects null");
                    }
                    if (item.startsWith("sideEffectOnReplica:")) {
                        sideEffectOnReplica = item.substring(item.indexOf("["));
                    }
                    if (item.startsWith("sideEffectOnLeader:")) {
                        sideEffectOnLeader = item.substring(item.indexOf("["));
                    }
                }
            }
            assertNotNull(sideEffectOnLeader);
            assertNotNull(sideEffectOnReplica);
            assertEquals(sideEffectOnLeader, sideEffectOnReplica);
        } catch (Exception ex) {
            logger.error(ex.getMessage(),
                         ex);
        } finally {
            eventsConsumer.close();
            controlConsumer.close();
            kafkaLogConsumer.close();
        }
    }

    private void checkInsertSideEffects(ConsumerRecord<String, byte[]> eventsRecord, ConsumerRecord<String, byte[]> controlRecord) {
        Assert.assertEquals(controlRecord.topic(), envConfig.getControlTopicName());
        ControlMessage controlMessage = deserialize(controlRecord.value());
        assertEquals(1, controlRecord.offset());
        assertTrue(!controlMessage.getSideEffects().isEmpty());
        assertTrue(controlMessage.getSideEffects().size() == 1);
        //Same msg content on Events topic and control topics
        assertEquals(controlRecord.key(), eventsRecord.key());
    }
}
