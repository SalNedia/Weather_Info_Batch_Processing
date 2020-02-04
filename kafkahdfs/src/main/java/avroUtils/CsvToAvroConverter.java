package avroUtils;

import java.io.*;

public class CsvToAvroConverter {

   
    public static void converter(String topic) throws Exception {
        {
            {
                String[] header=createSchema(topic).split(",",-1);
                File schemaFile = new File("avsc/" + topic + ".avsc");
                CsvToAvroGenericWriter writer = new CsvToAvroGenericWriter(schemaFile, "avro/" + topic + ".avro", CsvToAvroGenericWriter.MODE_WRITE);
                writer.setCsvHeader(header);
                int i = 0;
                try (BufferedReader br = new BufferedReader(new FileReader("data/" + topic + ".csv"))) {
                    String line;

                    while ((line = br.readLine()) != null) {

                        i++;
                        if (i != 1)
                            writer.append(line);

                    }
                }
                writer.closeWriter();


            }
        }
    }

    public static String createSchema(String topic) throws IOException {

        File fileAvsc = new File("avsc/" + topic + ".avsc");
        fileAvsc.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileAvsc));
        String row = "";
        String hd = "";

        try (BufferedReader br = new BufferedReader(new FileReader("data/" + topic + ".csv"))) {
            String line;
            String header = "{\"namespace\": \"utils\",\n\"type\": \"record\",\n \"name\": \"" + topic + "\",\n \"version\": \"1\",\n \"fields\": [\n";


            String[] value;
            String temp="";
            if ((line = br.readLine()) != null) {

                writer.write(header);
                value = line.split(",", -1);
                for (int i=0; i<value.length;i++) {
                    if (value[i].contains(" ")) {
                        String[] split = value[i].split(" ");
                        temp= "";
                        for (int j = 0; j < split.length; j++) {
                            temp+= split[j];
                            if ( j!= split.length - 1)
                                temp += "_";
                        }
                    }
                    else
                        temp=value[i];



                    if(i!=value.length-1) {
                        hd += temp+ ",";
                        row = "{\"name\": \"" + temp + "\", \"type\": \"string\"},\n";
                    }
                    else {
                        hd += temp ;
                        row = "{\"name\": \"" + temp + "\", \"type\": \"string\"}\n";
                    }
                    writer.write(row);
                }

            }
            br.close();
            writer.write("]\n }");
            writer.close();

        }
        return hd;
    }

}





