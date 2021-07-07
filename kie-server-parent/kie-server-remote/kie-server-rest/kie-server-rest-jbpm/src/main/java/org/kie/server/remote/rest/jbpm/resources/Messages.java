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

package org.kie.server.remote.rest.jbpm.resources;

public class Messages {

    public static final String PROCESS_DEFINITION_NOT_FOUND = "Could not find process definition \"{0}\" in container \"{1}\"";
    public static final String NO_PROCESS_AVAILABLE_WITH_ID = "No process available with given id : {0}";
    public static final String PROCESS_DEFINITION_FETCH_ERROR = "Error when retrieving process definition \"{0}\" in container \"{1}\":: Cause: {2}";

    public static final String PROCESS_INSTANCE_NOT_FOUND = "Could not find process instance with id \"{0}\"";
    public static final String TASK_INSTANCE_NOT_FOUND = "Could not find task instance with id \"{0}\"";
    public static final String TASK_NOT_FOUND = "No task found with id {0}";
    public static final String TASK_INSTANCE_NOT_FOUND_FOR_WORKITEM = "Could not find task instance for work item with id \"{0}\"";
    public static final String TASK_ATTACHMENT_NOT_FOUND = "Could not find task attachment with id \"{0}\" attached to task with id \"{1}\"";
    public static final String TASK_COMMENT_NOT_FOUND = "Could not find task comment with id \"{0}\" attached to task with id \"{1}\"";

    public static final String WORK_ITEM_NOT_FOUND = "Could not find work item instance with id \"{0}\"";

    public static final String NODE_INSTANCE_NOT_FOUND = "Could not find node instance with id \"{0}\" within process instance with id \"{1}\"";

    public static final String NODE_NOT_FOUND = "Could not find node with id \"{0}\" within process instance with id \"{1}\"";

    public static final String TIMER_INSTANCE_NOT_FOUND = "Could not find timer instance with id \"{0}\" within process instance with id \"{1}\"";

    public static final String VAR_INSTANCE_NOT_FOUND = "Could not find variable instance with name \"{0}\" within process instance with id \"{1}\"";

    public static final String VARIABLE_INSTANCE_NOT_FOUND = "Could not find variable \"{0}\" in process instance with id \"{1}\"";

    public static final String CONTAINER_NOT_FOUND = "Could not find container \"{0}\"";
    public static final String INCORRECT_CONTAINER_ID = "Process instance with id \"{0}\" does not belong to deployment id \"{1}\"";


    public static final String CREATE_RESPONSE_ERROR = "Unable to create response: {0}";

    public static final String UNEXPECTED_ERROR = "Unexpected error during processing: {0}";
    public static final String BAD_REQUEST = "The request could not be understood by the server due to malformed syntax: {0}";

    public static final String QUERY_NOT_FOUND = "Could not find query definition with name \"{0}\"";
    public static final String QUERY_ALREADY_EXISTS = "Query definition with name \"{0}\" already exists";
}
