/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.scripting;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.core.io.Resource;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
public class RefreshableResourceScriptSource implements ScriptSource {

	private final long refreshDelay;

	private final ResourceScriptSource source;

	private final AtomicLong lastModifiedChecked = new AtomicLong(System.currentTimeMillis());

	private volatile String script;


	public RefreshableResourceScriptSource(Resource resource, long refreshDelay) {
		this.refreshDelay = refreshDelay;
		this.source = new ResourceScriptSource(resource);
		try {
			this.script = this.source.getScriptAsString();
		}
		catch (IOException e) {
			this.lastModifiedChecked.set(0);
		}
	}

	public String getScriptAsString() throws IOException {
		String script = this.script;
		if (script == null) {
			script = source.getScriptAsString();
			this.script = script;
		}
		return script;
	}

	public String suggestedClassName() {
		return this.source.suggestedClassName();
	}

	public boolean isModified() {
		if (this.refreshDelay < 0) {
			return false;
		}
		long time = System.currentTimeMillis();
		if (this.refreshDelay == 0 || (time - this.lastModifiedChecked.get()) > this.refreshDelay) {
			this.lastModifiedChecked.set(time);
			boolean modified = this.source.isModified();
			if (modified) {
				this.script = null;
			}
			return modified;
		}
		return false;
	}

}
