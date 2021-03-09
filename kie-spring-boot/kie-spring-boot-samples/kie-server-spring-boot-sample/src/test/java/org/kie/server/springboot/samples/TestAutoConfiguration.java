/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.server.springboot.samples;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.kie.internal.identity.IdentityProvider;
import org.springframework.context.annotation.Bean;

public class TestAutoConfiguration {

    @Bean
    public IdentityProvider identityProvider() {
        
        return new IdentityProvider() {
            private Stack<String> contextUsers = new Stack<>();
            
            @Override
            public void setContextIdentity(String userId) {
                contextUsers.push(userId);
            }

            @Override
            public void removeContextIdentity() {
                contextUsers.pop();
            }
            
            private List<String> roles = Arrays.asList("PM", "HR");
            
            @Override
            public boolean hasRole(String arg0) {
                return roles.contains(arg0);
            }
            
            @Override
            public List<String> getRoles() {
                return roles;
            }
            
            @Override
            public String getName() {
                if(!contextUsers.isEmpty()) {
                   return contextUsers.peek();
                }
                return "john";
            }
        };
    }
    
}

