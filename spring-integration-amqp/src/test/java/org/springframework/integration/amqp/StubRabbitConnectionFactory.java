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

package org.springframework.integration.amqp;

import java.io.IOException;
import java.util.Map;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Command;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.FlowListener;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.Method;
import com.rabbitmq.client.ReturnListener;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Basic.RecoverOk;
import com.rabbitmq.client.AMQP.Channel.FlowOk;
import com.rabbitmq.client.AMQP.Confirm.SelectOk;
import com.rabbitmq.client.AMQP.Exchange.BindOk;
import com.rabbitmq.client.AMQP.Exchange.DeclareOk;
import com.rabbitmq.client.AMQP.Exchange.DeleteOk;
import com.rabbitmq.client.AMQP.Exchange.UnbindOk;
import com.rabbitmq.client.AMQP.Queue.PurgeOk;
import com.rabbitmq.client.AMQP.Tx.CommitOk;
import com.rabbitmq.client.AMQP.Tx.RollbackOk;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.1
 */
public class StubRabbitConnectionFactory implements ConnectionFactory {

	public Connection createConnection() throws AmqpException {
		return new StubConnection();
	}

	public String getHost() {
		return null;
	}

	public int getPort() {
		return 0;
	}

	public String getVirtualHost() {
		return null;
	}

	public void addConnectionListener(ConnectionListener listener) {
	}


	private static class StubConnection implements Connection {

		public Channel createChannel(boolean transactional) throws AmqpException {
			return new StubChannel();
		}

		public void close() throws AmqpException {
		}

		public boolean isOpen() {
			return false;
		}
	}
	
	private static class StubChannel implements Channel {

		public void addShutdownListener(ShutdownListener listener) {
		}

		public void removeShutdownListener(ShutdownListener listener) {
		}

		public ShutdownSignalException getCloseReason() {
			return null;
		}

		public void notifyListeners() {
		}

		public boolean isOpen() {
			return false;
		}

		public int getChannelNumber() {
			return 0;
		}

		public com.rabbitmq.client.Connection getConnection() {
			return null;
		}

		public void close() throws IOException {
		}

		public void close(int closeCode, String closeMessage) throws IOException {
		}

		public FlowOk flow(boolean active) throws IOException {
			return null;
		}

		public FlowOk getFlow() {
			return null;
		}

		public void abort() throws IOException {
		}

		public void abort(int closeCode, String closeMessage) throws IOException {
		}

		public ReturnListener getReturnListener() {
			return null;
		}

		public void addReturnListener(ReturnListener listener) {
		}

		public FlowListener getFlowListener() {
			return null;
		}

		public void setFlowListener(FlowListener listener) {
		}

		public ConfirmListener getConfirmListener() {
			return null;
		}

		public void setConfirmListener(ConfirmListener listener) {
		}

		public Consumer getDefaultConsumer() {
			return null;
		}

		public void setDefaultConsumer(Consumer consumer) {
		}

		public void basicQos(int prefetchSize, int prefetchCount, boolean global) throws IOException {
		}

		public void basicQos(int prefetchCount) throws IOException {
		}

		public void basicPublish(String exchange, String routingKey,
				BasicProperties props, byte[] body) throws IOException {
		}

		public void basicPublish(String exchange, String routingKey,
				boolean mandatory, boolean immediate, BasicProperties props,
				byte[] body) throws IOException {
		}

		public DeclareOk exchangeDeclare(String exchange, String type) throws IOException {
			return null;
		}

		public DeclareOk exchangeDeclare(String exchange, String type,
				boolean durable) throws IOException {
			return null;
		}

		public DeclareOk exchangeDeclare(String exchange, String type,
				boolean durable, boolean autoDelete,
				Map<String, Object> arguments) throws IOException {
			return null;
		}

		public DeclareOk exchangeDeclare(String exchange, String type,
				boolean durable, boolean autoDelete, boolean internal,
				Map<String, Object> arguments) throws IOException {
			return null;
		}

		public DeclareOk exchangeDeclarePassive(String name) throws IOException {
			return null;
		}

		public DeleteOk exchangeDelete(String exchange, boolean ifUnused) throws IOException {
			return null;
		}

		public DeleteOk exchangeDelete(String exchange) throws IOException {
			return null;
		}

		public BindOk exchangeBind(String destination, String source,
				String routingKey) throws IOException {
			return null;
		}

