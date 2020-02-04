package utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.spark.sql.Row;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CountryMap {


    // Get country for each city
    public static String sendGet(Row line) throws Exception {
        CityInfo city = CityParser.parse(line);
        String url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=" + city.getLatitude() + "&lon=" + city.getLongitude() + "&accept-language=en";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.addRequestProperty("REFERER", "https://nominatim.openstreetmap.org");
        //add request header
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        //Parsing response
        JsonObject jsonObject = (JsonObject) new JsonParser().parse(String.valueOf(response));
        JsonObject address = (JsonObject) jsonObject.get("address");
        String country = String.valueOf(address.get("country"));
        in.close();
        return country;

    }

}
