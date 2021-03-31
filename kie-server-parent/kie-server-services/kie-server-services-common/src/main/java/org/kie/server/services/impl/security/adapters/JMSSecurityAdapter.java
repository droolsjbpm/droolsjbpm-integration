/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.services.impl.security.adapters;

import java.io.IOException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.sasl.RealmCallback;

import org.kie.server.api.KieServerConstants;
import org.kie.server.api.security.SecurityAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JMSSecurityAdapter implements SecurityAdapter {
    private static final Logger logger = LoggerFactory.getLogger(JMSSecurityAdapter.class);

    private static final ServiceLoader<SecurityAdapter> securityAdapters = ServiceLoader.load(SecurityAdapter.class);
    private static List<SecurityAdapter> adapters = new ArrayList<>();

    private static ThreadLocal<UserDetails> currentUser = new ThreadLocal<UserDetails>();

    static  {
        for (SecurityAdapter adapter : securityAdapters) {
            adapters.add(adapter);
        }
    }

    @Override
    public String getUser(Object ... params) {
        if (currentUser.get() != null) {
            logger.debug("Returning name from JMS Adapter - {}", currentUser.get().getName());
            return currentUser.get().getName();
        }

        return null;
    }

    @Override
    public List<String> getRoles(Object ... params) {

        if (currentUser.get() != null) {
            logger.debug("Returning name from JMS Adapter - {}", currentUser.get().getName());
            return currentUser.get().getRoles();
        }

        return Collections.emptyList();
    }

    public static void login(String user, String pass) {
        if (currentUser.get() != null) {
            logger.debug("Already authenticated with user {}", currentUser.get().getName());
            return;
        }
        logger.debug("About to login as {} with pass {}", user, pass.length());
        try {
            CallbackHandler handler = new UserPassCallbackHandler(user, pass);
            final String domain = System.getProperty(KieServerConstants.KIE_SERVER_JAAS_DOMAIN, "kie-jms-login-context");
            LoginContext lc = new LoginContext( domain, handler);
            lc.login();
            Subject subject = lc.getSubject();
            logger.debug("Login Successful and subject is {}", subject);
            UserDetails userDetails = new UserDetails();
            userDetails.setName(user);
            List<String> roles = new ArrayList<String>();
            if (subject != null) {
                Set<Principal> principals = subject.getPrincipals();

                if (principals != null) {

                    roles = new ArrayList<String>();
                    for (Principal principal : principals) {
                        if (principal instanceof Group) {
                            Enumeration<? extends Principal> groups = ((Group) principal).members();

                            while (groups.hasMoreElements()) {
                                Principal groupPrincipal = (Principal) groups.nextElement();
                                roles.add(groupPrincipal.getName());
                            }
                            break;
                        }
                    }
                }
                roles.addAll(getRolesFromAdapter(subject));
            }


            userDetails.setRoles(roles);
            logger.debug("setting user details as {}", userDetails);
            currentUser.set(userDetails);

        } catch( Exception e ) {
            logger.debug( "Unable to login via JAAS with message supplied user and password", e);
        }
    }

    public static void logout() {
        currentUser.set(null);
    }

    private static class UserDetails {
        private String name;

        private List<String> roles;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        @Override
        public String toString() {
            return "UserDetails{" +
                    "name='" + name + '\'' +
                    ", roles=" + roles +
                    '}';
        }
    }

    private static class UserPassCallbackHandler implements CallbackHandler {
        private String user;
        private String pass;

        public UserPassCallbackHandler(String user, String pass) {
            this.user = user;
            this.pass = pass;
        }

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback current : callbacks) {
                if (current instanceof NameCallback) {
                    NameCallback ncb = (NameCallback) current;
                    ncb.setName(user);
                } else if (current instanceof PasswordCallback) {
                    PasswordCallback pcb = (PasswordCallback) current;
                    pcb.setPassword(pass.toCharArray());
                } else if (current instanceof RealmCallback) {
                    RealmCallback realmCallback = (RealmCallback) current;
                    realmCallback.setText(realmCallback.getDefaultText());
                }
            }

        }
    }

    protected static List<String> getRolesFromAdapter(Subject subject) {
        List<String> roles = new ArrayList<String>();

        for (SecurityAdapter adapter : adapters) {
            List<String> adapterRoles = adapter.getRoles(subject);
            if (adapterRoles != null && !adapterRoles.isEmpty()) {
                roles.addAll(adapterRoles);
            }
        }

        return roles;
    }
}
