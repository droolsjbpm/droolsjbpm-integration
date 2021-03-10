package org.jbpm.springboot.samples;

import java.io.File;

import org.appformer.maven.integration.MavenRepository;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.runtime.manager.impl.jpa.EntityManagerFactoryManager;
import org.jbpm.services.api.DeploymentService;
import org.jbpm.services.api.ProcessService;
import org.jbpm.springboot.samples.events.emitters.CountDownLatchEmitter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.process.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.appformer.maven.integration.MavenRepository.getMavenRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {JBPMApplication.class, TestAutoConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations="classpath:application-test.properties")
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class EventEmitterTest {

    static final String ARTIFACT_ID = "evaluation";
    static final String GROUP_ID = "org.jbpm.test";
    static final String VERSION = "1.0.0";

    private KModuleDeploymentUnit unit = null;

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private ProcessService processService;

    @Autowired
    private CountDownLatchEmitter countDownLatchEmitter;

    @BeforeClass
    public static void generalSetup() {
        KieServices ks = KieServices.Factory.get();
        ReleaseId releaseId = ks.newReleaseId(GROUP_ID, ARTIFACT_ID, VERSION);
        File kjar = new File("../kjars/evaluation/jbpm-module.jar");
        File pom = new File("../kjars/evaluation/pom.xml");
        MavenRepository repository = getMavenRepository();
        repository.installArtifact(releaseId, kjar, pom);

        EntityManagerFactoryManager.get().clear();
    }


    @Before
    public void setup() {
        unit = new KModuleDeploymentUnit(GROUP_ID, ARTIFACT_ID, VERSION);
        deploymentService.deploy(unit);
    }

    @After
    public void cleanup() {

        deploymentService.undeploy(unit);
    }

    @Test(timeout = 10000)
    public void testProcessEventListenerRegistration() throws Exception {
        countDownLatchEmitter.configure(4);

        assertNotNull(unit);
        assertNotNull(countDownLatchEmitter.getProcessService());

        Long processInstanceId = processService.startProcess(unit.getIdentifier(), "evaluation");

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId > 0);

        // "newCollection", "apply" and "deliver" methods should've been called
        assertThat(countDownLatchEmitter.getCountDownLatch().getCount()).isEqualTo(1);

        processService.abortProcessInstance(processInstanceId);
        countDownLatchEmitter.getCountDownLatch().await();

        assertThat(countDownLatchEmitter.getCountDownLatch().getCount()).isEqualTo(0);

        ProcessInstance pi = processService.getProcessInstance(processInstanceId);
        assertNull(pi);
    }
}
