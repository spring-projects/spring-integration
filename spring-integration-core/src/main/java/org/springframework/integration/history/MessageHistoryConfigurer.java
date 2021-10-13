/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.history;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.integration.support.management.ManageableSmartLifecycle;
import org.springframework.integration.support.management.TrackableComponent;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 */
@ManagedResource
@IntegrationManagedResource
public class MessageHistoryConfigurer implements ManageableSmartLifecycle, BeanFactoryAware,
		DestructionAwareBeanPostProcessor {

	private static final Log LOGGER = LogFactory.getLog(MessageHistoryConfigurer.class);

	private final Set<TrackableComponent> currentlyTrackedComponents = ConcurrentHashMap.newKeySet();

	private String[] componentNamePatterns = { "*" };

	private ListableBeanFactory beanFactory;

	private boolean autoStartup = true;

	private int phase = Integer.MIN_VALUE;

	private volatile boolean running;


	/**
	 * The patterns for which components will be tracked; default '*' (all trackable
	 * components). Cannot be changed if {@link #isRunning()}; invoke {@link #stop()} first.
	 * @param componentNamePatterns The patterns.
	 */
	public void setComponentNamePatterns(String[] componentNamePatterns) {
		Assert.notEmpty(componentNamePatterns, "componentNamePatterns must not be empty");
		Assert.state(!this.running, "'componentNamePatterns' cannot be changed without invoking stop() first");
		String[] trimmedAndSortedComponentNamePatterns = componentNamePatterns.clone();
		for (int i = 0; i < componentNamePatterns.length; i++) {
			trimmedAndSortedComponentNamePatterns[i] = trimmedAndSortedComponentNamePatterns[i].trim();
		}
		Arrays.sort(trimmedAndSortedComponentNamePatterns);
		this.componentNamePatterns = trimmedAndSortedComponentNamePatterns;
	}

	/**
	 * A comma-delimited list of patterns for which components will be tracked; default '*' (all trackable
	 * components). Cannot be changed if {@link #isRunning()}; invoke {@link #stop()} first.
	 * @param componentNamePatterns The patterns.
	 */
	@ManagedAttribute(description = "comma-delimited list of patterns; must invoke stop() before changing.")
	public void setComponentNamePatternsString(String componentNamePatterns) {
		setComponentNamePatterns(StringUtils.delimitedListToStringArray(componentNamePatterns, ",", " "));
	}

	@ManagedAttribute
	public String getComponentNamePatternsString() {
		return StringUtils.arrayToCommaDelimitedString(this.componentNamePatterns);
	}


	/**
	 * The patterns for which components will be tracked; default '*' (all trackable
	 * components). Cannot be changed if {@link #isRunning()}; invoke {@link #stop()} first.
	 * All members of the set must canonically represent the same patterns - allows multiple
	 * EnableMessageHistory annotations as long they all have the same patterns.
	 * @param componentNamePatternsSet A set of lists of comma-delimited patterns.
	 */
	public void setComponentNamePatternsSet(Set<String> componentNamePatternsSet) {
		Assert.notNull(componentNamePatternsSet, "'componentNamePatternsSet' must not be null");
		Assert.state(!this.running, "'componentNamePatternsSet' cannot be changed without invoking stop() first");
		String patterns = String.join(",", componentNamePatternsSet);
		this.componentNamePatterns = StringUtils.delimitedListToStringArray(patterns, ",", " ");
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ListableBeanFactory.class, beanFactory,
				"The provided 'beanFactory' must be of 'ListableBeanFactory' type.");
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof TrackableComponent && this.running) {
			trackComponentIfAny((TrackableComponent) bean);
		}
		return bean;
	}

	private void trackComponentIfAny(TrackableComponent component) {
		String componentName = component.getComponentName();
		boolean shouldTrack = PatternMatchUtils.simpleMatch(this.componentNamePatterns, componentName);
		component.setShouldTrack(shouldTrack);
		if (shouldTrack) {
			this.currentlyTrackedComponents.add(component);
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Enabling MessageHistory tracking for component '" + componentName + "'");
			}
		}
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return bean instanceof TrackableComponent;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		this.currentlyTrackedComponents.remove(bean);
	}

	/*
	 * SmartLifecycle implementation
	 */

	@Override
	public boolean isRunning() {
		return this.running;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	@ManagedOperation
	@Override
	public void start() {
		synchronized (this.currentlyTrackedComponents) {
			if (!this.running) {
				for (TrackableComponent component : getTrackableComponents(this.beanFactory)) {
					trackComponentIfAny(component);
					this.running = true;
				}
			}
		}
	}

	@ManagedOperation
	@Override
	public void stop() {
		synchronized (this.currentlyTrackedComponents) {
			if (this.running) {
				this.currentlyTrackedComponents.forEach(component -> {
					component.setShouldTrack(false);
					if (LOGGER.isInfoEnabled()) {
						LOGGER.info("Disabling MessageHistory tracking for component '"
								+ component.getComponentName() + "'");
					}
				});

				this.currentlyTrackedComponents.clear();
				this.running = false;
			}
		}
	}

	private static Collection<TrackableComponent> getTrackableComponents(ListableBeanFactory beanFactory) {
		return BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, TrackableComponent.class).values();
	}

}
