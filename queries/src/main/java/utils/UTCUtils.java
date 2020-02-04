package utils;
import net.iakovlev.timeshape.TimeZoneEngine;
import scala.Tuple2;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class UTCUtils {

    public static String convert(ZoneId zoneId,String date){
        DateTimeFormatter formatter=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime localDateTime=LocalDateTime.parse(date,formatter);
        ZonedDateTime convertZoneDateTime=ZonedDateTime.of(localDateTime,ZoneOffset.UTC.normalized());
        ZonedDateTime newZoneDateTime = convertZoneDateTime.withZoneSameInstant(zoneId);
        return formatter.format(newZoneDateTime);
    }

    public static List<Tuple2<String, ZoneId>> getZoneId(List<Float[]> latlon, String[] cities) {

        //Get timeZone with city's coordinate
        TimeZoneEngine engine= TimeZoneEngine.initialize();
        List<Tuple2<String,ZoneId>> results=new ArrayList<>();
        int i=0;
        for(Float[] value:latlon) {
            Optional<ZoneId> maybeZoneId = engine.query(value[0], value[1]);
            ZoneId zoneId = maybeZoneId.get();
            Tuple2<String,ZoneId> result=new Tuple2<>(cities[i],zoneId);
            i++;
            results.add(result);
        }
        return results;

    }




}
