/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.groovy;

import groovy.lang.GString;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.Message;
import org.springframework.integration.scripting.AbstractScriptExecutingMessageProcessor;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.groovy.GroovyObjectCustomizer;
import org.springframework.scripting.groovy.GroovyScriptFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class GroovyScriptExecutingMessageProcessor extends AbstractScriptExecutingMessageProcessor<Object> implements BeanFactoryAware, BeanClassLoaderAware {

	private final GroovyScriptFactory scriptFactory;

	private final VariableBindingGroovyObjectCustomizerDecorator
			customizerDecorator = new VariableBindingGroovyObjectCustomizerDecorator();

	private volatile ScriptSource scriptSource;


	/**
	 * Create a processor for the given {@link ScriptSource} that will use a
	 * DefaultScriptVariableGenerator.
	 */
	public GroovyScriptExecutingMessageProcessor(ScriptSource scriptSource) {
		super();
		this.scriptSource = scriptSource;
		this.scriptFactory = new GroovyScriptFactory(this.getClass().getSimpleName(), this.customizerDecorator);
	}

	/**
	 * Create a processor for the given {@link ScriptSource} that will use the provided
	 * ScriptVariableGenerator.
	 */
	public GroovyScriptExecutingMessageProcessor(ScriptSource scriptSource, ScriptVariableGenerator scriptVariableGenerator) {
		super(scriptVariableGenerator);
		this.scriptSource = scriptSource;
		this.scriptFactory = new GroovyScriptFactory(this.getClass().getSimpleName(), this.customizerDecorator);
	}


	/**
	 * Sets a {@link GroovyObjectCustomizer} for this processor.
	 */
	public void setCustomizer(GroovyObjectCustomizer customizer) {
		this.customizerDecorator.setCustomizer(customizer);
	}

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        scriptFactory.setBeanClassLoader(classLoader);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        scriptFactory.setBeanFactory(beanFactory);
    }

	@Override
	protected ScriptSource getScriptSource(Message<?> message) {
		return this.scriptSource;
	}

	@Override
	protected Object executeScript(ScriptSource scriptSource, Map<String, Object> variables) throws Exception {
		Assert.notNull(scriptSource, "scriptSource must not be null");
		synchronized (this) {
			if (!CollectionUtils.isEmpty(variables)) {
				this.customizerDecorator.setVariables(variables);
			}
			Object result = this.scriptFactory.getScriptedObject(scriptSource, null);
			return (result instanceof GString) ? result.toString() : result;
		}
	}

}
