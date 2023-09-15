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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * <p>
 * Configuration for connecting code samples to ActiveMQ.
 * </p>
 */
public class MqConfig {

	private static final Properties CONFIG = new Properties();

	static {
		try {
			var fileStream = Files
					.newInputStream(Path.of("<YOUR_WORKSPACE_ROOT_DIRECTORY>\\rsi-example\\src\\main\\resources\\mq.properties"));
			CONFIG.load(fileStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** User name that connects to ActiveMQ */
	public static final String ACTIVEMQ_USER = CONFIG.getProperty("ACTIVEMQ_USER");

	/** Host name where ActiveMQ is running */
	public static final String ACTIVEMQ_HOST = CONFIG.getProperty("ACTIVEMQ_HOST");

	/** Password of the user that connects to ActiveMQ */
	public static final String ACTIVEMQ_PASSWORD = CONFIG.getProperty("ACTIVEMQ_PASSWORD");

	/** Password of the user that connects to ActiveMQ via REST (HTTP BASIC) */
	public static final String ACTIVEMQ_HTTP_BASIC_PASSWORD = CONFIG.getProperty("ACTIVEMQ_HTTP_BASIC_PASSWORD");

	/** Port number where ActiveMQ / AMQP is listening */
	public static final int ACTIVEMQ_AMQP_PORT = Integer.parseInt(CONFIG.getProperty("ACTIVEMQ_AMQP_PORT"));

	/** Topic name for ActiveMQ */
	public static final String ACTIVEMQ_TOPIC_NAME = CONFIG.getProperty("ACTIVEMQ_TOPIC_NAME");

	/** Topic URI path for ActiveMQ */
	public static final String ACTIVEMQ_TOPIC_URI = "/topic/";

	/** Topic param for ActiveMQ */
	public static final String ACTIVEMQ_TOPIC_PARAM = "type=topic";

	/** AMQP URL SCHEME */
	public static final String AMQP_URL_SCHEME = "amqp://";

	/** TCP URL SCHEME */
	public static final String TCP_URL_SCHEME = "tcp://";

	/** HTTP URL SCHEME */
	public static final String HTTP_URL_SCHEME = "http://";

	/** Colon for URL composition */
	public static final String COLON = ":";

	/** Question mark for URL composition */
	public static final String QUESTION_MARK = "?";

	/** ActiveMQ Client URI */
	public static final String ACTIVEMQ_CLIENT_URI = "/api/message/event;";

	/** Port number where ActiveMQ / STOMP is listening */
	public static final int ACTIVEMQ_STOMP_PORT = Integer.parseInt(CONFIG.getProperty("ACTIVEMQ_STOMP_PORT"));

	/** Port number where ActiveMQ / MQTT is listening */
	public static final int ACTIVEMQ_MQTT_PORT = Integer.parseInt(CONFIG.getProperty("ACTIVEMQ_MQTT_PORT"));

	/** Port number for clients */
	public static final int ACTIVEMQ_CLIENT_PORT = Integer.parseInt(CONFIG.getProperty("ACTIVEMQ_CLIENT_PORT"));

}
