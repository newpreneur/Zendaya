package club.cycl.zendaya;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import java.util.Arrays;

public class CyClSensor {
    private  int accuracy;
    private long timestamp;
    private String sensorName;
    private String sensor_id;
    private String values;

    public CyClSensor(SensorEvent sensorEvent, String sensor_id) {
        this.sensorName =sensorEvent.sensor.getName();
        this.values=Arrays.toString(sensorEvent.values);
        this.sensor_id=sensor_id;
        this.timestamp=sensorEvent.timestamp;
        this.accuracy=sensorEvent.accuracy;
    }

    public CyClSensor() {
    }

    public int getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSensorName() {
        return sensorName;
    }

    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
    }

    public String getSensor_id() {
        return sensor_id;
    }

    public void setSensor_id(String sensor_id) {
        this.sensor_id = sensor_id;
    }


    public String getValues() {
        return values;
    }

    public void setValues(String values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "CyClSensor{" +
                "accuracy=" + accuracy +
                ", timestamp=" + timestamp +
                ", sensorName='" + sensorName + '\'' +
                ", sensor_id='" + sensor_id + '\'' +
                ", values='" + values + '\'' +
                '}';
    }
}
