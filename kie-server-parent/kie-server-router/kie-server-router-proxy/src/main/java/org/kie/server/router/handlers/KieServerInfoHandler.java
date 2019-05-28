/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.server.router.handlers;

import java.util.stream.Collectors;

import org.kie.server.router.KieServerRouterConstants;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;


public class KieServerInfoHandler implements HttpHandler {

    private static final String JAXB_RESPONSE = "<response type=\"SUCCESS\" msg=\"Kie Server info\">\n"+
            "<kie-server-info>\n"+
            "<capabilities>KieServer</capabilities>\n"+
            "<capabilities>BRM</capabilities>\n"+
            "<capabilities>BPM</capabilities>\n"+
            "<capabilities>CaseMgmt</capabilities>\n"+
            "<capabilities>BPM-UI</capabilities>\n"+
            "<capabilities>BRP</capabilities>\n"+
            "<location>\n"+
            getLocationUrl() + "\n"+
            "</location>\n"+
            "<messages/>\n"+            
            "<name>" + getRouterName() + "</name>\n"+
            "<id>" + getRouterId() + "</id>\n"+
            "<version>LATEST</version>\n"+
            "</kie-server-info>\n"+
            "</response>";
    
    private static final String JSON_RESPONSE = "{\n"+
            "  \"type\" : \"SUCCESS\",\n"+
            "  \"msg\" : \"Kie Server info\",\n"+
            "  \"result\" : {\n"+
            "    \"kie-server-info\" : {\n"+
            "      \"version\" : \"LATEST\",\n"+
            "      \"name\" : \"" + getRouterName() + "\",\n"+
            "      \"location\" : \"" + getLocationUrl() + "\",\n"+
            "      \"capabilities\" : [ \"KieServer\", \"BRM\", \"BPM\", \"CaseMgmt\", \"BPM-UI\", \"BRP\" ],\n"+     
            "      \"id\" : \"" + getRouterId() + "\"\n"+
            "    }\n"+
            "  }\n"+
            "}";
    
    private static final String XSTREAM_RESPONSE = "<org.kie.server.api.model.ServiceResponse>"+
                           "<type>SUCCESS</type>\n"+
                           "<msg>Kie Server info</msg>\n"+
                           "<result class=\"kie-server-info\">\n"+
                           "<serverId>" + getRouterId() + "</serverId>\n"+
                           "<version>LATEST</version>\n"+
                           "<name>" + getRouterName() + "</name>\n"+
                           "<location>\n"+
                           getLocationUrl() + "\n"+
                           "</location>\n"+
                           "<capabilities>\n"+
                           "<string>KieServer</string>\n"+
                           "<string>BRM</string>\n"+
                           "<string>BPM</string>\n"+
                           "<string>CaseMgmt</string>\n"+
                           "<string>BPM-UI</string>\n"+
                           "<string>BRP</string>\n"+
                           "</capabilities>\n"+                           
                           "</result>\n"+
                           "</org.kie.server.api.model.ServiceResponse>"; 
    
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.getRequestMethod().equals(HttpString.tryFromString("OPTIONS"))) {
            String response = "GET, OPTIONS";
            
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain;charset=UTF-8");
            exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, response.getBytes("UTF-8").length);
            exchange.getResponseHeaders().put(Headers.ALLOW, response);
            exchange.getResponseSender().send(response);
        }
        HeaderValues accept = exchange.getRequestHeaders().get(Headers.ACCEPT);
        HeaderValues kieContentType = exchange.getRequestHeaders().get("X-KIE-ContentType");
        
        String acceptRequest = "";
        if (accept != null) {
            acceptRequest = accept.stream().collect(Collectors.joining(","));
        }
        
        String kieContentTypeRequest = "";
        if (kieContentType != null) {
            kieContentTypeRequest = kieContentType.stream().collect(Collectors.joining(","));
        }
        
        String response = JAXB_RESPONSE;
        String contentTypeResponse = "application/xml";
        
        if (acceptRequest.toLowerCase().contains("json") || kieContentTypeRequest.toLowerCase().contains("json")) {
            response = JSON_RESPONSE;
            contentTypeResponse = "application/json";
        } else if (kieContentTypeRequest.toLowerCase().contains("xstream")) {
            response = XSTREAM_RESPONSE;
            contentTypeResponse = "application/xml";
        }
        
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentTypeResponse);
        exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, response.getBytes("UTF-8").length);
        exchange.getResponseSender().send(response);

    }

    public static String getLocationUrl() {
        String externalUrl = System.getProperty(KieServerRouterConstants.ROUTER_EXTERNAL_URL);

        if(externalUrl == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("http://");
            sb.append(System.getProperty(KieServerRouterConstants.ROUTER_HOST, "localhost"));
            sb.append(":");
            sb.append(System.getProperty(KieServerRouterConstants.ROUTER_PORT, "9000"));
            externalUrl = sb.toString();
        }

        return externalUrl;
    }

    public static String getRouterId() {
        return System.getProperty(KieServerRouterConstants.ROUTER_ID, "kie-server-router");
    }

    public static String getRouterName() {
        return System.getProperty(KieServerRouterConstants.ROUTER_NAME, "KIE Server Router");
    }
}
