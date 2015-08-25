package org.kie.server.controller.client;

import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.controller.api.model.KieServerInstance;
import org.kie.server.controller.api.model.KieServerInstanceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieServerControllerClient {

    private static Logger logger = LoggerFactory.getLogger(KieServerControllerClient.class);

    private ClientExecutor executor;
    private String controllerBaseUrl;
    private MarshallingFormat format = MarshallingFormat.JAXB;
    private CloseableHttpClient httpClient;

    public KieServerControllerClient( String controllerBaseUrl, String login, String password ) {
        URL url;
        try {
            url = new URL(controllerBaseUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed controller URL was specified: '" + controllerBaseUrl + "'!", e);
        }

        this.controllerBaseUrl = controllerBaseUrl;
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        if (login != null) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(url.getHost(), url.getPort()),
                    new UsernamePasswordCredentials(login, password));
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }
        this.httpClient = httpClientBuilder.build();
        this.executor = new ApacheHttpClient4Executor(httpClient);
    }

    public KieServerInstance getKieServerInstance(String kieServerInstanceId) {
        return makeGetRequestAndCreateCustomResponse(controllerBaseUrl + "/admin/server/" + kieServerInstanceId, KieServerInstance.class);
    }

    public KieServerInstance createKieServerInstance(KieServerInfo kieServerInfo) {
        return makePutRequestAndCreateCustomResponse(controllerBaseUrl + "/admin/server/" + kieServerInfo.getServerId(), kieServerInfo, KieServerInstance.class);
    }

    public void deleteKieServerInstance(String kieServerInstanceId) {
        makeDeleteRequest(controllerBaseUrl + "/admin/server/" + kieServerInstanceId);
    }

    public KieServerInstanceList listKieServerInstances() {
        return makeGetRequestAndCreateCustomResponse(controllerBaseUrl + "/admin/servers", KieServerInstanceList.class);
    }

    public KieContainerResource getContainerInfo(String kieServerInstanceId, String containerId) {
        return makeGetRequestAndCreateCustomResponse(controllerBaseUrl + "/admin/server/" + kieServerInstanceId + "/containers/" + containerId, KieContainerResource.class);
    }

    public KieContainerResource createContainer(String kieServerInstanceId, String containerId, KieContainerResource container) {
        return makePutRequestAndCreateCustomResponse(controllerBaseUrl + "/admin/server/" + kieServerInstanceId + "/containers/" + containerId, container, KieContainerResource.class);
    }

    public void disposeContainer(String kieServerInstanceId, String containerId) {
        makeDeleteRequest(controllerBaseUrl + "/admin/server/" + kieServerInstanceId + "/containers/" + containerId);
    }

    private <T> T makeGetRequestAndCreateCustomResponse(String uri, Class<T> resultType) {
        ClientRequest clientRequest = new ClientRequest(uri, executor);
        ClientResponse<T> response;

        try {
            response = clientRequest.accept(getMediaType(format)).get(resultType);

            if ( response.getStatus() == Response.Status.OK.getStatusCode() ) {
                return response.getEntity();
            } else {
                response.releaseConnection();
                throw createExceptionForUnexpectedResponseCode( clientRequest, response );
            }

        } catch (Exception e) {
            throw createExceptionForUnexpectedFailure(clientRequest, e);
        }
    }

    private void makeDeleteRequest(String uri) {
        ClientRequest clientRequest = new ClientRequest(uri, executor);
        ClientResponse<?> response;

        try {
            response = clientRequest.accept(getMediaType(format)).delete();
            response.releaseConnection();

            if ( response.getStatus() != Response.Status.NO_CONTENT.getStatusCode() ) {
                throw createExceptionForUnexpectedResponseCode( clientRequest, response );
            }

        } catch (Exception e) {
            throw createExceptionForUnexpectedFailure(clientRequest, e);
        }
    }

    private <T> T makePutRequestAndCreateCustomResponse(String uri, Object bodyObject, Class<T> resultType) {
        ClientRequest clientRequest = new ClientRequest(uri, executor);
        ClientResponse<T> response;

        try {
            response = clientRequest.accept(getMediaType(format))
                    .body(getMediaType(format), bodyObject).put(resultType);

            if ( response.getStatus() == Response.Status.CREATED.getStatusCode() ) {
                return response.getEntity();
            } else {
                response.releaseConnection();
                throw createExceptionForUnexpectedResponseCode( clientRequest, response );
            }

        } catch (Exception e) {
            throw createExceptionForUnexpectedFailure(clientRequest, e);
        }
    }

    private RuntimeException createExceptionForUnexpectedResponseCode(
            ClientRequest request,
            ClientResponse<?> response) {
        String summaryMessage = "Unexpected HTTP response code when requesting URI '" + getClientRequestUri(request) + "'! Response code: " +
                response.getStatus();
        logger.debug( summaryMessage);
        return new RuntimeException(summaryMessage);
    }

    private RuntimeException createExceptionForUnexpectedFailure(
            ClientRequest request, Exception e) {
        String summaryMessage = "Unexpected exception when requesting URI '" + getClientRequestUri(request) + "'!";
        logger.debug( summaryMessage);
        return new RuntimeException(summaryMessage, e);
    }

    private String getClientRequestUri(ClientRequest clientRequest) {
        String uri;
        try {
            uri = clientRequest.getUri();
        } catch (Exception e) {
            throw new RuntimeException("Malformed client URL was specified!", e);
        }
        return uri;
    }

    public void close() {
        try {
            executor.close();
            httpClient.close();
        } catch (Exception e) {
            logger.error("Exception thrown while closing resources!", e);
        }
    }

    public MarshallingFormat getMarshallingFormat() {
        return format;
    }

    public void setMarshallingFormat(MarshallingFormat format) {
        this.format = format;
    }

    private String getMediaType( MarshallingFormat format ) {
        switch ( format ) {
            case JAXB: return MediaType.APPLICATION_XML;
            case JSON: return MediaType.APPLICATION_JSON;
            default: return MediaType.APPLICATION_XML;
        }
    }
}
