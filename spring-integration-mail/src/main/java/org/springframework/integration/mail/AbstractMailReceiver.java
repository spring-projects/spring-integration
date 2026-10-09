/*
 * Copyright 2002-present the original author or authors.
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
