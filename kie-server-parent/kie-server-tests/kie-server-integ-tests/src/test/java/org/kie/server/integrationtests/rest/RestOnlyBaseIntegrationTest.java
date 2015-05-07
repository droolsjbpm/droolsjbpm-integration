package org.kie.server.integrationtests.rest;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.integrationtests.KieServerBaseIntegrationTest;
import org.kie.server.integrationtests.config.TestConfig;

import javax.ws.rs.core.MediaType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public abstract class RestOnlyBaseIntegrationTest extends KieServerBaseIntegrationTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{{MarshallingFormat.JAXB}, {MarshallingFormat.JSON}});
    }

    @Parameterized.Parameter
    public MarshallingFormat marshallingFormat;

    protected ClientRequest newRequest(String uriString) {
        URI uri;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Malformed request URI was specified: '" + uriString + "'!", e);
        }
        if (TestConfig.isLocalServer()) {
            return new ClientRequest(uriString);
        } else {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(uri.getHost(), uri.getPort()),
                    new UsernamePasswordCredentials(TestConfig.getUsername(), TestConfig.getPassword())
            );
            HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).build();
            ApacheHttpClient4Executor executor = new ApacheHttpClient4Executor(client);
            return new ClientRequest(uriString, executor);
        }
    }

    @Override
    protected KieServicesClient createDefaultClient() {
        KieServicesConfiguration config;
        if (TestConfig.isLocalServer()) {
            config = KieServicesFactory.newRestConfiguration(TestConfig.getHttpUrl(), null, null);
        } else {
            config = KieServicesFactory.newRestConfiguration(TestConfig.getHttpUrl(), TestConfig.getUsername(), TestConfig.getPassword());
        }
        config.setMarshallingFormat(marshallingFormat);
        return KieServicesFactory.newKieServicesClient(config);
    }

    protected MediaType getMediaType() {
        switch (marshallingFormat) {
            case JAXB:
                return MediaType.APPLICATION_XML_TYPE;
            case JSON:
                return MediaType.APPLICATION_JSON_TYPE;
            case XSTREAM:
                return MediaType.APPLICATION_XML_TYPE;
            default:
                throw new RuntimeException("Unrecognized marshalling format: " + marshallingFormat);
        }
    }

}
