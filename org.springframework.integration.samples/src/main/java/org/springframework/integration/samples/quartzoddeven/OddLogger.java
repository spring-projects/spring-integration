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

package org.springframework.integration.samples.quartzoddeven;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.integration.annotation.Handler;
import org.springframework.integration.annotation.MessageEndpoint;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
@MessageEndpoint
public class OddLogger {

	@Handler
	public void log(int i) {
		System.out.println("odd:  " + i  + " at " + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
	}

}
