import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;
import utils.FileInfoParser;
import utils.Stats;
import utils.TimeUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;

public class Query3 {
    public static void getResponse(SparkSession spark, String pathFile, List<ZoneId> zoneIdList, HashMap<String, String> hmapCities, String format, String[] citiesList,int indexCicle) throws IOException {

        //starting processing time
        long startQueryTime = System.currentTimeMillis();

        Dataset<Row> fileRow = spark.read().format(format).load(pathFile);
        JavaRDD<Row> tempRDD = fileRow.toJavaRDD();

        //RDD transformations
        JavaPairRDD<String,Float> tempInfoRDD=tempRDD.filter(x->FileInfoParser.checkDate(x))
                .flatMapToPair(line-> FileInfoParser.parse(line,citiesList,hmapCities,zoneIdList,0,true))
                .filter(x->(x._1().split("_")[2].equals("S") || x._1().split("_")[2].equals("W")) &&
                        (x._1().split("_")[3].split("-")[0].equals("2017") ||  x._1().split("_")[3].split("-")[0].equals("2016")));

        JavaPairRDD<String, String> output = compute(tempInfoRDD);
        output.saveAsTextFile(Main.hdfs_uri+"/user/query3");

        //stopping processing time
        TimeUtils.calculateTime(startQueryTime,System.currentTimeMillis(),3);

        //deleting old outputs (needed only for re-execute this code many times for metrics computations)
        if(indexCicle!=4)
            Main.fs.delete(new Path(Main.hdfs_uri+"/user/query3"),true);
    }

    public static JavaPairRDD<String,String> compute(JavaPairRDD<String,Float> tempInfoRDD){


        JavaPairRDD<String, Stats> avgRDD = tempInfoRDD.aggregateByKey((new Stats(0,0)),
                (v,x) -> new Stats(v.getSum()+x,v.getNum()+1),
                (v1,v2) -> new Stats(v1.getSum()+v2.getSum(),v1.getNum()+v2.getNum()));

        JavaPairRDD<String,String> avgResult = avgRDD.mapToPair( p->new Tuple2<>(p._1().split("_")[0]+"_"+p._1().split("_")[1]+"_"+p._1().split("_")[3] , p._1().split("_")[2]+"_"+p._2().getAvg()));
        JavaPairRDD<String, String> diffAvg = avgResult.reduceByKey((x, y) -> FileInfoParser.splitAvg(x, y))
                .mapToPair(p->new Tuple2<>(p._1().split("_")[0]+"_"+p._1().split("_")[2],p._2()+"_"+p._1().split("_")[1]));


        JavaPairRDD<String, Stats> rankRDD = diffAvg.aggregateByKey((new Stats(null)),
                (v,x) -> new Stats(v.addElement(x)),
                (v1,v2) -> new Stats(Lists.newArrayList(Iterables.concat(v1.getRank(),v2.getRank())))).cache();
        List<Tuple2<String, Stats>> rank2016 = rankRDD.filter(x -> x._1().split("_")[1].equals("2016")).collect();
        JavaPairRDD<String, Stats> rank2017 = rankRDD.filter(x -> x._1().split("_")[1].equals("2017"));

        JavaPairRDD<String,String>output=rank2017.mapToPair(p->new Tuple2<>(p._1(),p._2().computeRank(p._1(),rank2016)));
        return output;

    }


    public static void getResponseFull(JavaRDD<Row> tempRDD, List<ZoneId> zoneIdList, HashMap<String,String> hmapCities,String[] cities,int indexCicle) throws IOException {

        long startQueryTime = System.currentTimeMillis();

        JavaPairRDD<String, Float> tempInfoRDD = tempRDD.flatMapToPair(line -> FileInfoParser.parse(line, cities, hmapCities, zoneIdList, 0, true)).sortByKey()
                .filter(x -> (x._1().split("_")[2].equals("S") || x._1().split("_")[2].equals("W")) &&
                        (x._1().split("_")[3].split("-")[0].equals("2017") || x._1().split("_")[3].split("-")[0].equals("2016")));
        JavaPairRDD<String, String> output = compute(tempInfoRDD);

        output.saveAsTextFile(Main.hdfs_uri+"/user/query3");
        TimeUtils.calculateTime(startQueryTime,System.currentTimeMillis(),3);
        if(indexCicle!=4)
            Main.fs.delete(new Path(Main.hdfs_uri+"/user/query3"),true);


    }

}
