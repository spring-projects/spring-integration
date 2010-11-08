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
package org.springframework.integration.twitter.core;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 *
 */
@SuppressWarnings("serial")
public class TwitterOperationException extends RuntimeException {

	/**
	 * 
	 */
	public TwitterOperationException() {
		super();
	}

	/**
	 * @param description
	 */
	public TwitterOperationException(String description) {
		super(description);
	}

	/**
	 * @param throwable
	 */
	public TwitterOperationException(Throwable throwable) {
		super(throwable);
	}

	/**
	 * @param description
	 * @param throwable
	 */
	public TwitterOperationException(String description, Throwable throwable) {
		super(description, throwable);
	}

}
