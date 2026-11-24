/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.jms;

import java.io.Serial;

import org.springframework.jms.JmsException;

/**
 * A timeout occurred within an async gateway.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class JmsTimeoutException extends JmsException {

	@Serial
	private static final long serialVersionUID = 5439915454935047936L;

	public JmsTimeoutException(String description) {
		super(description);
	}

}
