package com.google.cloud.backend.android.sample.geekwatch;

import java.io.IOException;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.cloud.backend.android.CloudBackendActivity;
import com.google.cloud.backend.android.CloudCallbackHandler;
import com.google.cloud.backend.android.CloudEntity;
import com.google.cloud.backend.android.CloudQuery;
import com.google.cloud.backend.android.F;
import com.google.cloud.backend.android.R;
import com.google.cloud.backend.android.CloudQuery.Scope;

public class GeekwatchActivity extends CloudBackendActivity {

     String postId = "";  //Id of the entity this user updates on the server to store location

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_geekwatch);
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);

		GoogleMap map = ((SupportMapFragment)  getSupportFragmentManager().findFragmentById(R.id.map))
	               .getMap();
		map.getUiSettings().setCompassEnabled(true);
		map.setMyLocationEnabled(true);
	  
	}
	
	public void updateLocations ()
	{
		 CloudCallbackHandler<List<CloudEntity>> handler = new CloudCallbackHandler<List<CloudEntity>>() {
		      @Override
		      public void onComplete(List<CloudEntity> results) {
		    	  //TODO update the UI with this data
		    	
		      }

		      @Override
		      public void onError(IOException exception) {
		      }
		    };

		    // execute the query with the handler
		    CloudQuery cq = new CloudQuery("Location");
		    Geohash e = new Geohash();
		    double [] gps = new double[] {37.42783,-122.126985} ;//get current GPS
		  
		 

		    cq.setFilter(F.eq("geohash", e.encode(gps[0], gps[1])));
		    cq.setLimit(50);
		    cq.setScope(Scope.FUTURE_AND_PAST);
		    getCloudBackend().list(cq, handler);
		
	}
	
	public void postLocation (int gps[]) {

		    // create a response handler that will receive the result or an error
		    CloudCallbackHandler<CloudEntity> handler = new CloudCallbackHandler<CloudEntity>() {
		      @Override
		      public void onComplete(final CloudEntity result) {
		    	  postId = result.getId();
		    	
		      }

		      @Override
		      public void onError(final IOException exception) {
		      //  handleEndpointException(exception);
		      }
		    };
		    Geohash geohash = new Geohash();
			CloudEntity newPost = new CloudEntity("Location");
			newPost.setId(postId);
		    newPost.put("geohash", geohash.encode(gps[0], gps[1]));
		    getCloudBackend().insert(newPost, handler);
	}
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_geekwatch, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
