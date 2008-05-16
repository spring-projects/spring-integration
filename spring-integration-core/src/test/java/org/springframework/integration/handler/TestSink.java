/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.handler;

import java.util.concurrent.CountDownLatch;

/**
 * @author Mark Fisher
 */
public class TestSink {

	private String result;

	private CountDownLatch latch;


	public void setLatch(CountDownLatch latch) {
		this.latch = latch;
	}

	public void validMethod(String s) {
	}

	public void invalidMethodWithNoArgs() {
	}

	public String methodWithReturnValue(String s) {
		return "value";
	}

	public void store(String s) {
		this.result = s;
		if (this.latch != null) {
			this.latch.countDown();
		}
	}

	public String get() {
		return this.result;
	}

}
