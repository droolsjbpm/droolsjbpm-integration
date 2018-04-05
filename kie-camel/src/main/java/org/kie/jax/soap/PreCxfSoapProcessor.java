/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.jax.soap;

import javax.xml.soap.SOAPMessage;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreCxfSoapProcessor implements Processor {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(PreCxfSoapProcessor.class);

    public void process(Exchange exchange) throws Exception {
        exchange.setPattern(ExchangePattern.InOut);
        BindingOperationInfo boi = (BindingOperationInfo)exchange.getProperty(BindingOperationInfo.class.toString());
        if (boi != null) {
            LOGGER.info("boi.isUnwrapped" + boi.isUnwrapped());
        }
        SOAPMessage soapMessage = (SOAPMessage)exchange.getIn().getBody();
        exchange.getOut().setBody(soapMessage.getSOAPBody().getTextContent());
    }
}
