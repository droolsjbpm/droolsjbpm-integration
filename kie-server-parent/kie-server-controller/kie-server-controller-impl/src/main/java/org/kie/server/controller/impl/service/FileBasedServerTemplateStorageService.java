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
package org.kie.server.controller.impl.service;

import org.kie.server.controller.api.service.PersistingServerTemplateStorageService;
import org.kie.server.controller.api.storage.KieServerTemplateStorage;
import org.kie.server.controller.impl.storage.FileBasedKieServerTemplateStorage;

public class FileBasedServerTemplateStorageService	implements PersistingServerTemplateStorageService {
	private static FileBasedKieServerTemplateStorage storage;
	
	public FileBasedServerTemplateStorageService() {
		storage = FileBasedKieServerTemplateStorage.getInstance();
	}

	@Override
	public KieServerTemplateStorage getTemplateStorage() {
		return storage;
	}

}
