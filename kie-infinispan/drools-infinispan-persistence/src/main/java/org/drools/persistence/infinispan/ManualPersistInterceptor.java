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

package org.drools.persistence.infinispan;

import org.drools.core.command.SingleSessionCommandService;
import org.drools.core.command.impl.AbstractInterceptor;
import org.drools.core.command.impl.ExecutableCommand;
import org.drools.core.fluent.impl.InternalExecutable;
import org.drools.persistence.PersistableRunner;
import org.drools.persistence.PersistenceContext;
import org.drools.persistence.PersistenceContextManager;
import org.drools.persistence.SessionMarshallingHelper;
import org.drools.persistence.info.SessionInfo;
import org.kie.api.runtime.Context;
import org.kie.api.runtime.Executable;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.RequestContext;

public class ManualPersistInterceptor extends AbstractInterceptor {

	private final SingleSessionCommandService interceptedService;
	
	public ManualPersistInterceptor(SingleSessionCommandService decorated ) {
		this.interceptedService = decorated;
	}

	public RequestContext execute( Executable executable, RequestContext ctx ) {
		executeNext(executable, ctx);
		try {
	    	KieSession ksession = interceptedService.getKieSession();
	    	java.lang.reflect.Field sessionInfoField = PersistableRunner.class.getDeclaredField( "sessionInfo" );
	    	sessionInfoField.setAccessible(true);
	    	java.lang.reflect.Field jpmField = PersistableRunner.class.getDeclaredField("jpm");
	    	jpmField.setAccessible(true);
	    	Object jpm = jpmField.get(interceptedService);
	    	Object sessionInfo = sessionInfoField.get(interceptedService);
	    	if ( ( (InternalExecutable) executable ).canRunInTransaction() ) {
	    		executeNext(new SingleCommandExecutable( new PersistCommand(sessionInfo, jpm, ksession) ), ctx);
	    	}
		} catch (Exception e) {
			throw new RuntimeException("Couldn't force persistence of session info", e);
		}
		return ctx;
	}

	public static class PersistCommand implements ExecutableCommand<Void> {
		
		private final SessionInfo sessionInfo;
		private final PersistenceContext persistenceContext;
		private final KieSession ksession;
		
		public PersistCommand(Object sessionInfo, Object jpm, KieSession ksession) {
			this.sessionInfo = (SessionInfo) sessionInfo;
			this.persistenceContext = ((PersistenceContextManager) jpm).getApplicationScopedPersistenceContext();
			this.ksession = ksession;
		}
		
		@Override
		public Void execute(Context context ) {
			/*if (sessionInfo.getId() == null || sessionInfo.getId() <= 0) {
				sessionInfo.setJPASessionMashallingHelper(new SessionMarshallingHelper(
						ksession, ksession.getSessionConfiguration()));
			} else {
				sessionInfo.setJPASessionMashallingHelper(new SessionMarshallingHelper(
						ksession.getKieBase(), ksession.getSessionConfiguration(), 
						ksession.getEnvironment()));
			}*/
			if (sessionInfo.getJPASessionMashallingHelper() == null) {
				sessionInfo.setJPASessionMashallingHelper(new SessionMarshallingHelper(
						ksession.getKieBase(), ksession.getSessionConfiguration(), 
						ksession.getEnvironment()));
			}
			sessionInfo.transform();
			persistenceContext.persist(sessionInfo);
			return null;
		}
	}

	public SingleSessionCommandService getInterceptedService() {
		return interceptedService;
	}
	
}
