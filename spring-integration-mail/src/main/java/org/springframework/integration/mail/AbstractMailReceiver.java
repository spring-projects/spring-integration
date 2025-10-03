/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.mail;

import jakarta.mail.URLName;
import org.jspecify.annotations.Nullable;

/**
 * Base class for {@link org.springframework.integration.mail.inbound.MailReceiver} implementations.
 *
 * @author Arjen Poutsma
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Dominik Simmen
 * @author Yuxin Wang
 * @author Ngoc Nhan
 * @author Filip Hrisafov
 * @author Jiandong Ma
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.mail.inbound.AbstractMailReceiver}
 */
@Deprecated(forRemoval = true, since = "7.0")
public abstract class AbstractMailReceiver extends org.springframework.integration.mail.inbound.AbstractMailReceiver {

	public AbstractMailReceiver() {
	}

	public AbstractMailReceiver(URLName urlName) {
		super(urlName);
	}

	public AbstractMailReceiver(@Nullable String url) {
		super(url);
	}

}
