/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.jpa.core;

/**
 * An Exception that would be thrown if any of the Operations from {@link JpaOperations} fails.
 *
 * @author Amol Nayak
 * @author Gary Russell
 *
 * @since 2.2
 *
 */
public class JpaOperationFailedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final String offendingJPAQl;

	public JpaOperationFailedException(String message, String offendingJPAQ1) {
		super(message);
		this.offendingJPAQl = offendingJPAQ1;
	}

	public String getOffendingJPAQl() {
		return this.offendingJPAQl;
	}

}
