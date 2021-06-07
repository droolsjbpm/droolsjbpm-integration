/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.controller.websocket.common.encoder;

import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;
import org.kie.server.controller.api.model.KieServerControllerServiceResponse;
import org.kie.server.controller.websocket.common.WebSocketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieServerControllerServiceResponseEncoder implements Encoder.Text<KieServerControllerServiceResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KieServerControllerServiceResponseEncoder.class);

    @Override
    public String encode(final KieServerControllerServiceResponse object) throws EncodeException {
        LOGGER.debug("Encoding object: {}",
                     object);
        return WebSocketUtils.marshal(object);
    }

    @Override
    public void init(final EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
