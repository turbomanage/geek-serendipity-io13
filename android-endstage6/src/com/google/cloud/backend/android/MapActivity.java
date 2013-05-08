package com.google.cloud.backend.android;

import java.io.IOException;
import java.util.List;

import android.location.Location;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.cloud.backend.android.CloudQuery.Scope;

public class MapActivity extends CloudBackendActivity implements
		OnMyLocationChangeListener {

	
	
	

	private String Android_id = "unknown";
	private CloudLog cloudLog = new CloudLog();
	
	private GoogleMap mMap;
	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the
		// map.
		Android_id = Secure.getString(getApplicationContext().getContentResolver(),
                Secure.ANDROID_ID); 
		
		if (mMap == null) {
			// Try to obtain the map from the SupportMapFragment.
			mMap = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map)).getMap();
			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				setUpMap();
			}
		}
	}

	private void setUpMap() {
		mMap.setMyLocationEnabled(true);
		mMap.setOnMyLocationChangeListener(this);
		cloudLog.Log(this.Android_id + ": "+ "from setUpMap()");

	}

	@Override
	public void onMyLocationChange(Location location) {
		this.myLocation = gh.encode(location);
		if (!locSent) {
			sendMyLocation();
		}
		cloudLog.Log(this.Android_id + ": "+ "from onMyLocationChange()");


	}

	protected String myLocation;
	protected boolean locSent;
	private static final Geohasher gh = new Geohasher();

	private void sendMyLocation() {
		CloudEntity self = new CloudEntity("Geek");
		self.put("interest", "Cloud");
		self.put("location", this.myLocation);
		// getCloudBackend().update
		getCloudBackend().update(self, new CloudCallbackHandler<CloudEntity>() {
			@Override
			public void onComplete(CloudEntity results) {
				locSent = true;
				drawMyMarker();
			}
		});
		cloudLog.Log(this.Android_id + ": "+ "from sendMyLocation()");

	}

	private void drawMyMarker() {
		if (myLocation == null)
			return;
		float markerColor = BitmapDescriptorFactory.HUE_AZURE;
		mMap.addMarker(new MarkerOptions().position(gh.decode(myLocation))
				.title("UberGeek").snippet(myLocation)
				.icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
		cloudLog.Log(this.Android_id + ": "+ "from drawMyMarker()");


	}

	private void queryGeeks() {
		CloudCallbackHandler<List<CloudEntity>> handler = new CloudCallbackHandler<List<CloudEntity>>() {
			@Override
			public void onComplete(List<CloudEntity> results) {
				drawMarkers(results);
				cloudLog.Log(Android_id + ": "+ "from queryGeeks() -complete ");

			}

			@Override
			public void onError(IOException e) {
				Toast.makeText(getApplicationContext(), e.getMessage(),
						Toast.LENGTH_LONG).show();
				cloudLog.Log(Android_id + ": "+ "from queryGeeks() - error "+ e.getMessage());

			}
		};

		// Remove previous query
		getCloudBackend().clearAllSubscription();

		CloudQuery cq = new CloudQuery("Geek");
		cq.setLimit(50);
		cq.setScope(Scope.FUTURE_AND_PAST);
		getCloudBackend().list(cq, handler);
		cloudLog.Log(this.Android_id + ": "+ "from queryGeeks()");

	}

	protected void drawMarkers(List<CloudEntity> results) {
		mMap.clear();
		for (CloudEntity geek : results) {
			float markerColor = BitmapDescriptorFactory.HUE_RED;
			String locHash = (String) geek.get("location");
			mMap.addMarker(new MarkerOptions()
					.position(gh.decode(locHash))
					.title("Some Geek")
					.snippet(myLocation)
					.icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
		}
		drawMyMarker();
		cloudLog.Log(Android_id + ": "+ "from drawMarkers() ");

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		setUpMapIfNeeded();
		cloudLog.Log(Android_id + ": "+ "from onCreate() ");
	}

	@Override
	protected void onResume() {
		super.onResume();
		setUpMapIfNeeded();
		TextView overlay = (TextView) findViewById(R.id.overlay);
		String username = "";
		if (super.getAccountName() != null) {
			username = super.getAccountName();
		}
		overlay.setText(username);
		queryGeeks();
		cloudLog.Log(Android_id + ": "+ "from onResume() ");

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_map, menu);
		return true;
	}

}
