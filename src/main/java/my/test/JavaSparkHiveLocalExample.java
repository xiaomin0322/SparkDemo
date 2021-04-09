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
import java.util.ArrayList;
import java.util.List;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
// $example off:spark_hive$

import com.huangyueran.spark.utils.SparkUtils;

public class JavaSparkHiveLocalExample {

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
    // warehouseLocation points to the default location for managed databases and tables
    String warehouseLocation = "spark-warehouse";
    SparkSession spark = SparkSession
      .builder()
      .appName("Java Spark Hive Example")
      //.config("spark.sql.warehouse.dir", warehouseLocation)
      .config(SparkUtils.getLocalSparkConf(SparkUtils.class))
      .enableHiveSupport()
      .getOrCreate();

    spark.sql("CREATE TABLE IF NOT EXISTS src (key INT, value STRING)");
    spark.sql("LOAD DATA LOCAL INPATH 'data/resources/kv1.txt' INTO TABLE src");

    // Queries are expressed in HiveQL
    spark.sql("SELECT * FROM src").show();
    // +---+-------+
    // |key|  value|
    // +---+-------+
    // |238|val_238|
    // | 86| val_86|
    // |311|val_311|
    // ...


    spark.stop();
  }
}
