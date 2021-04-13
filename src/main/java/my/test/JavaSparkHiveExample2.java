/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package my.test;

// $example on:spark_hive$
import java.io.Serializable;

import org.apache.spark.sql.SparkSession;
// $example off:spark_hive$

import com.huangyueran.spark.utils.Constant;
import com.huangyueran.spark.utils.SparkUtils;

public class JavaSparkHiveExample2 {

	// $example on:spark_hive$
	public static class Record implements Serializable {
		private int key;
		private String value;

		public int getKey() {
			return key;
		}

		public void setKey(int key) {
			this.key = key;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
	// $example off:spark_hive$

	public static void main(String[] args) {
		// $example on:spark_hive$
		// warehouseLocation points to the default location for managed databases and
		// tables
		String warehouseLocation = Constant.HDFS_FILE_PREX + "/user/zzm/hive-to-hdfs4/000000_0";
		SparkSession spark = SparkSession.builder().appName("Java Spark Hive Example")
				.config(SparkUtils.getLocalSparkConf(SparkUtils.class))
				// .config(SparkUtils.getRemoteSparkConf(SparkUtils.class))
				.enableHiveSupport().getOrCreate();

		// Aggregation queries are also supported.
		// spark.sql("SELECT * FROM default.t25").show();
		 spark.sql("SELECT count(1) FROM t22 where stuid <100000").show();
		System.out.println("--------------------------------------------------------------------");
		//spark.sql("SELECT count(*) FROM t25").show();
		System.out.println("--------------------------------------------------------------------");
		spark.stop();
	}
}
