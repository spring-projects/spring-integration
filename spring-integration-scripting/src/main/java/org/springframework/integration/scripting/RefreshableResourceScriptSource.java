/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.scripting;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.core.io.Resource;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * A {@link ScriptSource} implementation, which caches a script string and refreshes it from the
 * target file (if modified) according the provided {@link #refreshDelay}.
 *
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class RefreshableResourceScriptSource implements ScriptSource {

	private final long refreshDelay;

	private final ResourceScriptSource source;

	private final AtomicLong lastModifiedChecked = new AtomicLong(System.currentTimeMillis());

	private volatile String script;

	public RefreshableResourceScriptSource(Resource resource, long refreshDelay) {
		this.refreshDelay = refreshDelay;
		this.source = new ResourceScriptSource(resource);
		try {
			this.script = this.source.getScriptAsString();
		}
		catch (IOException e) {
			this.lastModifiedChecked.set(0);
		}
	}

	public String getScriptAsString() throws IOException {
		if (this.script == null || this.isModified()) {
			this.lastModifiedChecked.set(System.currentTimeMillis());
			this.script = this.source.getScriptAsString();
		}
		return this.script;
	}

	public String suggestedClassName() {
		return this.source.getResource().getFilename();
	}

	public boolean isModified() {
		return this.refreshDelay >= 0 &&
				(System.currentTimeMillis() - this.lastModifiedChecked.get()) > this.refreshDelay &&
				this.source.isModified();
	}

	@Override
	public String toString() {
		return this.source.toString();
	}

}