		public BindOk exchangeBind(String destination, String source,
				String routingKey, Map<String, Object> arguments)
				throws IOException {
			return null;
		}

		public UnbindOk exchangeUnbind(String destination, String source,
				String routingKey) throws IOException {
			return null;
		}

		public UnbindOk exchangeUnbind(String destination, String source,
				String routingKey, Map<String, Object> arguments)
				throws IOException {
			return null;
		}

		public com.rabbitmq.client.AMQP.Queue.DeclareOk queueDeclare() throws IOException {
			return null;
		}

		public com.rabbitmq.client.AMQP.Queue.DeclareOk queueDeclare(
				String queue, boolean durable, boolean exclusive,
				boolean autoDelete, Map<String, Object> arguments)
				throws IOException {
			return null;
		}

		public com.rabbitmq.client.AMQP.Queue.DeclareOk queueDeclarePassive(
				String queue) throws IOException {
			return null;
		}

		public com.rabbitmq.client.AMQP.Queue.DeleteOk queueDelete(String queue) throws IOException {
			return null;
		}

		public com.rabbitmq.client.AMQP.Queue.DeleteOk queueDelete(
				String queue, boolean ifUnused, boolean ifEmpty)
				throws IOException {
			return null;
		}

		public com.rabbitmq.client.AMQP.Queue.BindOk queueBind(String queue,
				String exchange, String routingKey) throws IOException {
			return null;
		}

		public com.rabbitmq.client.AMQP.Queue.BindOk queueBind(String queue,
				String exchange, String routingKey,
				Map<String, Object> arguments) throws IOException {
			return null;
		}

		public com.rabbitmq.client.AMQP.Queue.UnbindOk queueUnbind(
				String queue, String exchange, String routingKey)
				throws IOException {
			return null;
		}

		public com.rabbitmq.client.AMQP.Queue.UnbindOk queueUnbind(
				String queue, String exchange, String routingKey,
				Map<String, Object> arguments) throws IOException {
			return null;
		}

		public PurgeOk queuePurge(String queue) throws IOException {
			return null;
		}

		public GetResponse basicGet(String queue, boolean autoAck) throws IOException {
			return null;
		}

		public void basicAck(long deliveryTag, boolean multiple) throws IOException {
		}

		public void basicNack(long deliveryTag, boolean multiple, boolean requeue) throws IOException {
		}

		public void basicReject(long deliveryTag, boolean requeue) throws IOException {
		}

		public String basicConsume(String queue, Consumer callback) throws IOException {
			return null;
		}

		public String basicConsume(String queue, boolean autoAck, Consumer callback) throws IOException {
			return null;
		}

		public String basicConsume(String queue, boolean autoAck,
				String consumerTag, Consumer callback) throws IOException {
			return null;
		}

		public String basicConsume(String queue, boolean autoAck,
				String consumerTag, boolean noLocal, boolean exclusive,
				Map<String, Object> arguments, Consumer callback)
				throws IOException {
			return null;
		}

		public void basicCancel(String consumerTag) throws IOException {
		}

		public RecoverOk basicRecover() throws IOException {
			return null;
		}

		public RecoverOk basicRecover(boolean requeue) throws IOException {
			return null;
		}
		
		@Deprecated
		public void basicRecoverAsync(boolean requeue) throws IOException {
		}

		public com.rabbitmq.client.AMQP.Tx.SelectOk txSelect() throws IOException {
			return null;
		}

		public CommitOk txCommit() throws IOException {
			return null;
		}

		public RollbackOk txRollback() throws IOException {
			return null;
		}

		public SelectOk confirmSelect() throws IOException {
			return null;
		}

		public long getNextPublishSeqNo() {
			return 0;
		}

		public void asyncRpc(Method method) throws IOException {
		}

		public Command rpc(Method method) throws IOException {
			return null;
		}

		public boolean removeReturnListener(ReturnListener listener) {
			return false;
		}

		public void clearReturnListeners() {
		}

		public void addFlowListener(FlowListener listener) {
		}

		public boolean removeFlowListener(FlowListener listener) {
			return false;
		}

		public void clearFlowListeners() {
		}

		public void addConfirmListener(ConfirmListener listener) {
		}

		public boolean removeConfirmListener(ConfirmListener listener) {
			return false;
		}

		public void clearConfirmListeners() {
		}

		public boolean waitForConfirms() throws InterruptedException {
			return false;
		}

		public void waitForConfirmsOrDie() throws IOException,
				InterruptedException {
		}
	}

}
