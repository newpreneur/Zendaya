package club.cycl.zendaya;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;

public class CyClPoint {

    public final double latitute;
    public final double longitude;

    public double getLatitute() {
        return latitute;
    }
    public double getLongitude() {
        return longitude;
    }
    public CyClPoint(double latitute, double longitude) {
        this.latitute = latitute;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "CyClPoint{" +
                "latitute=" + latitute +
                ", longitude=" + longitude +
                '}';
    }
}
