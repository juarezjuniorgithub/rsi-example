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

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import oracle.rsi.ReactiveStreamsIngestion;

/**
 * A class that builds RSI service.
 */
public final class RSIService {

	/** ExecutorService that uses virtual threads from JDK 19 **/
	private ExecutorService workers;
	private ReactiveStreamsIngestion rsi;

	/**
	 * Start RSI
	 * 
	 * @return {@link oracle.rsi.ReactiveStreamsIngestion} object
	 */
	public ReactiveStreamsIngestion start() {
		if (rsi != null) {
			return rsi;
		}

		workers = Executors.newVirtualThreadPerTaskExecutor();
		rsi = ReactiveStreamsIngestion.builder().url(DatabaseConfig.getJdbcConnectionUrl()) // "jdbc:oracle:thin:@host:port/DB"
				.username(DatabaseConfig.USER).password(DatabaseConfig.PASSWORD).schema(DatabaseConfig.SCHEMA)
				.entity(Retailer.class).executor(workers)
				// .bufferRows(1000)
				.bufferInterval(Duration.ofSeconds(5)).build();

		return rsi;
	}

	/**
	 * Stop RSI
	 */
	public void stop() {
		if (rsi != null) {
			rsi.close();
		}

		if (workers != null) {
			workers.shutdown();
		}
	}

}
