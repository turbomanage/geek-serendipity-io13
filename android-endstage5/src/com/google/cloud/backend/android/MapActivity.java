package com.google.cloud.backend.android;

import java.io.IOException;
import java.util.List;

import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.cloud.backend.android.CloudQuery.Order;
import com.google.cloud.backend.android.CloudQuery.Scope;

public class MapActivity extends CloudBackendActivity implements
		OnMyLocationChangeListener {

	private GoogleMap mMap;

	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the
		// map.
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
	}

	@Override
	public void onMyLocationChange(Location location) {
		this.myLocation = gh.encode(location);
		if (!locSent) {
			sendMyLocation();
		}
	}

	protected String myLocation;
	protected static boolean locSent;
	private static final Geohasher gh = new Geohasher();

	private void sendMyLocation() {
		CloudEntity self = new CloudEntity("Geek");
		self.put("interest", "Cloud");
		self.put("location", this.myLocation);
		getCloudBackend().update(self, new CloudCallbackHandler<CloudEntity>() {
			@Override
			public void onComplete(CloudEntity results) {
				locSent = true;
			}
		});
	}

	private void drawMyMarker() {
		if (myLocation == null)
			return;
		mMap.clear();
		float markerColor = BitmapDescriptorFactory.HUE_AZURE;
		mMap.addMarker(new MarkerOptions().position(gh.decode(myLocation))
				.title("UberGeek").snippet(myLocation)
				.icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
	}

	private void queryGeeks() {
		CloudCallbackHandler<List<CloudEntity>> handler = new CloudCallbackHandler<List<CloudEntity>>() {
			@Override
			public void onComplete(List<CloudEntity> results) {
				drawMarkers(results);
			}

			@Override
			public void onError(IOException e) {
				Toast.makeText(getApplicationContext(), e.getMessage(),
						Toast.LENGTH_LONG).show();
			}
		};

		// Remove previous query
		getCloudBackend().clearAllSubscription();

		CloudQuery cq = new CloudQuery("Geek");
		cq.setLimit(50);
        cq.setSort(CloudEntity.PROP_UPDATED_AT, Order.DESC);
		cq.setScope(Scope.FUTURE_AND_PAST);
		getCloudBackend().list(cq, handler);
	}

	protected void drawMarkers(List<CloudEntity> results) {
	    mMap.clear();
		for (CloudEntity geek : results) {
			float markerColor = BitmapDescriptorFactory.HUE_RED;
			String createdBy = geek.getCreatedBy();
			// mark self in blue
			if (createdBy != null && createdBy.equals(super.getAccountName())) {
			    markerColor = BitmapDescriptorFactory.HUE_AZURE;
			}
			String locHash = (String) geek.get("location");
			mMap.addMarker(new MarkerOptions()
					.position(gh.decode(locHash))
					.title("Some Geek")
					.snippet(myLocation)
					.icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		setUpMapIfNeeded();
	}

	@Override
	protected void onPostCreate() {
	    super.onPostCreate();
	    queryGeeks();
	}

	@Override
	protected void onResume() {
		super.onResume();
		setUpMapIfNeeded();
		TextView overlay = (TextView) findViewById(R.id.overlay);
		String username = "Not signed in";
		if (super.getAccountName() != null) {
			username = super.getAccountName();
		}
		overlay.setText(username);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_map, menu);
		return true;
	}

}
