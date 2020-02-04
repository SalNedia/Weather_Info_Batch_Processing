* [Application usage](#application-usage)
* [Deployment](#aws-deployment)

To execute the all application there are several scripts available in the directory `/NOME/run`
There are three main jar to execute the all project
First of all you need to start the vpcConfigFinal.py script by specifing AWS_ACCESS_KEY_ID,AWS_SECRET_ACCESS_KEY,KEY_PEM that allows you to create the Virtual Private Cloud (VPC) network that will represent your environment.For development simplicity all ports need to be opened for Ingress traffic.
Then you need to create Hadoop CLuster with Amazon Emr by specifing the vpc network created before.
Before the building sf the jar change hdfs address in the files and the kafka address in ProducerKafka and ConsumerKafka in the properties bootstrap server(for Kafka application).

The jar Kafkahdfs has one entrypoint the TypeOfFIle representing the type of file you want to ingest.
The jar QuerySabd has two main entrypoint, first for the type of query to execute(4 for all query) and the other for the type of format(0 for csv,1 for avro and 2 for parquet).
The jar Hbase has one two entrypoint, one for the type of query(4 for all) and one for the number of Hadoop supervisor.

### Step for running jars

1)In order to launch the app you need to start the transfDataset.sh(for transfer file by local pc to ec2 machine) and specyfing three parameters:
        1)hadoop emr address
        2)path where dataset
        3)kafka jar path
2)Transfer all jar in hadoop emr machine

3)Then enter in hadoop emr machine, create dir data,avsc,avro and parquet and move all csv file in the dir data.
4) java -jar kafka jar "type of file"
5) You can run the application on a Spark cluster exploiting the spark-submit.sh script in the master node. Move to /usr/ and execute ./bin/spark-submit --packages org.apache.spark:spark-avro_2.11:2.4.0
                   --deploy-mode cluster --master yarn
                   /home/hadoop/sabd_query-1.0-SNAPSHOT-jar-with-dependencies.jar  <query to execute (4 for all)>   <file format>
6) Now you can import the output in hbase with java -jar hbasejar "type of query(4 for all)" "number of supervisor"


