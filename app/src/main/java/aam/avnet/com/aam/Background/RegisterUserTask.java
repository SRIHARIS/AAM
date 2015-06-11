package aam.avnet.com.aam.Background;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import aam.avnet.com.aam.Constants;
import aam.avnet.com.aam.UserProfile;


/**
 * Update the userprofile to Google App engine DB
 */
public class RegisterUserTask extends AsyncTask<UserProfile,Void,Boolean> {


    @Override
    protected Boolean doInBackground(UserProfile... params) {

        boolean isSuccessful = true;
        HttpClient client = new DefaultHttpClient();
        String url = "https://mobile.ng.bluemix.net:443/data/rest/v1/apps/55ceaafa-8656-44ac-80d2-036ce6d8fba6/injections?classname=UserProfile";
        HttpPost post = new HttpPost(url);
        post.addHeader("app_id", Constants.APP_ID);
        post.addHeader("IBM-Application-Secret", Constants.SECRET);
        post.addHeader("classname","UserProfile");
        try {
            JSONArray arr = new JSONArray();
            JSONObject object = new JSONObject();
            object.put("USERNAME", params[0].getUsername());
            object.put("MODEL",params[0].getVehicleModel());
            object.put("MANUFACTURER", params[0].getVehcileMan());
            arr.put(object);
            //pairs.
            StringEntity se = new StringEntity( arr.toString());
            post.setEntity(se);
            HttpResponse resp = client.execute(post);
            Log.d("resp",resp.getStatusLine().toString());
            Log.d("resp",post.getRequestLine().toString());
        }catch(Exception e){
            e.printStackTrace();
        }
        return isSuccessful;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        Log.d("Updated","User");
    }
}
