import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableInput;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.mapred.FsInput;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.Seekable;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;
import utils.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static String hdfs_uri = "hdfs://18.184.214.165:8020";

    private static String[] pathListCsv = {hdfs_uri + "/user/hdfs/temperature.csv", hdfs_uri + "/user/hdfs/pressure.csv", hdfs_uri + "/user/hdfs/humidity.csv", hdfs_uri + "/user/hdfs/city_attributes.csv", hdfs_uri + "/user/hdfs/weather_description.csv"};
    private static String[] pathListAvro = {hdfs_uri + "/user/hdfs/temperature.avro", hdfs_uri + "/user/hdfs/pressure.avro", hdfs_uri + "/user/hdfs/humidity.avro", hdfs_uri + "/user/hdfs/city_attributes.avro", hdfs_uri + "/user/hdfs/weather_description.avro"};
    private static String[] pathListParquet = {hdfs_uri + "/user/hdfs/temperature.parquet", hdfs_uri + "/user/hdfs/pressure.parquet", hdfs_uri + "/user/hdfs/humidity.parquet", hdfs_uri + "/user/hdfs/city_attributes.parquet", hdfs_uri + "/user/hdfs/weather_description.parquet"};

    public static long startTime;
    public static long startSQLTime;
    public static int typeFile;      //0 for csv,1 for avro,2 for parquet
    public static FileSystem fs;
    public static Path pathMetrics; //path to metrics file (needed for evaluate performance)
    public static int RUN = 5;      //number of run for


    public static void main(String[] args) throws Exception {

        int query = Integer.parseInt(args[0]);
        typeFile = Integer.parseInt(args[1]);


        //Set spark context
        SparkConf conf = new SparkConf()
                /* .setMaster("local")*/
                .setAppName("Query");
        JavaSparkContext sc = new JavaSparkContext(conf);
        sc.setLogLevel("ERROR");
        SparkSession spark = SparkSession.builder()
                .appName("App")
                .getOrCreate();

        //hadoop configuration
        Configuration hadoopconf = sc.hadoopConfiguration();
        hadoopconf.set("fs.defaultFS", hdfs_uri);
        hadoopconf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
        hadoopconf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
        hadoopconf.set("dfs.permissions", "false");

        fs = FileSystem.get(URI.create(hdfs_uri), hadoopconf);
        pathMetrics = new Path(hdfs_uri + "/metrics");
        FSDataOutputStream outputStream = fs.create(pathMetrics);

        //executes this code RUN times for metrics
        for (int i = 0; i < RUN; i++) {

            //starting time
            startTime = System.currentTimeMillis();
            //Set format and path in function of the file's format
            Dataset<Row> inputCity = null;
            String[] path = null;
            String format = "";

            switch (typeFile) {
                case 0:
                    inputCity = spark.read().format("csv").load(pathListCsv[3]);
                    format = "csv";
                    path = pathListCsv;
                    break;
                case 1:
                    inputCity = spark.read().format("avro").load(pathListAvro[3]);
                    format = "avro";
                    path = pathListAvro;
                    break;

                case 2:
                    inputCity = spark.read().format("parquet").load(pathListParquet[3]);
                    format = "parquet";
                    path = pathListParquet;
                    break;
            }

            //Get cities info
            JavaRDD<Row> city_info = inputCity.toJavaRDD();
            //Mapping city/country
            JavaPairRDD<String, String> cityCountryMapRDD = city_info.filter(x -> CityParser.check(x))
                    .mapToPair(c -> new Tuple2<>(CityParser.parse(c).getCity(), CountryMap.sendGet(c)));
            Map<String, String> map = cityCountryMapRDD.collectAsMap();
            HashMap<String, String> hmapCities = new HashMap<>(map);

            //Get cities'coordinates
            JavaPairRDD<String, Float[]> cityCoordinateRDD = city_info.filter(y -> CityParser.check(y))
                    .mapToPair(c -> new Tuple2<>(CityParser.parse(c).getCity(), new Float[]{Float.parseFloat(CityParser.parse(c).getLatitude()), Float.parseFloat(CityParser.parse(c).getLongitude())}));

            List<Float[]> values = cityCoordinateRDD.values().collect();
            List<String> cityList = cityCoordinateRDD.keys().collect();
            String[] citiesName = cityList.toArray(new String[cityList.size()]);

            //Get mapping pair zoneId/cities
            List<Tuple2<String, ZoneId>> mapping = UTCUtils.getZoneId(values, citiesName);
            JavaRDD rdd = sc.parallelize(mapping);
            JavaPairRDD<String, ZoneId> mappingPair = JavaPairRDD.fromJavaRDD(rdd);
            List<ZoneId> zoneIdList = mappingPair.values().collect();
            TimeUtils.calculateTime(Main.startTime, System.currentTimeMillis(), 0);
            JavaRDD<Row> tempRDD = null;

            //Execute query
            switch (query) {
                case 1:
                    /*  Process Query 1 */
                    Query1.getResponse(spark, path[4], zoneIdList, format, citiesName, i);
                    break;

                case 2:
                    /*  Process Query 2 */
                    Query2.getResponse(spark, path, zoneIdList, hmapCities, format, citiesName, i);
                    break;

                case 3:
                    /*  Process Query 3 */
                    Query3.getResponse(spark, path[0], zoneIdList, hmapCities, format, citiesName, i);
                    break;
                case 4:
                    /*Process all queries */
                    Query1.getResponse(spark, path[4], zoneIdList, format, citiesName, i);
                    tempRDD = Query2.getResponse(spark, path, zoneIdList, hmapCities, format, citiesName, i);
                    //caching results from query 2
                    tempRDD.cache();
                    Query3.getResponseFull(tempRDD, zoneIdList, hmapCities, citiesName, i);
                    break;
                case 5:
                    startSQLTime = System.currentTimeMillis();
                    /*Query2 in SparkSQL processing*/
                    for (int x = 0; x < path.length - 2; x++) {
                        JavaRDD<FileInfo> filemapRDD = FileUtils.mapper(sc, spark, path[x], zoneIdList, hmapCities, format, citiesName, x);
                        Query2.processSQL(spark, filemapRDD, i);
                    }

                default:
                    break;

            }
            //stopping time
            TimeUtils.calculateTime(startTime, System.currentTimeMillis(), 5);

        }
        TimeUtils.compute(outputStream);
        outputStream.close();
        spark.stop();
        sc.stop();
    }
}
