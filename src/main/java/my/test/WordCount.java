package my.test;

import com.huangyueran.spark.utils.Constant;
import com.huangyueran.spark.utils.SparkUtils;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.VoidFunction;
import scala.Tuple2;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author huangyueran
 * @time 2019-7-21 16:38:20
 */
public class WordCount {
	public static void main(String[] args) {
		JavaSparkContext sc = SparkUtils.getLocalSparkContext(WordCount.class);
		//JavaSparkContext sc = SparkUtils.getRemoteSparkContext(WordCount.class);
		
		JavaRDD<String> text = sc.textFile(Constant.HDFS_FILE_PREX +"/user/zzm/hive-to-hdfs4/000000_0");
		JavaRDD<String> words = text.flatMap(new FlatMapFunction<String, String>() {
			private static final long serialVersionUID = 1L;

			@Override
			public Iterator<String> call(String line) throws Exception {
				return Arrays.asList(line.split(",")).iterator();
			}
		});

		JavaPairRDD<String, Integer> pairs = words.mapToPair(new PairFunction<String, String, Integer>() {
			private static final long serialVersionUID = 1L;

			@Override
			public Tuple2<String, Integer> call(String word) throws Exception {
				return new Tuple2<String, Integer>(word, 1);
			}
		});

		// 统计词出现次数
		JavaPairRDD<String, Integer> results = pairs.reduceByKey(new Function2<Integer, Integer, Integer>() {
			private static final long serialVersionUID = 1L;

			@Override
			public Integer call(Integer value1, Integer value2) throws Exception {
				return value1 + value2;
			}
		});

		// 键值对互换
		JavaPairRDD<Integer, String> temp = results
				.mapToPair(new PairFunction<Tuple2<String, Integer>, Integer, String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public Tuple2<Integer, String> call(Tuple2<String, Integer> tuple) throws Exception {
						return new Tuple2<Integer, String>(tuple._2, tuple._1);
					}
				});

		// 排序
		JavaPairRDD<String, Integer> sorted = temp.sortByKey(false)
				.mapToPair(new PairFunction<Tuple2<Integer, String>, String, Integer>() {
					private static final long serialVersionUID = 1L;

					@Override
					public Tuple2<String, Integer> call(Tuple2<Integer, String> tuple) throws Exception {
						return new Tuple2<String, Integer>(tuple._2, tuple._1);
					}
				});

		List<Tuple2<String, Integer>> list = sorted.collect();

		sorted.foreach(new VoidFunction<Tuple2<String, Integer>>() {
			private static final long serialVersionUID = 1L;

			@Override
			public void call(Tuple2<String, Integer> tuple) throws Exception {
				System.out.println("word:" + tuple._1 + "\tcount:" + tuple._2);
			}
		});

		for (Tuple2<String, Integer> t : list) {
			System.out.println(t._1 + "======" + t._2);
		}

		sc.close();
	}
}
