/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.file.remote.session;

import org.springframework.beans.factory.FactoryBean;

/**
 * Temporary factory bean to manage SessionFactory until deprecated 'cache-sessions' attribute
 * is removed.
 * 
 * @author Oleg Zhurakousky
 * @since 2.1
 *
 */
public class SessionFactoryFactoryBean<T> implements FactoryBean<SessionFactory<T>> {
	
	private final SessionFactory<T> sessionFactory;
	
	public SessionFactoryFactoryBean(SessionFactory<T> sessionFactory, boolean cacheSessions){
		if (cacheSessions && !(sessionFactory instanceof CachingSessionFactory)){
			this.sessionFactory = new CachingSessionFactory<T>(sessionFactory);
		}
		else {
			this.sessionFactory = sessionFactory;
		}
	}

	public SessionFactory<T> getObject() throws Exception {
		return this.sessionFactory;
	}

	
	public Class<?> getObjectType() {
		return this.sessionFactory.getClass();
	}

	public boolean isSingleton() {
		return true;
	}

}
