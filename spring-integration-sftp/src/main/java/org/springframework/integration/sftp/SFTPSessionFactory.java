/*
 * Copyright 2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.sftp;

import org.apache.commons.lang.StringUtils;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;


/**
 * Factories {@link org.springframework.integration.sftp.SFTPSession} instances. There are lots of ways to construct a
 * {@link org.springframework.integration.sftp.SFTPSession} instance, and not all of them are obvious. This factory
 * does its best to make it work.
 *
 * @author Josh Long
 * @author Mario Gray
 */
public class SFTPSessionFactory implements FactoryBean<SFTPSession>, InitializingBean {
    private volatile String knownHosts;
    private volatile String password;
    private volatile String privateKey;
    private volatile String privateKeyPassphrase;
    private volatile String remoteHost;
    private volatile String user;
    private volatile int port = 22; // the default

    public void afterPropertiesSet() throws Exception {
        assert !StringUtils.isEmpty(this.remoteHost) : "remoteHost can't be empty!";
        assert !StringUtils.isEmpty(this.user) : "user can't be empty!";
        assert !StringUtils.isEmpty(this.password) || !StringUtils.isEmpty(this.privateKey) || !StringUtils.isEmpty(this.privateKeyPassphrase) : "you must configure either a password or a private key and/or a private key passphrase!";
        assert this.port >= 0 : "port must be a valid number! ";
    }

    public String getKnownHosts() {
        return knownHosts;
    }

    public SFTPSession getObject() throws Exception {
        return new SFTPSession(this.getUser(), this.getRemoteHost(), this.getPassword(), this.getPort(), this.getKnownHosts(), null, this.getPrivateKey(), this.getPrivateKeyPassphrase());
    }

    public Class<?extends SFTPSession> getObjectType() {
        return SFTPSession.class;
    }

    public String getPassword() {
        return password;
    }

    public int getPort() {
        return port;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPrivateKeyPassphrase() {
        return privateKeyPassphrase;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public String getUser() {
        return user;
    }

    public boolean isSingleton() {
        return false;
    }

    public void setKnownHosts(String knownHosts) {
        this.knownHosts = knownHosts;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
        this.privateKeyPassphrase = privateKeyPassphrase;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
