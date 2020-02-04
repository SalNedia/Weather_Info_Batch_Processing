
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;
import utils.*;

import java.io.IOException;
import java.time.ZoneId;
import java.util.List;

public class Query1 {

    public static void getResponse(SparkSession spark, String pathWeather, List<ZoneId> zoneIdList, String format, String[] citiesList,int indexCicle) throws IOException {

        //starting processing time
        long startQueryTime = System.currentTimeMillis();

        Dataset<Row> weather_file=spark.read().format(format).load(pathWeather);
        JavaRDD<Row> weather_info=weather_file.toJavaRDD();

        //RDD transformations
        JavaPairRDD<String, Integer> weather_infoJavaRDD=weather_info.filter(x->FileInfoParser.checkDate(x))
                .flatMapToPair(line->FileInfoParser.parseDescription(line,citiesList,zoneIdList))
                .filter(x->x._1().split("-")[1].equals("03") || x._1().split("-")[1].equals("04") || x._1().split("-")[1].equals("05") )
                .reduceByKey((k,z)->k+z)
                .filter(f->f._2()>=18)
                .mapToPair(p->new Tuple2<>(FileInfoParser.getKey(p._1()),1))
                .reduceByKey((x,y)->x+y).filter(p->p._2()>=15)
                .mapToPair(p->new Tuple2<>(FileInfoParser.getKey2(p._1()),1))
                .reduceByKey((x,y)->x+y).filter(p->p._2()>=3);
        JavaPairRDD<String, Iterable<String>> results= weather_infoJavaRDD.mapToPair(p-> new Tuple2<>(p._1().split("_")[1],p._1().split("_")[0])).groupByKey();

        //save output result on hdfs
        results.saveAsTextFile(Main.hdfs_uri+"/user/query1");

        //stopping processing time
        TimeUtils.calculateTime(startQueryTime,System.currentTimeMillis(),1);

        //deleting old outputs (needed only for re-execute this code many times for metrics computations)
        if(indexCicle!=4)
            Main.fs.delete(new Path(Main.hdfs_uri+"/user/query1"),true);





    }
}
