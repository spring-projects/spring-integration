/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.kafka.support;

import org.springframework.integration.kafka.core.ZookeeperConnectDefaults;

/**
 * @author Soby Chacko
 */
public class ZookeeperConnect {
    private String zkConnect = ZookeeperConnectDefaults.ZK_CONNECT;
    private String zkConnectionTimeout = ZookeeperConnectDefaults.ZK_CONNECTION_TIMEOUT;
    private String zkSessionTimeout = ZookeeperConnectDefaults.ZK_SESSION_TIMEOUT;
    private String zkSyncTime = ZookeeperConnectDefaults.ZK_SYNC_TIME;

    public String getZkConnect() {
        return zkConnect;
    }

    public void setZkConnect(final String zkConnect) {
        this.zkConnect = zkConnect;
    }

    public String getZkConnectionTimeout() {
        return zkConnectionTimeout;
    }

    public void setZkConnectionTimeout(final String zkConnectionTimeout) {
        this.zkConnectionTimeout = zkConnectionTimeout;
    }

    public String getZkSessionTimeout() {
        return zkSessionTimeout;
    }

    public void setZkSessionTimeout(final String zkSessionTimeout) {
        this.zkSessionTimeout = zkSessionTimeout;
    }

    public String getZkSyncTime() {
        return zkSyncTime;
    }

    public void setZkSyncTime(final String zkSyncTime) {
        this.zkSyncTime = zkSyncTime;
    }
}
