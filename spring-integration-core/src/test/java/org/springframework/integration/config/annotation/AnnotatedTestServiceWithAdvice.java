/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.config.annotation;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.endpoint.annotation.TestService;

/**
 * @author Gary Russell
 */
@MessageEndpoint
public class AnnotatedTestServiceWithAdvice implements TestService {

	@ServiceActivator(inputChannel = "inputChannel", outputChannel = "outputChannel")
	public String sayHello(String name) {
		return "hello " + name;
	}

	@ServiceActivator(inputChannel = "advisedIn", outputChannel = "advisedOut", adviceChain = "advice")
	public String sayHelloWithAdvice(String name) {
		return "hello " + name;
	}

}
