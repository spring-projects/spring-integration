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

package org.springframework.integration.history;

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
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class MessageHistoryConfigurer implements SmartLifecycle, BeanFactoryAware {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile String[] componentNamePatterns = new String[] { "*" };

	private final Set<String> currentlyTrackedComponentNames = new HashSet<String>();

	private volatile BeanFactory beanFactory;

	private volatile boolean running;

	private volatile boolean autoStartup = true;

	private int phase = Integer.MIN_VALUE;

	private final Object lifecycleMonitor = new Object();


	public void setComponentNamePatterns(String[] componentNamePatterns) {
		Assert.notEmpty(componentNamePatterns, "componentNamePatterns must not be empty");
		this.componentNamePatterns = componentNamePatterns;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	private static Collection<TrackableComponent> getTrackableComponents(ListableBeanFactory beanFactory) {
		return BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, TrackableComponent.class).values();
	}


	/*
	 * SmartLifecycle implementation
	 */

	public boolean isRunning() {
		return this.running;
	}

	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	public int getPhase() {
		return this.phase;
	}

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
			}
		}
	}

	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

}
