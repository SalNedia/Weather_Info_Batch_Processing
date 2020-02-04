package utils;

import java.io.Serializable;

public class FileInfo implements Serializable {

    private String country;
    private String date;
    private float value;

    public FileInfo(String country, String date, float value) {
        this.country = country;
        this.date = date;
        this.value = value;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }
}
