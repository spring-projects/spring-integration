/*
 * Copyright 2002-2014 the original author or authors.
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
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;

import com.rabbitmq.client.AMQP.Basic.RecoverOk;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Confirm.SelectOk;
import com.rabbitmq.client.AMQP.Exchange.BindOk;
import com.rabbitmq.client.AMQP.Exchange.DeclareOk;
import com.rabbitmq.client.AMQP.Exchange.DeleteOk;
import com.rabbitmq.client.AMQP.Exchange.UnbindOk;
import com.rabbitmq.client.AMQP.Queue.PurgeOk;
import com.rabbitmq.client.AMQP.Tx.CommitOk;
import com.rabbitmq.client.AMQP.Tx.RollbackOk;
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

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.1
 */
public class StubRabbitConnectionFactory implements ConnectionFactory {

	@Override
	public Connection createConnection() throws AmqpException {
		return new StubConnection();
	}

	@Override
	public String getHost() {
		return null;
	}

	@Override
	public int getPort() {
		return 0;
	}

	@Override
	public String getVirtualHost() {
		return null;
	}

	@Override
	public void addConnectionListener(ConnectionListener listener) {
	}

	@Override
	public boolean removeConnectionListener(ConnectionListener listener) {
		return false;
	}

	@Override
	public void clearConnectionListeners() {
	}


	private static class StubConnection implements Connection {

		@Override
		public Channel createChannel(boolean transactional) throws AmqpException {
			return new StubChannel();
		}

		@Override
		public void close() throws AmqpException {
		}

		@Override
		public boolean isOpen() {
			return false;
		}
	}

	private static class StubChannel implements Channel {

		@Override
		public void addShutdownListener(ShutdownListener listener) {
		}

		@Override
		public void removeShutdownListener(ShutdownListener listener) {
		}

		@Override
		public ShutdownSignalException getCloseReason() {
			return null;
		}

		@Override
		public void notifyListeners() {
		}

		@Override
		public boolean isOpen() {
			return false;
		}

		@Override
		public int getChannelNumber() {
			return 0;
		}

		@Override
		public com.rabbitmq.client.Connection getConnection() {
			return null;
		}

		@Override
		public void close() throws IOException {
		}

		@Override
		public void close(int closeCode, String closeMessage) throws IOException {
		}

		@Override
		public boolean flowBlocked() {
			return false;
		}

		@Override
		public void abort() throws IOException {
		}

		@Override
		public void abort(int closeCode, String closeMessage) throws IOException {
		}

		@SuppressWarnings("unused")
		public ReturnListener getReturnListener() {
			return null;
		}

		@Override
		public void addReturnListener(ReturnListener listener) {
		}

		@SuppressWarnings("unused")
		public FlowListener getFlowListener() {
			return null;
		}

		@SuppressWarnings("unused")
		public void setFlowListener(FlowListener listener) {
		}

		@SuppressWarnings("unused")
		public ConfirmListener getConfirmListener() {
			return null;
		}

		@SuppressWarnings("unused")
		public void setConfirmListener(ConfirmListener listener) {
		}

		@Override
		public Consumer getDefaultConsumer() {
			return null;
		}

		@Override
		public void setDefaultConsumer(Consumer consumer) {
		}

		@Override
		public void basicQos(int prefetchSize, int prefetchCount, boolean global) throws IOException {
		}

		@Override
		public void basicQos(int prefetchCount, boolean global) throws IOException {
		}

		@Override
		public void basicQos(int prefetchCount) throws IOException {
		}

		@Override
		public void basicPublish(String exchange, String routingKey,
				BasicProperties props, byte[] body) throws IOException {
		}

		@Override
		public void basicPublish(String exchange, String routingKey,
				boolean mandatory, boolean immediate, BasicProperties props,
				byte[] body) throws IOException {
		}

		@Override
		public DeclareOk exchangeDeclare(String exchange, String type) throws IOException {
			return null;
		}

		@Override
		public DeclareOk exchangeDeclare(String exchange, String type,
				boolean durable) throws IOException {
			return null;
		}

		@Override
		public DeclareOk exchangeDeclare(String exchange, String type,
				boolean durable, boolean autoDelete,
				Map<String, Object> arguments) throws IOException {
			return null;
		}

		@Override
		public DeclareOk exchangeDeclare(String exchange, String type,
				boolean durable, boolean autoDelete, boolean internal,
				Map<String, Object> arguments) throws IOException {
			return null;
		}

		@Override
		public DeclareOk exchangeDeclarePassive(String name) throws IOException {
			return null;
		}

		@Override
		public DeleteOk exchangeDelete(String exchange, boolean ifUnused) throws IOException {
			return null;
		}

		@Override
		public DeleteOk exchangeDelete(String exchange) throws IOException {
			return null;
		}

		@Override
		public BindOk exchangeBind(String destination, String source,
				String routingKey) throws IOException {
			return null;
		}

		@Override
		public BindOk exchangeBind(String destination, String source,
				String routingKey, Map<String, Object> arguments)
				throws IOException {
			return null;
		}

		@Override
		public UnbindOk exchangeUnbind(String destination, String source,
				String routingKey) throws IOException {
			return null;
		}

		@Override
		public UnbindOk exchangeUnbind(String destination, String source,
				String routingKey, Map<String, Object> arguments)
				throws IOException {
			return null;
		}

		@Override
		public com.rabbitmq.client.AMQP.Queue.DeclareOk queueDeclare() throws IOException {
			return null;
		}

