/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.services.jbpm.queries;

import org.kie.server.services.jbpm.queries.util.QueryStrategy;

/**
 * Provides the context for query execution, including things like {@link QueryStrategy}, query-name, mapper-name, etc.
 * 
 * @author <a href="mailto:duncan.doyle@redhat.com">Duncan Doyle</a>
 */
public interface QueryCallback {

	public QueryStrategy getQueryStrategy();
	
	public String getQueryName();
	
	public String getMapperName();

}
