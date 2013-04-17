package com.google.cloud.backend.android.sample.geekwatch;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.cloud.backend.android.CloudBackendActivity;
import com.google.cloud.backend.android.R;

public class GeekwatchActivity extends CloudBackendActivity implements
		OnLayoutChangeListener {

	private GoogleMap mMap;
	private TextView loc;
	private LatLngBounds bounds;
	private LatLng myLocation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_geekwatch);
		loc = (TextView) findViewById(R.id.loc);
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
		final View mapView = getSupportFragmentManager().findFragmentById(
				R.id.map).getView();
		mapView.addOnLayoutChangeListener(this);
		mMap.setOnMyLocationChangeListener(new OnMyLocationChangeListener() {

			@Override
			public void onMyLocationChange(Location location) {
				double lat = location.getLatitude();
				double lon = location.getLongitude();
				myLocation = new LatLng(lat, lon);
				// if current location Geohash has changed more than yyyyyy, requery
				findGeeks();
//				mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation,17f));
				loc.setText("Current loc:\n" + lat + "\n" + lon + "\n"
						+ new Geohash().encode(lat, lon));
			}
		});
	}

	protected void findGeeks() {
		String[] geekLocations = new String[] { "dn5bptz", "dn5bpts" };
		addMarkersToMap(geekLocations);
	}

	private void addMarkersToMap(String[] geekLocations) {
		Geohash decoder = new Geohash();
		mMap.clear();
		// get positions and build map bounds
		List<LatLng> positions = new ArrayList<LatLng>();
		Builder boundsBuilder = new LatLngBounds.Builder();
		if (myLocation != null) {
			boundsBuilder.include(myLocation);
		}
		for (String geohash : geekLocations) {
			double[] latlon = decoder.decode(geohash);
			LatLng pos = new LatLng(latlon[0], latlon[1]);
			positions.add(pos);
			boundsBuilder.include(pos);
			Object geekMarker = mMap.addMarker(new MarkerOptions()
					.position(pos)
					.title("Name")
					.snippet("Interest")
					.icon(BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
		}
		bounds = boundsBuilder.build();
	}

	@Override
	public void onLayoutChange(View v, int left, int top, int right,
			int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
		if (bounds != null) {
			mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 24));
		}
	}

}
