package com.google.cloud.backend.android;

import java.io.IOException;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.clientlogging.*;

public class CloudLog {
	
	public void Log (final String message) {


			new Thread(new Runnable() {
			    public void run() {
			    	
					Clientlogging.Builder builder = new Clientlogging.Builder (
							   AndroidHttp.newCompatibleTransport(), new GsonFactory(), null);
					Clientlogging service = builder.build();
					try {
						service.logs().log(message).execute();
						android.util.Log.d("log", message);

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						android.util.Log.e("log", e.toString());

					}
			    }
			  }).start();
			
			
			
	
	}

}
