/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.file.entries;

import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.List;


/**
 * A convenience base class for any {@link EntryListFilter} whose criteria can be
 * evaluated against each File in isolation. If the entire List of files is
 * required for evaluation, implement the {@link EntryListFilter} interface directly.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Josh Long
 */
public abstract class AbstractEntryListFilter<T> implements InitializingBean, EntryListFilter<T> {
	public abstract boolean accept(T t);

	public List<T> filterEntries(T[] entries) {
		List<T> accepted = new ArrayList<T>();

		if (entries != null) {
			for (T t : entries) {
				if (this.accept(t)) {
					accepted.add(t);
				}
			}
		}

		return accepted;
	}

	public void afterPropertiesSet() throws Exception {
		// its all you!
	}
}
