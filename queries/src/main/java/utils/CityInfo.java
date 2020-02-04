package utils;

import java.io.Serializable;

public class CityInfo implements Serializable {

    private String city;
    private String latitude;
    private String longitude;

    public CityInfo(String city, String latitude, String longitude) {
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;

    }

    public String getCity() {
        return city;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }
}
