/*
 * Copyright 2015-2021 the original author or authors.
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

package org.springframework.integration.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;
import org.springframework.context.Phased;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.leader.event.AbstractLeaderEvent;
import org.springframework.integration.leader.event.OnGrantedEvent;
import org.springframework.integration.leader.event.OnRevokedEvent;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Bulk start/stop {@link SmartLifecycle} in a particular role in phase order.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
public class SmartLifecycleRoleController implements ApplicationListener<AbstractLeaderEvent>,
		ApplicationContextAware {

	private static final Log LOGGER = LogFactory.getLog(SmartLifecycleRoleController.class);

	private static final String IN_ROLE = " in role ";

	private final MultiValueMap<String, SmartLifecycle> lifecycles = new LinkedMultiValueMap<>();

	private final MultiValueMap<String, String> lazyLifecycles = new LinkedMultiValueMap<>();

	private ApplicationContext applicationContext;

	/**
	 * Construct an instance without any lifecycle initially: can be added later on via
	 * {@link #addLifecycleToRole(String, SmartLifecycle)}.
	 * @since 5.5
	 */
	public SmartLifecycleRoleController() {
	}

	/**
	 * Construct an instance with the provided lists of roles and lifecycles, which must be of equal length.
	 * @param roles the roles.
	 * @param lifecycles the lifecycles corresponding to the roles.
	 */
	public SmartLifecycleRoleController(List<String> roles, List<SmartLifecycle> lifecycles) {
		Assert.notNull(roles, "'roles' cannot be null");
		Assert.notNull(lifecycles, "'lifecycles' cannot be null");
		Assert.isTrue(roles.size() == lifecycles.size(), "'roles' and 'lifecycles' must be the same length");
		Iterator<SmartLifecycle> iterator = lifecycles.iterator();
		for (String role : roles) {
			SmartLifecycle lifecycle = iterator.next();
			addLifecycleToRole(role, lifecycle);
		}
	}

	/**
	 * Construct an instance with the provided map of roles/instances.
	 * @param lifecycles the {@link MultiValueMap} of beans in roles.
	 */
	public SmartLifecycleRoleController(MultiValueMap<String, SmartLifecycle> lifecycles) {
		lifecycles.forEach((role, values) -> values.forEach(lifecycle -> addLifecycleToRole(role, lifecycle)));
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * Add a {@link SmartLifecycle} to the role.
	 * @param role the role.
	 * @param lifecycle the {@link SmartLifecycle}.
	 */
	public final void addLifecycleToRole(String role, SmartLifecycle lifecycle) {
		List<SmartLifecycle> componentsInRole = this.lifecycles.get(role);
		if (CollectionUtils.isEmpty(componentsInRole)) {
			this.lifecycles.add(role, lifecycle);
		}
		else {
			componentsInRole
					.stream()
					.filter(e ->
							e == lifecycle ||
									(e instanceof NamedComponent && lifecycle instanceof NamedComponent
											&& ((NamedComponent) e).getComponentName()
											.equals(((NamedComponent) lifecycle).getComponentName())))
					.findFirst()
					.ifPresent(e -> {
						throw new IllegalArgumentException("Cannot add the Lifecycle '" +
								(lifecycle instanceof NamedComponent
										? ((NamedComponent) lifecycle).getComponentName()
										: lifecycle)
								+ "' to the role '" + role + "' because a Lifecycle with the name '"
								+ (e instanceof NamedComponent ? ((NamedComponent) e).getComponentName() : e)
								+ "' is already present.");
					});

			componentsInRole.add(lifecycle);
		}
	}

	/**
	 * Add a {@link SmartLifecycle} bean to the role using its name.
	 * @param role the role.
	 * @param lifecycleBeanName the bean name of the {@link SmartLifecycle}.
	 */
	public final void addLifecycleToRole(String role, String lifecycleBeanName) {
		Assert.state(this.applicationContext != null, "An application context is required to use this method");
		this.lazyLifecycles.add(role, lifecycleBeanName);
	}

	/**
	 * Add a {@link SmartLifecycle} beans to the role using their names.
	 * @param role the role.
	 * @param lifecycleBeanNames the bean names of the {@link SmartLifecycle}s.
	 */
	public void addLifecyclesToRole(String role, List<String> lifecycleBeanNames) {
		Assert.state(this.applicationContext != null, "An application context is required to use this method");
		lifecycleBeanNames.forEach(lifecycleBeanName -> this.lazyLifecycles.add(role, lifecycleBeanName));
	}

	/**
	 * Start all registered {@link SmartLifecycle}s in the role.
	 * @param role the role.
	 */
	public void startLifecyclesInRole(String role) {
		if (this.lazyLifecycles.size() > 0) {
			addLazyLifecycles();
		}
		List<SmartLifecycle> componentsInRole = this.lifecycles.get(role);
		if (componentsInRole != null) {
			componentsInRole = new ArrayList<>(componentsInRole);
			componentsInRole.sort(Comparator.comparingInt(Phased::getPhase));
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Starting " + componentsInRole + IN_ROLE + role);
			}

			componentsInRole.forEach(lifecycle -> {
				try {
					lifecycle.start();
				}
				catch (Exception e) {
					LOGGER.error("Failed to start " + lifecycle + IN_ROLE + role, e);
				}
			});
		}
		else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("No components in role " + role + ". Nothing to start");
			}
		}
	}

	/**
	 * Stop all registered {@link SmartLifecycle}s in the role.
	 * @param role the role.
	 */
	public void stopLifecyclesInRole(String role) {
		if (this.lazyLifecycles.size() > 0) {
			addLazyLifecycles();
		}
		List<SmartLifecycle> componentsInRole = this.lifecycles.get(role);
		if (componentsInRole != null) {
			componentsInRole = new ArrayList<>(componentsInRole);
			componentsInRole.sort((o1, o2) -> Integer.compare(o2.getPhase(), o1.getPhase()));
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stopping " + componentsInRole + IN_ROLE + role);
			}

			componentsInRole.forEach(lifecycle -> {
				try {
					lifecycle.stop();
				}
				catch (Exception e) {
					LOGGER.error("Failed to stop " + lifecycle + IN_ROLE + role, e);
				}
			});
		}
		else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("No components in role " + role + ". Nothing to stop");
			}
		}
	}

	/**
	 * Return a collection of the roles currently managed by this controller.
	 * @return the roles.
	 * @since 4.3.8
	 */
	public Collection<String> getRoles() {
		if (this.lazyLifecycles.size() > 0) {
			addLazyLifecycles();
		}
		return Collections.unmodifiableCollection(this.lifecycles.keySet());
	}

	/**
	 * Return true if all endpoints in the role are running.
	 * @param role the role.
	 * @return true if at least one endpoint in the role, and all are running.
	 * @since 4.3.8
	 */
	public boolean allEndpointsRunning(String role) {
		Map<String, Boolean> status = getEndpointsRunningStatus(role);
		return !status.isEmpty() && status.values().stream().allMatch(b -> b);
	}

	/**
	 * Return true if none of the endpoints in the role are running or if
	 * there are no endpoints in the role.
	 * @param role the role.
	 * @return true if there are no endpoints or none are running.
	 * @since 4.3.8
	 */
	public boolean noEndpointsRunning(String role) {
		Map<String, Boolean> status = getEndpointsRunningStatus(role);
		return status.isEmpty() || status.values().stream().noneMatch(b -> b);
	}

	/**
	 * Return the running status of each endpoint in the role.
	 * @param role the role.
	 * @return A map of component names : running status
	 * @since 4.3.8
	 */
	public Map<String, Boolean> getEndpointsRunningStatus(String role) {
		if (this.lazyLifecycles.size() > 0) {
			addLazyLifecycles();
		}

		if (!this.lifecycles.containsKey(role)) {
			return Collections.emptyMap();
		}

		AtomicInteger index = new AtomicInteger();
		return this.lifecycles.get(role)
				.stream()
				.collect(Collectors.toMap(e ->
								(e instanceof NamedComponent)
										? ((NamedComponent) e).getComponentName()
										: (e.getClass().getSimpleName() + "#" + index.getAndIncrement()),
						Lifecycle::isRunning));
	}

	private synchronized void addLazyLifecycles() {
		this.lazyLifecycles.forEach(this::doAddLifecyclesToRole);
		this.lazyLifecycles.clear();
	}

	private void doAddLifecyclesToRole(String role, List<String> lifecycleBeanNames) {
		lifecycleBeanNames.forEach(lifecycleBeanName -> {
			try {
				SmartLifecycle lifecycle = this.applicationContext.getBean(lifecycleBeanName, SmartLifecycle.class);
				addLifecycleToRole(role, lifecycle);
			}
			catch (NoSuchBeanDefinitionException e) {
				LOGGER.warn("Skipped; no such bean: " + lifecycleBeanName);
			}
		});
	}

	@Override
	public void onApplicationEvent(AbstractLeaderEvent event) {
		if (event instanceof OnGrantedEvent) {
			startLifecyclesInRole(event.getRole());
		}
		else if (event instanceof OnRevokedEvent) {
			stopLifecyclesInRole(event.getRole());
		}
	}

	/**
	 * Remove the provided SmartLifecycle from all the roles,
	 * for example when a SmartLifecycle bean is destroyed.
	 * The role entry in the lifecycles map is cleared as well
	 * if its value list is empty after SmartLifecycle removal.
	 * @param lifecycle the SmartLifecycle to remove.
	 * @return the removal status
	 * @since 5.0
	 */
	public boolean removeLifecycle(SmartLifecycle lifecycle) {
		boolean removed = false;

		for (List<SmartLifecycle> componentsInRole : this.lifecycles.values()) {
			removed = componentsInRole.removeIf(Predicate.isEqual(lifecycle));
			if (removed) {
				break;
			}
		}

		if (removed) {
			this.lifecycles.entrySet()
					.removeIf(entry -> entry.getValue().isEmpty());
		}

		return removed;
	}

}
