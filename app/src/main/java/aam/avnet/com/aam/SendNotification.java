package aam.avnet.com.aam;

/**
 * Created by 914893 on 1/12/15.
 */


import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;


public class SendNotification extends AsyncTask<NotificationData,Void,Void> {

    public static String gcmURL = "https://android.googleapis.com/gcm/send";
    public static String APIKey = "key=AIzaSyA4YW7ZM8GnjNHVDXoBu2svqQkE8evODkk";

    @Override
    protected Void doInBackground(NotificationData... nd) {
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(gcmURL);
        JSONObject mainData = new JSONObject();

        try {
            JSONObject data = new JSONObject();
            data.putOpt("title", nd[0].getTitle());
            data.putOpt("message", nd[0].getMessage());
            data.putOpt("tripId",nd[0].getTripId());
            data.putOpt("responseDeviceId",nd[0].getCurrentDeviceId());

            JSONArray regIds = new JSONArray();
            regIds.put(nd[0].getDeviceId());
            mainData.put("registration_ids", regIds);
            mainData.put("data", data);


            StringEntity se = new StringEntity(mainData.toString());
            post.setEntity(se);
            post.addHeader("Authorization", APIKey);
            post.addHeader("Content-Type", "application/json");
            HttpResponse response = client.execute(post);


            //Log.d("response code =", Integer.toString(response.getStatusLine().getStatusCode()));
            BufferedReader rd = new BufferedReader(new InputStreamReader(response
                    .getEntity().getContent()));
            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            };
            //Log.d("Result", result.toString());
    }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}


