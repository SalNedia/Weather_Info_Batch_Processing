import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import scala.Tuple2;
import utils.*;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Query2 {


    public static JavaRDD<Row> getResponse(SparkSession spark, String[] pathList, List<ZoneId> zoneIdList, HashMap<String, String> hmapCities, String format, String[] citiesList, int indexCicle) throws IOException {

        //starting processing time
        long startQueryTime = System.currentTimeMillis();
        JavaRDD<Row> filterRDD, temp = null;

        //Get temperature values
        for (int i = 0; i < pathList.length - 2; i++) {
            int index = i;
            Dataset<Row> fileRow = spark.read().format(format).load(pathList[i]);
            JavaRDD<Row> fileRDD = fileRow.toJavaRDD();
            filterRDD = fileRDD.filter(x -> FileInfoParser.checkDate(x));
            //Caching temperatureRDD
            if (index == 0)
                temp = filterRDD;

            //RDD transformations
            JavaPairRDD<String, Float> fileInfoRDD = filterRDD.flatMapToPair(line -> FileInfoParser.parse(line, citiesList, hmapCities, zoneIdList, index, false)).sortByKey().cache();
            JavaPairRDD<String, Stats> avgRDD = fileInfoRDD.aggregateByKey((new Stats(0, 0, 0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)),
                    (v, x) -> new Stats(v.getSum() + x, v.getNum() + 1, v.getSumSquare() + Math.pow(x, 2), v.computeMin(x), v.computeMax(x)),
                    (v1, v2) -> new Stats(v1.getSum() + v2.getSum(), v1.getNum() + v2.getNum(), v1.getSumSquare() + v2.getSumSquare(), Math.min(v1.getMin(), v2.getMin()), Math.max(v1.getMax(), v2.getMax())));

            JavaPairRDD<String, String> AvgResult = avgRDD.mapToPair(p -> new Tuple2<>(p._1(), p._2().getValues())).sortByKey();
            //save output result on hdfs
            AvgResult.saveAsTextFile(Main.hdfs_uri + "/user/query2_" + i);
        }

        //stopping processing time
        TimeUtils.calculateTime(startQueryTime, System.currentTimeMillis(), 2);

        //deleting old outputs (needed only for re-execute this code many times for metrics computations)
        if (indexCicle != 4) {
            for (int i = 0; i < pathList.length - 2; i++) {
                Main.fs.delete(new Path(Main.hdfs_uri + "/user/query2_" + i), true);
            }
        }

        return temp;

    }

    public static void processSQL(SparkSession spark, JavaRDD<FileInfo> values, int indexCicle) throws IOException {

        Dataset<Row> df = createSchemaFromPreprocessedData(spark, values);
        // Register the DataFrame as a SQL temporary view
        df.createOrReplaceTempView("query2");
        Dataset<Row> result = spark.sql("SELECT DISTINCT country,timestamp,AVG(value),STD(value),MIN(value),MAX(value) FROM query2  " +
                "GROUP BY country, timestamp");

        result.toJavaRDD().saveAsTextFile(Main.hdfs_uri + "/user/query2sql_" + indexCicle);
        TimeUtils.calculateTime(Main.startSQLTime, System.currentTimeMillis(), 4);
        if (indexCicle != 4)
            Main.fs.delete(new Path(Main.hdfs_uri + "/user/query2sql_" + indexCicle), true);


    }

    private static Dataset<Row> createSchemaFromPreprocessedData(SparkSession spark,
                                                                 JavaRDD<FileInfo> values) {

        // Generate the schema based on the string of schema
        List<StructField> fields = new ArrayList<>();
        fields.add(DataTypes.createStructField("timestamp", DataTypes.StringType, true));
        fields.add(DataTypes.createStructField("country", DataTypes.StringType, true));
        fields.add(DataTypes.createStructField("value", DataTypes.FloatType, true));
        StructType schema = DataTypes.createStructType(fields);

        // Convert records of the RDD to Rows
        JavaRDD<Row> rowRDD = values.map(new Function<FileInfo, Row>() {
            @Override
            public Row call(FileInfo val) throws Exception {
                return RowFactory.create(val.getDate(), val.getCountry(), val.getValue());
            }
        });

        // Apply the schema to the RDD
        Dataset<Row> df = spark.createDataFrame(rowRDD, schema);

        return df;

    }
}
