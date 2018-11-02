package sapadvrtisrproj.ms.sapientia.ro.allam4.Data;

import com.google.firebase.firestore.GeoPoint;

public class User {
    private String bus;
    private String latitude;
    private String longitude;
    private String status;

    public User(String bus, String latitude, String longitude, String status) {
        this.bus = bus;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
    }

    public String getBus() {
        return bus;
    }

    public void setBus(String bus) {
        this.bus = bus;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
