package aam.avnet.com.aam;

/**
 * Created by 914893 on 1/7/15.
 */
public class UserProfile {

    private String username;
    private String deviceId;
    private String vehicleModel;
    private String vehcileMan;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getVehcileMan() {
        return vehcileMan;
    }

    public void setVehcileMan(String vehcileMan) {
        this.vehcileMan = vehcileMan;
    }
}