		@Override
		public com.rabbitmq.client.AMQP.Queue.DeclareOk queueDeclare(
				final String queue, boolean durable, boolean exclusive,
				boolean autoDelete, Map<String, Object> arguments)
				throws IOException {
			return new com.rabbitmq.client.AMQP.Queue.DeclareOk() {

				@Override
				public int protocolClassId() {
					return 0;
				}

				@Override
				public int protocolMethodId() {
					return 0;
				}

				@Override
				public String protocolMethodName() {
					return null;
				}

				@Override
				public int getConsumerCount() {
					return 0;
				}

				@Override
				public int getMessageCount() {
					return 0;
				}

				@Override
				public String getQueue() {
					return queue;
				}};
		}

		@Override
		public com.rabbitmq.client.AMQP.Queue.DeclareOk queueDeclarePassive(
				String queue) throws IOException {
			return null;
		}

		@Override
		public com.rabbitmq.client.AMQP.Queue.DeleteOk queueDelete(String queue) throws IOException {
			return null;
		}

		@Override
		public com.rabbitmq.client.AMQP.Queue.DeleteOk queueDelete(
				String queue, boolean ifUnused, boolean ifEmpty)
				throws IOException {
			return null;
		}

		@Override
		public com.rabbitmq.client.AMQP.Queue.BindOk queueBind(String queue,
				String exchange, String routingKey) throws IOException {
			return null;
		}

		@Override
		public com.rabbitmq.client.AMQP.Queue.BindOk queueBind(String queue,
				String exchange, String routingKey,
				Map<String, Object> arguments) throws IOException {
			return null;
		}

		@Override
		public com.rabbitmq.client.AMQP.Queue.UnbindOk queueUnbind(
				String queue, String exchange, String routingKey)
				throws IOException {
			return null;
		}

		@Override
		public com.rabbitmq.client.AMQP.Queue.UnbindOk queueUnbind(
				String queue, String exchange, String routingKey,
				Map<String, Object> arguments) throws IOException {
			return null;
		}

		@Override
		public PurgeOk queuePurge(String queue) throws IOException {
			return null;
		}

		@Override
		public GetResponse basicGet(String queue, boolean autoAck) throws IOException {
			return null;
		}

		@Override
		public void basicAck(long deliveryTag, boolean multiple) throws IOException {
		}

		@Override
		public void basicNack(long deliveryTag, boolean multiple, boolean requeue) throws IOException {
		}

		@Override
		public void basicReject(long deliveryTag, boolean requeue) throws IOException {
		}

		@Override
		public String basicConsume(String queue, Consumer callback) throws IOException {
			return null;
		}

		@Override
		public String basicConsume(String queue, boolean autoAck, Consumer callback) throws IOException {
			return null;
		}

		@Override
		public String basicConsume(String queue, boolean autoAck,
				String consumerTag, Consumer callback) throws IOException {
			return null;
		}

		@Override
		public String basicConsume(String queue, boolean autoAck, Map<String, Object> arguments, Consumer callback)
				throws IOException {
			return null;
		}

		@Override
		public String basicConsume(String queue, boolean autoAck,
				String consumerTag, boolean noLocal, boolean exclusive,
				Map<String, Object> arguments, Consumer callback)
				throws IOException {
			return null;
		}

		@Override
		public void basicCancel(String consumerTag) throws IOException {
		}

		@Override
		public RecoverOk basicRecover() throws IOException {
			return null;
		}

		@Override
		public RecoverOk basicRecover(boolean requeue) throws IOException {
			return null;
		}

		@Override
		@Deprecated
		public void basicRecoverAsync(boolean requeue) throws IOException {
		}

		@Override
		public com.rabbitmq.client.AMQP.Tx.SelectOk txSelect() throws IOException {
			return null;
		}

		@Override
		public CommitOk txCommit() throws IOException {
			return null;
		}

		@Override
		public RollbackOk txRollback() throws IOException {
			return null;
		}

		@Override
		public SelectOk confirmSelect() throws IOException {
			return null;
		}

		@Override
		public long getNextPublishSeqNo() {
			return 0;
		}

		@Override
		public void asyncRpc(Method method) throws IOException {
		}

		@Override
		public Command rpc(Method method) throws IOException {
			return null;
		}

		@Override
		public boolean removeReturnListener(ReturnListener listener) {
			return false;
		}

		@Override
		public void clearReturnListeners() {
		}

		@Override
		public void addFlowListener(FlowListener listener) {
		}

		@Override
		public boolean removeFlowListener(FlowListener listener) {
			return false;
		}

		@Override
		public void clearFlowListeners() {
		}

		@Override
		public void addConfirmListener(ConfirmListener listener) {
		}

		@Override
		public boolean removeConfirmListener(ConfirmListener listener) {
			return false;
		}

		@Override
		public void clearConfirmListeners() {
		}

		@Override
		public boolean waitForConfirms() throws InterruptedException {
			return false;
		}

		@Override
		public void waitForConfirmsOrDie() throws IOException,
				InterruptedException {
		}

		@Override
		public boolean waitForConfirms(long timeout)
				throws InterruptedException, TimeoutException {
			return false;
		}

		@Override
		public void waitForConfirmsOrDie(long timeout) throws IOException,
				InterruptedException, TimeoutException {
		}

		@Override
		public void basicPublish(String arg0, String arg1, boolean arg2, BasicProperties arg3, byte[] arg4)
				throws IOException {
		}
	}

}
