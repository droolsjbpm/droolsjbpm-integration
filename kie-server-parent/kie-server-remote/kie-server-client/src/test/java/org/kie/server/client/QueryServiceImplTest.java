package org.kie.server.client;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.model.definition.ProcessDefinition;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;

public class QueryServiceImplTest extends BaseKieServicesClientTest {

    private QueryServicesClient queryServicesClient;

    @Before
    public void createClient() {
        config.setCapabilities(Arrays.asList(KieServerConstants.CAPABILITY_BPM));
        queryServicesClient = KieServicesFactory.newKieServicesClient(config).getServicesClient(QueryServicesClient.class);
    }

    @Test
    public void testFindProcessesByIdNotFound() {
        stubFor(get(urlEqualTo("/"))
                        .withHeader("Accept", equalTo("application/xml"))
                        .willReturn(aResponse()
                                            .withStatus(404)
                                            .withHeader("Content-Type", "application/xml")
                                            .withBody("<response type=\"NOTFOUND\" msg= </response>")));

        List<ProcessDefinition> list = queryServicesClient.findProcessesById("Not Exist");
        assertEquals(0, list.size());
    }
}
