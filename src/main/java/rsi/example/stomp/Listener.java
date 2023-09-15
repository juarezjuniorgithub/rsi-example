/*
  Copyright (c) 2022, 2023, Oracle and/or its affiliates.
  This software is dual-licensed to you under the Universal Permissive License
  (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
  2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
  either license.
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
     https://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
package rsi.example.stomp;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.fusesource.stomp.jms.message.StompJmsBytesMessage;

import oracle.rsi.PushPublisher;
import oracle.rsi.ReactiveStreamsIngestion;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;
import rsi.example.common.MqConfig;
import rsi.example.common.RSIService;
import rsi.example.common.Retailer;

/**
 * A listener class that listens to inputs from the topic in ActiveMQ using
 * STOMP protocol. RSI service starts at the time when the listener is up. Once
 * the data is received, RSI streams the records into the database.
 */
public class Listener {

	private static final OracleJsonFactory JSON_FACTORY = new OracleJsonFactory();
	private static final RSIService RSI_SERVICE = new RSIService();

	public static void main(String[] args) throws Exception {

		// Setup ActiveMQ connection and consumer
		StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
		factory.setBrokerURI(MqConfig.TCP_URL_SCHEME + MqConfig.ACTIVEMQ_HOST + ":" + MqConfig.ACTIVEMQ_STOMP_PORT);

		Connection connection = factory.createConnection();
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Destination dest = new StompJmsDestination(MqConfig.ACTIVEMQ_TOPIC_URI + MqConfig.ACTIVEMQ_TOPIC_NAME);

		MessageConsumer consumer = session.createConsumer(dest);

		long start = System.currentTimeMillis();
		long count = 1;

		// Start up RSI
		ReactiveStreamsIngestion rsi = RSI_SERVICE.start();
		PushPublisher<Retailer> pushPublisher = ReactiveStreamsIngestion.pushPublisher();
		pushPublisher.subscribe(rsi.subscriber());

		System.out.println("Waiting for messages...");
		while (true) {
			Message msg = consumer.receive();

			if (msg instanceof TextMessage || msg instanceof StompJmsBytesMessage) {
				String body = getBody(msg);
				if (body.trim().equals("SHUTDOWN")) {
					long diff = System.currentTimeMillis() - start;
					System.out.println(String.format("Received %d in %.2f seconds", count, (1.0 * diff / 1000.0)));

					// close RSI and worker threads
					pushPublisher.close();
					RSI_SERVICE.stop();
					break;

				} else {
					// Create OracleJsonObject from the incoming message
					OracleJsonObject jsonObject = JSON_FACTORY
							.createJsonTextValue(new ByteArrayInputStream(body.getBytes())).asJsonObject();

					// Push the data
					pushPublisher.accept(new Retailer(jsonObject));

					if (count == 0) {
						start = System.currentTimeMillis();
					}
//					if (count % 1000 == 0) {
//						System.out.println(String.format("Received %d messages.", count));
//					}
					count++;
				}

			} else {
				System.out.println("Unexpected message type: " + msg.getClass());
			}
		}
		connection.close();
	}

	private static String getBody(Message msg) throws JMSException {
		if (msg instanceof TextMessage) {
			return ((TextMessage) msg).getText();

		} else if (msg instanceof StompJmsBytesMessage) {
			StompJmsBytesMessage stompMsg = (StompJmsBytesMessage) msg;

			byte bytesArray[] = new byte[(int) stompMsg.getBodyLength()];
			stompMsg.readBytes(bytesArray);
			return new String(bytesArray, StandardCharsets.UTF_8);

		} else {
			throw new IllegalArgumentException("Unexpected message type: " + msg.getClass());
		}
	}
}
