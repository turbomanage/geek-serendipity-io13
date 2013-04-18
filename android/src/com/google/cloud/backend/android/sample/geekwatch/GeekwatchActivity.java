package com.google.cloud.backend.android.sample.geekwatch;

import java.io.IOException;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.cloud.backend.android.CloudBackendActivity;
import com.google.cloud.backend.android.CloudCallbackHandler;
import com.google.cloud.backend.android.CloudEntity;
import com.google.cloud.backend.android.R;

public class GeekwatchActivity extends CloudBackendActivity implements
		OnCameraChangeListener, OnMyLocationChangeListener {

	private GoogleMap mMap;
	private TextView locText;
	private String mCurrentRegionHash;
	private Location mCurrentLocation;
	private CloudEntity geek;
	private static final Geohasher gh = new Geohasher();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_geekwatch);
		locText = (TextView) findViewById(R.id.loc);
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setUpMapIfNeeded();
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

	@Override
	protected void onResume() {
		super.onResume();
		setUpMapIfNeeded();
	}

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
		// Hide the zoom controls as the button panel will cover it.
		mMap.getUiSettings().setCompassEnabled(true);
		mMap.setMyLocationEnabled(true);
		mMap.setOnCameraChangeListener(this);
		mMap.setOnMyLocationChangeListener(this);
		// set zoom only on init
		mMap.moveCamera(CameraUpdateFactory.zoomTo(16f));
	}

	@Override
	public void onMyLocationChange(Location location) {
		double lat = location.getLatitude();
		double lon = location.getLongitude();
		// Recenter map only if location has changed > 30m
		if (mCurrentLocation == null
				|| location.distanceTo(mCurrentLocation) > 30.) {
			mCurrentLocation = location;
			LatLng myLocation = new LatLng(lat, lon);
			// center map on new location
			mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
			sendGeekLocation(lat, lon);
		}
		locText.setText("Current loc:\n" + lat + "\n" + lon + "\n"
				+ gh.encode(lat, lon));
	}

	protected void findGeeks(LatLngBounds visibleBounds) {
		String visibleRegionHash = findHashForRegion(visibleBounds);
		// We've moved or the visible region has changed
		if (!visibleRegionHash.equals(mCurrentRegionHash)) {
			mCurrentRegionHash = visibleRegionHash;
			Toast.makeText(this, visibleRegionHash, Toast.LENGTH_LONG).show();
//			queryGeeksWithin(visibleRegionHash);
			String[] geekLocations = new String[] { "dn5bptz", "dn5bpts" };
			addMarkersToMap(geekLocations);
		}
	}

	protected String findHashForRegion(LatLngBounds visibleBounds) {
		double leftLon = visibleBounds.southwest.longitude;
		double rightLon = visibleBounds.northeast.longitude;
		double topLat = visibleBounds.northeast.latitude;
		double bottomLat = visibleBounds.southwest.latitude;
		String[] hashedBounds = new String[4];
		hashedBounds[0] = gh.encode(topLat, leftLon);
		hashedBounds[1] = gh.encode(topLat, rightLon);
		hashedBounds[2] = gh.encode(bottomLat, leftLon);
		hashedBounds[3] = gh.encode(bottomLat, rightLon);
		return findCommonGeoPrefix(hashedBounds);
	}

	protected String findCommonGeoPrefix(String[] hashes) {
		for (int pos = 0; pos < hashes[0].length(); pos++) {
			for (String hash : hashes) {
				if (hash.charAt(pos) != hashes[0].charAt(pos)) {
					return hashes[0].substring(0, pos);
				}
			}
		}
		return hashes[0];
	}

	private void addMarkersToMap(String[] geekLocations) {
		mMap.clear();
		// get positions and build map bounds
		for (String geohash : geekLocations) {
			double[] latlon = gh.decode(geohash);
			LatLng pos = new LatLng(latlon[0], latlon[1]);
			Object geekMarker = mMap.addMarker(new MarkerOptions()
					.position(pos)
					.title("Name")
					.snippet("Interest")
					.icon(BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
		}
	}

	@Override
	public void onCameraChange(CameraPosition position) {
		LatLngBounds visibleBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
		findGeeks(visibleBounds);
	}

	private void sendGeekLocation(double lat, double lon) {
		// create a CloudEntity with the new post
		CloudEntity newGeek = new CloudEntity("Geek");
		newGeek.put("name", "Brad");
		newGeek.put("interest", "Cloud");
		newGeek.put("location", gh.encode(lat, lon));
		// create a response handler that will receive the result or an error
		CloudCallbackHandler<CloudEntity> handler = new CloudCallbackHandler<CloudEntity>() {
			@Override
			public void onComplete(final CloudEntity result) {
				geek = result;
				Toast.makeText(GeekwatchActivity.this, "location sent",
						Toast.LENGTH_LONG).show();
			}

			@Override
			public void onError(final IOException exception) {
				handleEndpointException(exception);
			}
		};

		// execute the insertion with the handler
		// TODO query for existing username before inserting
		if (geek == null) {
			getCloudBackend().insert(newGeek, handler);
		} else {
			geek.put("location", gh.encode(lat, lon));
			getCloudBackend().update(geek, handler);
		}
	}

	private void handleEndpointException(IOException e) {
		Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
		// btSend.setEnabled(true);
	}

}
