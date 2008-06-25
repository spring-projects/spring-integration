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
package org.springframework.integration.security.config;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextImpl;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

/**
 * 
 * @author Jonas Partner
 *
 */
public class SecurityTestUtil {
	
	public static SecurityContext createContext(String username, String password, String... roles){
		SecurityContextImpl ctxImpl = new SecurityContextImpl();
		UsernamePasswordAuthenticationToken authToken;
		if(roles != null && roles.length > 0){
			GrantedAuthority[] authorities = new GrantedAuthority[roles.length];
			for (int i =0; i < roles.length; i++) {
				authorities[i] = new GrantedAuthorityImpl(roles[i]);
			}
			authToken = new UsernamePasswordAuthenticationToken(username,password,authorities);
		} else {
			authToken = new UsernamePasswordAuthenticationToken(username, password);
		}
		ctxImpl.setAuthentication(authToken);
		return ctxImpl;
	}
}
