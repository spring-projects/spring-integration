/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.integration.support.management;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import reactor.core.support.Assert;

/**
 * Configures beans that implement {@link IntegrationManagement}.
 *
 * TODO: This class will be expanded by INT-3755/3756.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public class IntegrationManagementConfigurer implements SmartInitializingSingleton, ApplicationContextAware {

	private ApplicationContext applicationContext;

	private boolean defaultLoggingEnabled = true;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * Disable all logging in the main message flow in framework components. When 'false', such logging will be
	 * skipped, regardless of logging level. When 'true', the logging is controlled as normal by the logging
	 * subsystem log level configuration.
	 * <p>
	 * It has been found that in high-volume messaging environments, calls to methods such as
	 * {@code logger.isDebuggingEnabled()} can be quite expensive and account for an inordinate amount of CPU
	 * time.
	 * <p>
	 * Set this to false to disable logging by default in all framework components that implement
	 * {@link IntegrationManagement} (channels, message handlers etc). This turns off logging such as
	 * "PreSend on channel", "Received message" etc.
	 * <p>
	 * After the context is initialized, individual components can have their setting changed by invoking
	 * {@link IntegrationManagement#setLoggingEnabled(boolean)}.
	 * @param defaultLoggingEnabled defaults to true.
	 */
	public void setDefaultLoggingEnabled(boolean defaultLoggingEnabled) {
		this.defaultLoggingEnabled = defaultLoggingEnabled;
	}

	@Override
	public void afterSingletonsInstantiated() {
		Assert.state(this.applicationContext != null, "'applicationContext' must not be null");
		Map<String, IntegrationManagement> managed = this.applicationContext.getBeansOfType(IntegrationManagement.class);
		for (IntegrationManagement bean : managed.values()) {
			bean.setLoggingEnabled(this.defaultLoggingEnabled);
		}
	}

}
