/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config;

import java.util.concurrent.TimeUnit;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;

/**
 * @author Mark Fisher
 */
@MessageEndpoint
public class PollerAnnotationConsumerAdviceChainTestBean {

	@ServiceActivator(inputChannel="input", outputChannel="output")
	@Poller(interval=5, timeUnit=TimeUnit.SECONDS, maxMessagesPerPoll=1,
			adviceChain="beforeAdvice  ,aroundAdvice, afterAdvice ") // spacing intentional
	public String testMethod(String input) {
		return input.toUpperCase();
	}

}
