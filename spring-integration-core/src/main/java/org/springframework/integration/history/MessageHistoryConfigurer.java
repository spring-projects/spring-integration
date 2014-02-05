/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.history;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.context.SmartLifecycle;
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
public class MessageHistoryConfigurer implements SmartLifecycle, BeanFactoryAware {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile String[] componentNamePatterns = new String[] { "*" };

	private volatile boolean componentNamePatternsExplicitlySet;

	private final Set<String> currentlyTrackedComponentNames = new HashSet<String>();

	private volatile BeanFactory beanFactory;

	private volatile boolean running;

	private volatile boolean autoStartup = true;

	private final int phase = Integer.MIN_VALUE;

	private final Object lifecycleMonitor = new Object();


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
		Assert.isTrue(!this.componentNamePatternsExplicitlySet
				|| Arrays.equals(this.componentNamePatterns, trimmedAndSortedComponentNamePatterns),
					"When more than one message history definition " +
					"(@EnableMessageHistory or <message-history>)" +
					" is found in the context, they all must have the same 'componentNamePatterns'");
		this.componentNamePatterns = trimmedAndSortedComponentNamePatterns;
		this.componentNamePatternsExplicitlySet = true;
	}

	/**
	 * A comma-delimited list of patterns for which components will be tracked; default '*' (all trackable
	 * components). Cannot be changed if {@link #isRunning()}; invoke {@link #stop()} first.
	 * @param componentNamePatterns The patterns.
	 */
	@ManagedAttribute(description="comma-delimited list of patterns; must invoke stop() before changing.")
	public void setComponentNamePatternsString(String componentNamePatterns) {
		this.setComponentNamePatterns(StringUtils.delimitedListToStringArray(componentNamePatterns, ",", " "));
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
		for (String s : componentNamePatternsSet) {
			String[] componentNamePatterns = StringUtils.delimitedListToStringArray(s, "," , " ");
			Arrays.sort(componentNamePatterns);
			if (this.componentNamePatternsExplicitlySet
					&& !Arrays.equals(this.componentNamePatterns, componentNamePatterns)) {
				throw new BeanDefinitionValidationException("When more than one message history definition " +
						"(@EnableMessageHistory or <message-history>)" +
						" is found in the context, they all must have the same 'componentNamePatterns'");
			}
			else {
				this.componentNamePatterns = componentNamePatterns;
				this.componentNamePatternsExplicitlySet = true;
			}
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	private static Collection<TrackableComponent> getTrackableComponents(ListableBeanFactory beanFactory) {
		return BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, TrackableComponent.class).values();
	}


	/*
	 * SmartLifecycle implementation
	 */

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	@ManagedOperation
	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!this.running && this.beanFactory instanceof ListableBeanFactory) {
				for (TrackableComponent component : getTrackableComponents((ListableBeanFactory) beanFactory)) {
					String componentName = component.getComponentName();
					boolean shouldTrack = PatternMatchUtils.simpleMatch(this.componentNamePatterns, componentName);
					component.setShouldTrack(shouldTrack);
					if (shouldTrack) {
						this.currentlyTrackedComponentNames.add(componentName);
						if (this.logger.isInfoEnabled()) {
							this.logger.info("Enabling MessageHistory tracking for component '" + componentName + "'");
						}
					}
				}
				this.running = true;
			}
		}
	}

	@ManagedOperation
	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.running && this.beanFactory instanceof ListableBeanFactory) {
				for (TrackableComponent component : getTrackableComponents((ListableBeanFactory) beanFactory)) {
					String componentName = component.getComponentName();
					if (this.currentlyTrackedComponentNames.contains(componentName)) {
						component.setShouldTrack(false);
						if (this.logger.isInfoEnabled()) {
							this.logger.info("Disabling MessageHistory tracking for component '" + componentName + "'");
						}
					}
				}
				this.currentlyTrackedComponentNames.clear();
				this.running = false;
				this.componentNamePatternsExplicitlySet = false; // allow pattern changes
			}
		}
	}

	@Override
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

}
