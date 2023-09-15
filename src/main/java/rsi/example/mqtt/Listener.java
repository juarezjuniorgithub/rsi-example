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
package rsi.example.mqtt;

import java.io.ByteArrayInputStream;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import oracle.rsi.PushPublisher;
import oracle.rsi.ReactiveStreamsIngestion;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;

import rsi.example.common.RSIService;
import rsi.example.common.Retailer;
import rsi.example.common.MqConfig;

/**
 * A listener class that listens to inputs from the topic in ActiveMQ using MQTT
 * protocol. RSI service starts at the time when the listener is up. Once the
 * data is received, RSI streams the records into the database.
 */
public class Listener {

	private static final OracleJsonFactory JSON_FACTORY = new OracleJsonFactory();
	private static final RSIService RSI_SERVICE = new RSIService();
	
	public static void main(String[] args) throws Exception {
		
		// Setup ActiveMQ connection and consumer
		MQTT mqtt = new MQTT();
		mqtt.setHost(MqConfig.ACTIVEMQ_HOST, MqConfig.ACTIVEMQ_MQTT_PORT);
		mqtt.setUserName(MqConfig.ACTIVEMQ_USER);
		mqtt.setPassword(MqConfig.ACTIVEMQ_PASSWORD);

		final CallbackConnection connection = mqtt.callbackConnection();
		connection.listener(new org.fusesource.mqtt.client.Listener() {
			long count = 0;
			long start = System.currentTimeMillis();

			PushPublisher<Retailer> pushPublisher;

			public void onConnected() {
				// Start up RSI
				ReactiveStreamsIngestion rsi = RSI_SERVICE.start();
				pushPublisher = ReactiveStreamsIngestion.pushPublisher();
				pushPublisher.subscribe(rsi.subscriber());
				System.out.println("Waiting for messages...");
			}

			public void onDisconnected() {
				RSI_SERVICE.stop();
			}

			public void onFailure(Throwable value) {
				try {
					pushPublisher.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				RSI_SERVICE.stop();

				value.printStackTrace();
				System.exit(-2);
			}

			public void onPublish(UTF8Buffer topic, Buffer msg, Runnable ack) {
				String body = msg.utf8().toString();

				if (body.trim().equals("SHUTDOWN")) {
					long diff = System.currentTimeMillis() - start;
					System.out.println(String.format("Received %d in %.2f seconds", count, (1.0 * diff / 1000.0)));

					try {
						pushPublisher.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
					RSI_SERVICE.stop();

					connection.disconnect(new Callback<Void>() {
						@Override
						public void onSuccess(Void value) {
							System.exit(0);
						}

						@Override
						public void onFailure(Throwable value) {
							value.printStackTrace();
							System.exit(-2);
						}
					});
				} else {
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
				ack.run();
			}
		});

		connection.connect(new Callback<Void>() {
			@Override
			public void onSuccess(Void value) {
				Topic[] topics = { new Topic(MqConfig.ACTIVEMQ_TOPIC_NAME, QoS.AT_LEAST_ONCE) };
				connection.subscribe(topics, new Callback<byte[]>() {
					public void onSuccess(byte[] qoses) {
					}

					public void onFailure(Throwable value) {
						value.printStackTrace();
						System.exit(-2);
					}
				});
			}

			@Override
			public void onFailure(Throwable value) {
				value.printStackTrace();
				System.exit(-2);
			}
		});

		// Wait forever..
		synchronized (Listener.class) {
			while (true)
				Listener.class.wait();
		}
	}
}
