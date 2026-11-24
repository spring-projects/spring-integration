/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.config.annotation;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;

/**
 * @author Mark Fisher
 */
@MessageEndpoint
public class TypeConvertingTestEndpoint {

	@ServiceActivator(inputChannel = "inputChannel", outputChannel = "outputChannel")
	public int multiplyByTwo(int number) {
		return number * 2;
	}

}
