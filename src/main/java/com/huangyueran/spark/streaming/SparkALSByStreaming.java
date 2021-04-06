package com.huangyueran.spark.streaming;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.mllib.recommendation.ALS;
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel;
import org.apache.spark.mllib.recommendation.Rating;
import org.apache.spark.rdd.RDD;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kafka.serializer.StringDecoder;
import scala.Tuple2;

/**
 * @author huangyueran
 * @category 鍩轰簬Spark-streaming銆乲afka鐨勫疄鏃舵帹鑽愭ā鏉緿EMO 鍘熺郴缁熶腑鍖呭惈鍟嗗煄椤圭洰銆乴ogback銆乫lume銆乭adoop
 * The real time recommendation template DEMO based on Spark-streaming and Kafka contains the mall project, logback, flume and Hadoop in the original system
 */
public final class SparkALSByStreaming {

    private static final Logger log = LoggerFactory.getLogger(SparkALSByStreaming.class);

    private static final String KAFKA_ADDR = "middleware:9092";
    private static final String TOPIC = "RECOMMEND_TOPIC";
    private static final String HDFS_ADDR = "hdfs://middleware:9000";

    private static final String MODEL_PATH = "/spark-als/model";


    //	鍩轰簬Hadoop銆丗lume銆並afka銆乻park-streaming銆乴ogback銆佸晢鍩庣郴缁熺殑瀹炴椂鎺ㄨ崘绯荤粺DEMO
    //	Real time recommendation system DEMO based on Hadoop, Flume, Kafka, spark-streaming, logback and mall system
    //	鍟嗗煄绯荤粺閲囬泦鐨勬暟鎹泦鏍煎紡 Data Format:
    //	鐢ㄦ埛ID锛屽晢鍝両D锛岀敤鎴疯涓鸿瘎鍒嗭紝鏃堕棿鎴�
    //	UserID,ItemId,Rating,TimeStamp
    //	53,1286513,9,1508221762
    //	53,1172348420,9,1508221762
    //	53,1179495514,12,1508221762
    //	53,1184890730,3,1508221762
    //	53,1210793742,159,1508221762
    //	53,1215837445,9,1508221762

    public static void main(String[] args) {
        System.setProperty("HADOOP_USER_NAME", "root"); // 璁剧疆鏉冮檺鐢ㄦ埛

        SparkConf sparkConf = new SparkConf().setAppName("JavaKafkaDirectWordCount").setMaster("local[1]");

        final JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, Durations.seconds(6));

        Map<String, String> kafkaParams = new HashMap<String, String>(); // key鏄痶opic鍚嶇О,value鏄嚎绋嬫暟閲�
        kafkaParams.put("metadata.broker.list", KAFKA_ADDR); // 鎸囧畾broker鍦ㄥ摢
        HashSet<String> topicsSet = new HashSet<String>();
        topicsSet.add(TOPIC); // 鎸囧畾鎿嶄綔鐨則opic

        // Create direct kafka stream with brokers and topics
        // createDirectStream()
        JavaPairInputDStream<String, String> messages = KafkaUtils.createDirectStream(jssc, String.class, String.class,
                StringDecoder.class, StringDecoder.class, kafkaParams, topicsSet);

        JavaDStream<String> lines = messages.map(new Function<Tuple2<String, String>, String>() {
            public String call(Tuple2<String, String> tuple2) {
                return tuple2._2();
            }
        });

        JavaDStream<Rating> ratingsStream = lines.map(new Function<String, Rating>() {
            public Rating call(String s) {
                String[] sarray = StringUtils.split(StringUtils.trim(s), ",");
                return new Rating(Integer.parseInt(sarray[0]), Integer.parseInt(sarray[1]),
                        Double.parseDouble(sarray[2]));
            }
        });

        // 杩涜娴佹帹鑽愯绠�
        ratingsStream.foreachRDD(new VoidFunction<JavaRDD<Rating>>() {

            public void call(JavaRDD<Rating> ratings) throws Exception {
                //  鑾峰彇鍒板師濮嬬殑鏁版嵁闆�
                SparkContext sc = ratings.context();

                RDD<String> textFileRDD = sc.textFile(HDFS_ADDR + "/flume/logs", 3); // 璇诲彇鍘熷鏁版嵁闆嗘枃浠�
                JavaRDD<String> originalTextFile = textFileRDD.toJavaRDD();

                final JavaRDD<Rating> originaldatas = originalTextFile.map(new Function<String, Rating>() {
                    public Rating call(String s) {
                        String[] sarray = StringUtils.split(StringUtils.trim(s), ",");
                        return new Rating(Integer.parseInt(sarray[0]), Integer.parseInt(sarray[1]),
                                Double.parseDouble(sarray[2]));
                    }
                });
                log.info("========================================");
                log.info("Original TextFile Count:{}", originalTextFile.count()); // HDFS涓凡缁忓瓨鍌ㄧ殑鍘熷鐢ㄦ埛琛屼负鏃ュ織鏁版嵁
                log.info("========================================");

                //  灏嗗師濮嬫暟鎹泦鍜屾柊鐨勭敤鎴疯涓烘暟鎹繘琛屽悎骞�
                JavaRDD<Rating> calculations = originaldatas.union(ratings);

                log.info("Calc Count:{}", calculations.count());

                // Build the recommendation model using ALS
                int rank = 10; // 妯″瀷涓殣璇箟鍥犲瓙鐨勪釜鏁�
                int numIterations = 6; // 璁粌娆℃暟

                // 寰楀埌璁粌妯″瀷
                if (!ratings.isEmpty()) { // 濡傛灉鏈夌敤鎴疯涓烘暟鎹�
                    MatrixFactorizationModel model = ALS.train(JavaRDD.toRDD(calculations), rank, numIterations, 0.01);
                    //  鍒ゆ柇鏂囦欢鏄惁瀛樺湪,濡傛灉瀛樺湪 鍒犻櫎鏂囦欢鐩綍
                    Configuration hadoopConfiguration = sc.hadoopConfiguration();
                    hadoopConfiguration.set("fs.defaultFS", HDFS_ADDR);
                    FileSystem fs = FileSystem.get(hadoopConfiguration);
                    Path outpath = new Path(MODEL_PATH);
                    if (fs.exists(outpath)) {
                        log.info("########### 鍒犻櫎" + outpath.getName() + " ###########");
                        fs.delete(outpath, true);
                    }

                    // 淇濆瓨model
                    model.save(sc, HDFS_ADDR + MODEL_PATH);

                    //  璇诲彇model
                    MatrixFactorizationModel modelLoad = MatrixFactorizationModel.load(sc, HDFS_ADDR + MODEL_PATH);
                    // 涓烘寚瀹氱敤鎴锋帹鑽�10涓晢鍝�(鐢靛奖)
                    for(int userId=0;userId<30;userId++){ // streaming_sample_movielens_ratings.txt
                        Rating[] recommendProducts = modelLoad.recommendProducts(userId, 10);
                        log.info("get recommend result:{}", Arrays.toString(recommendProducts));
                    }
                }

            }
        });

        // ==========================================================================================

        jssc.start();
        try {
            jssc.awaitTermination();
        } catch (Exception e) {
            e.printStackTrace();
        }


        // Local Model
        try {
            Thread.sleep(10000000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // jssc.stop();
        // jssc.close();
    }

}