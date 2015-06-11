package aam.avnet.com.aam;

import android.content.Context;

public class NotificationData {

	private String title;
	private String message;
    private String tripId;
    private String deviceId;
    private Context context;
    private String currentDeviceId;

    /**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getCurrentDeviceId() {
        return currentDeviceId;
    }

    public void setCurrentDeviceId(String currentDeviceId) {
        this.currentDeviceId = currentDeviceId;
    }
}
