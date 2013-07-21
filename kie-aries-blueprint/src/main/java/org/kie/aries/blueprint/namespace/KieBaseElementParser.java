/*
 * Copyright 2013 JBoss Inc
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
package org.kie.aries.blueprint.namespace;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.drools.core.util.StringUtils;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class KieBaseElementParser extends AbstractElementParser {

    @Override
    public ComponentMetadata parseElement(ParserContext context, Element element) {
        String id = getId(context, element);

        String releaseIdRef = element.getAttribute("releaseId");

        MutableBeanMetadata beanMetadata = (MutableBeanMetadata) context.createMetadata(BeanMetadata.class);
        beanMetadata.setClassName("org.kie.aries.blueprint.factorybeans.KieObjectsFactoryBean");
        beanMetadata.setFactoryMethod("fetchKBase");
        beanMetadata.setId(id);

        beanMetadata.addArgument(createValue(context, id),null,0);

        if (!StringUtils.isEmpty(releaseIdRef)) {
            beanMetadata.addArgument(createRef(context, releaseIdRef),null,1);
        } else {
            beanMetadata.addArgument(createNullMetadata(),null,1);
        }

        beanMetadata.setActivation(ComponentMetadata.ACTIVATION_LAZY);

        String prefix = element.getPrefix();
        NodeList ksessionNodeList = element.getElementsByTagName(prefix+":ksession");
        if (ksessionNodeList != null) {
            for (int i=0; i < ksessionNodeList.getLength(); i++){
                Node ksessionNode = ksessionNodeList.item(i);
                if (ksessionNode instanceof Element) {
                    Element ksessionElement = (Element) ksessionNode;
                    ksessionElement.setAttribute(KieSessionElementParser.ID_ATTRIBUTE, ksessionElement.getAttribute("name"));
                    ksessionElement.setAttribute(KieSessionElementParser.ATTRIBUTE_KBASE_REF, id);
                    context.getComponentDefinitionRegistry().registerComponentDefinition(new KieSessionElementParser().parseElement(context, ksessionElement));
                }
            }
        }
        return beanMetadata;
    }

}
