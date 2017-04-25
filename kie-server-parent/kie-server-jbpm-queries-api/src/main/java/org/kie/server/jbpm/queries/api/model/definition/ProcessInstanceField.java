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

package org.kie.server.jbpm.queries.api.model.definition;

/**
 * ProcessInstance fields.
 * <p/>
 * These are the filterable fields in our ProcessInstanceQuery API.
 */
public enum ProcessInstanceField {

	//@formatter:off
	ID,
	CORRELATIONKEY,
	DURATION,
	END_DATE,
	EXTERNALID,
	USER_IDENTITY,
	OUTCOME,
	PARENTPROCESSINSTANCEID,
	PROCESSID,
	PROCESSINSTANCEDESCRIPTION,
	PROCESSINSTANCEID,
	PROCESSNAME,
	PROCESSTYPE,
	PROCESSVERSION,
	START_DATE,
	STATUS
	//@formatter:on
}
