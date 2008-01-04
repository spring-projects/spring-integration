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

package org.springframework.integration.adapter;

/**
 * @author Mark Fisher
 */
public class TestSource {

	private boolean fooCalled;

	public String validMethod() {
		return "valid";
	}

	public String invalidMethodWithArg(String arg) {
		return "invalid";
	}

	public void invalidMethodWithNoReturnValue() {
	}

	public String foo() {
		if (this.fooCalled) {
			try {
				Thread.sleep(5000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		this.fooCalled = true;
		return "bar";
	}

}
