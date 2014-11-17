package org.kie.server.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;

import java.io.IOException;
import java.net.ServerSocket;

public abstract class BaseKieServicesClientTest {

    private final int port = findFreePort();
    protected final String mockServerBaseUri = "http://localhost:" + port;

    // we need the rule (and thus the mock server) per class so that the tests can be executed in parallel without affecting
    // one another. That means each test class needs it own port that the server listens on
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(port);

    public static int findFreePort() {
        int port = 0;
        try {
            ServerSocket server =
                    new ServerSocket(0);
            port = server.getLocalPort();
            server.close();
        } catch (IOException e) {
            // failed to dynamically allocate port, try to use hard coded one
            port = 9789;
        }
        System.out.println("Allocating port: " + port);
        return port;
    }
}
