package sapadvrtisrproj.ms.sapientia.ro.allam4.Data;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.Timestamp;

public class User {

    private String bus;
    private String latitude;
    private String longitude;
    private String status;
    private Timestamp timestamp;
    private String id;


    public User() {
    }

    public User(String bus, String latitude, String longitude, String status, Timestamp timestamp) {
        this.bus = bus;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.timestamp = timestamp;
    }

    public User(String bus, String latitude, String longitude, String status, Timestamp timestamp, String id) {
        this.bus = bus;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.timestamp = timestamp;
        this.id = id;
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

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    //    public User(String bus, String latitude, String longitude, String status) {
//        this.bus = bus;
//        this.latitude = latitude;
//        this.longitude = longitude;
//        this.status = status;
//    }
//
//    public String getBus() {
//        return bus;
//    }
//
//    public void setBus(String bus) {
//        this.bus = bus;
//    }
//
//    public String getLatitude() {
//        return latitude;
//    }
//
//    public void setLatitude(String latitude) {
//        this.latitude = latitude;
//    }
//
//    public String getLongitude() {
//        return longitude;
//    }
//
//    public void setLongitude(String longitude) {
//        this.longitude = longitude;
//    }
//
//    public String getStatus() {
//        return status;
//    }
//
//    public void setStatus(String status) {
//        this.status = status;
//    }
}
