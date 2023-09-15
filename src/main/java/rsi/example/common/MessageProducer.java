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

package rsi.example.common;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.IntStream;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;

public class MessageProducer {

	private static final OracleJsonFactory JSON_FACTORY = new OracleJsonFactory();
	private static final ResteasyClient client = (ResteasyClient) ClientBuilder.newClient();

	private static final ResteasyWebTarget target = client
			.target(MqConfig.HTTP_URL_SCHEME + MqConfig.ACTIVEMQ_HOST + MqConfig.COLON + MqConfig.ACTIVEMQ_CLIENT_PORT
					+ MqConfig.ACTIVEMQ_CLIENT_URI + MqConfig.QUESTION_MARK + MqConfig.ACTIVEMQ_TOPIC_PARAM);

	public static void main(String args[]) {

		target.register(new AuthHeadersRequestFilter(MqConfig.ACTIVEMQ_HTTP_BASIC_PASSWORD,
				MqConfig.ACTIVEMQ_HTTP_BASIC_PASSWORD));
		Instant start = Instant.now();

		var threads = IntStream.range(0, 50).mapToObj(i -> Thread.startVirtualThread(() -> {
			sendMessage();
			System.out.println("Message #: " + (i));

		})).toList();

		for (var thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).getSeconds();
		System.out.println("Elapsed: " + timeElapsed);
		client.close();

	}

	private static void sendMessage() {

		Response response = null;
		try {

			String body = "{\"rank\": 1,\r\n" + "\"msr\": 217,\r\n" + "\"retailer\": \"100224\",\r\n"
					+ "\"name\": \"Freddys One Stop\",\r\n" + "\"city\": \"Roland\",\r\n"
					+ "\"phone\": \"(918) 503-6288\",\r\n" + "\"terminal_type\": \"Extrema\",\r\n"
					+ "\"weeks_active\": 37,\r\n" + "\"instant_sales_amt\": \"$318,600.00 \",\r\n"
					+ "\"online_sales_amt\": \"$509,803.00 \",\r\n" + "\"total_sales_amt\": \"$828,403.00 \"}";

			OracleJsonObject jsonObject = JSON_FACTORY.createJsonTextValue(new ByteArrayInputStream(body.getBytes()))
					.asJsonObject();

			Retailer retailer = new Retailer(jsonObject);

			response = target.request().post(Entity.json(retailer));
			System.out.println(response.getStatusInfo());
		} finally {
			response.close();
		}

	}

}