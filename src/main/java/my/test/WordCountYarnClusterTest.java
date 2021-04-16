package my.test;

import java.io.IOException;

import org.apache.spark.deploy.SparkSubmit;

import com.huangyueran.spark.utils.Constant;

public class WordCountYarnClusterTest {
public static void main(String[] args) throws IOException {
	System.setProperty("HADOOP_USER_NAME", "hadoop");
	System.setProperty("user.name", "hadoop");
	
	//System.setProperty("HADOOP_CONF_DIR", "C:\\eclipse-workspace\\SparkDemo\\src\\main\\resources");
	//System.setProperty("HADOOP_CONF_DIR", "C:\\my\\soft\\hadoop\\hadoop-2.8.5\\hadoop-2.8.5\\etc\\hadoop");
	
	
	System.out.println("------------"+System.getenv("HADOOP_CONF_DIR"));
	
	System.out.println("------------"+System.getenv("HADOOP_HOME"));
	
	
	String appName = "wordCount-yarn-cluster";
	String className = "my.test.WordCountCluster";
	String path = "C:\\eclipse-workspace\\SparkDemo\\target\\SparkDemo-1.0-SNAPSHOT.jar";
	path = Constant.HDFS_FILE_PREX +"/user/zzm/SparkDemo-1.0-SNAPSHOT.jar";
		String [] arg0=new String[]{
		 "--jars",Constant.HDFS_FILE_PREX +"/user/zzm/spark-lib",		
		"--master","yarn",//ip端口
		"--deploy-mode","cluster",
		 "--name",appName,
		 "--class",className,//运行主类main
		 //"--spark.yarn.archive",Constant.HDFS_FILE_PREX + "/user/zzm/spark-lib",
		 "--executor-memory","2G",
		 "--total-executor-cores","10",
		 "--executor-cores","2",
		 path,//在linux上的包 可改为hdfs上面的路径
		// "LR", "20180817111111", "66"//jar中的参数，注意这里的参数写法
		};
		SparkSubmit.main(arg0);
}
}