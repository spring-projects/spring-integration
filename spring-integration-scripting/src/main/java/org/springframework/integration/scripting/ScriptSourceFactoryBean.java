/*
 * Copyright 2013 the original author or authors.
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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.scripting.support.StaticScriptSource;

/**
 * {@link FactoryBean} for {@link ScriptSource} based on {@link Resource} and {@code #refreshDelay}.
 * If {@code #refreshDelay < 0} produces {@link StaticScriptSource} based on
 * 'script text' from {@link Resource}, otherwise produces {@link RefreshableResourceScriptSource}.
 *
 * @author Artem Bilan
 * @since 3.0
 */
public class ScriptSourceFactoryBean implements FactoryBean<ScriptSource>, InitializingBean {

	private final Resource resource;

	private final long refreshDelay;

	private ScriptSource scriptSource;

	public ScriptSourceFactoryBean(Resource resource) {
		this(resource, -1);
	}

	public ScriptSourceFactoryBean(Resource resource, long refreshDelay) {
		this.resource = resource;
		this.refreshDelay = refreshDelay;
	}

	@Override
	public ScriptSource getObject() throws Exception {
		return this.scriptSource;
	}

	@Override
	public Class<?> getObjectType() {
		return ScriptSource.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.refreshDelay >= 0) {
			this.scriptSource = new RefreshableResourceScriptSource(resource, refreshDelay);
		}
		else {
			ScriptSource script = new ResourceScriptSource(resource);
			this.scriptSource = new StaticScriptSource(script.getScriptAsString(), script.suggestedClassName());
		}
	}
}
