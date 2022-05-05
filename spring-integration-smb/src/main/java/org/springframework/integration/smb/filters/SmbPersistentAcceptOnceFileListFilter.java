/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.smb.filters;

import org.springframework.integration.file.filters.AbstractPersistentAcceptOnceFileListFilter;
import org.springframework.integration.metadata.ConcurrentMetadataStore;

import jcifs.smb.SmbFile;

/**
 * Implementation of {@link AbstractPersistentAcceptOnceFileListFilter} for SMB.
 *
 * @author Prafull Kumar Soni
 */
public class SmbPersistentAcceptOnceFileListFilter extends AbstractPersistentAcceptOnceFileListFilter<SmbFile> {


	public SmbPersistentAcceptOnceFileListFilter(ConcurrentMetadataStore store, String prefix) {
		super(store, prefix);
	}

	@Override
	protected long modified(SmbFile file) {
		return file.getLastModified();
	}

	@Override
	protected String fileName(SmbFile file) {
		return file.getName();
	}

}
