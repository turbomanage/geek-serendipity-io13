package com.google.cloud.backend.android.sample.geekwatch;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.cloud.backend.android.CloudBackendActivity;
import com.google.cloud.backend.android.CloudCallbackHandler;
import com.google.cloud.backend.android.CloudEntity;
import com.google.cloud.backend.android.CloudQuery.Order;
import com.google.cloud.backend.android.CloudQuery.Scope;
import com.google.cloud.backend.android.F.Op;
import com.google.cloud.backend.android.R;

public class GeekwatchActivity extends CloudBackendActivity implements
		OnCameraChangeListener, OnMyLocationChangeListener {

	private GoogleMap mMap;
	private TextView locText;
	private String mCurrentRegionHash;
	private Location mCurrentLocation;
	private Location mLastLocation;
	private Geek geek;
	private static final Geohasher gh = new Geohasher();

	public class CustomInfoWindowAdapter implements InfoWindowAdapter {

		@Override
		public View getInfoContents(Marker marker) {
			View view = getLayoutInflater().inflate(
					R.layout.map_info_window_layout, null);
			TextView title = (TextView) view.findViewById(R.id.title);
			TextView interest = (TextView) view.findViewById(R.id.interest);
			TextView sinceWhen = (TextView) view.findViewById(R.id.sinceWhen);
			String[] info = marker.getSnippet().split(":");
			title.setText(marker.getTitle());
			sinceWhen.setText(info[0]);
			interest.setText(info[1]);
			return view;
		}

		@Override
		public View getInfoWindow(Marker marker) {
			// use default window
			return null;
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_geekwatch);
		locText = (TextView) findViewById(R.id.loc);
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setUpMapIfNeeded();
		startUpdateTimer();
	}

	/**
	 * Starts the timer to send location every 2 min
	 */
	private void startUpdateTimer() {
		final Handler handler = new Handler();
		handler.post(new Runnable() {
			@Override
			public void run() {
				sendMyLocation();
				handler.postDelayed(this, 120000); // 2 min
			}
		});
	}

	/**
	 * Send my location to server if we've moved >30m
	 */
	private void sendMyLocation() {
		if (mCurrentLocation != null) {
			if (mLastLocation == null
					|| mLastLocation.distanceTo(mCurrentLocation) > 30.) {
				sendMyLocation(mCurrentLocation);
			}
		}
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
		mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());
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
		locText.setText("Current loc:\n" + lat + "\n" + lon + "\n"
				+ gh.encode(lat, lon));
		mCurrentLocation = location;
		// Recenter map only if location has changed > 100m
		// since camera move triggers a query
		if (mLastLocation == null
				|| mLastLocation.distanceTo(location) > 100.) {
			LatLng myLocation = new LatLng(lat, lon);
			// center map on new location
			mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
		}
	}

	private void findGeeks(LatLngBounds visibleBounds) {
		String visibleRegionHash = gh.findHashForRegion(visibleBounds);
		// We've moved or the visible region has changed
		if (!visibleRegionHash.equals(mCurrentRegionHash)) {
			mCurrentRegionHash = visibleRegionHash;
			Toast.makeText(this, visibleRegionHash, Toast.LENGTH_LONG).show();
			queryGeeksWithin(visibleRegionHash);
		}
	}

	private void queryGeeksWithin(String visibleRegionHash) {
		CloudCallbackHandler<List<CloudEntity>> handler = new CloudCallbackHandler<List<CloudEntity>>() {
			@Override
			public void onComplete(List<CloudEntity> results) {
				List<Geek> geeks = Geek.fromEntities(results);
				addMarkersToMap(geeks);
			}

			@Override
			public void onError(IOException exception) {
				handleEndpointException(exception);
			}
		};

		// execute the query with the handler
		// TODO Pass visibleRegionHash as a parameter to a standing query
		getCloudBackend().listByKind("Geek", CloudEntity.PROP_CREATED_AT,
				Order.DESC, 50, Scope.FUTURE_AND_PAST, handler);
	}

	private void addMarkersToMap(List<Geek> geeks) {
		mMap.clear();
		for (Geek geek : geeks) {
			double[] latlon = gh.decode(geek.getGeohash());
			LatLng pos = new LatLng(latlon[0], latlon[1]);
			Object geekMarker = mMap.addMarker(new MarkerOptions()
					.position(pos)
					.title(geek.getName())
					.snippet(sinceWhen(geek) + ":" + geek.getInterest())
					.icon(BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
		}
	}

	private String sinceWhen(Geek geek) {
		Date updatedAt = geek.getUpdatedAt();
		if (updatedAt == null) {
			return "";
		}
		long secs = (System.currentTimeMillis() - updatedAt.getTime()) / 1000;
		if (secs < 60) {
			return secs + " sec ago";
		} else if (secs < 3600) {
			return (secs / 60) + " min ago";
		} else if (secs < 3600 * 24) {
			return (secs / 3600) + "hrs ago";
		} else {
			return (secs / 3600 / 24) + "days ago";
		}
	}

	@Override
	public void onCameraChange(CameraPosition position) {
		LatLngBounds visibleBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
		findGeeks(visibleBounds);
	}

	private void sendMyLocation(final Location loc) {
		final double lat = loc.getLatitude();
		final double lon = loc.getLongitude();
		// create a CloudEntity with the new post
		final Geek newGeek = new Geek(super.getAccountName(), "Cloud",
				gh.encode(lat, lon));
		// create a response handler that will receive the result or an error
		final CloudCallbackHandler<CloudEntity> handler = new CloudCallbackHandler<CloudEntity>() {
			@Override
			public void onComplete(final CloudEntity result) {
				geek = new Geek(result);
				// Update mLastLocation only after success so timer will keep
				// trying otherwise
				mLastLocation = loc;
			}

			@Override
			public void onError(final IOException exception) {
				handleEndpointException(exception);
			}
		};

		// execute the insertion with the handler
		// query for existing username before inserting
		if (geek == null) {
			getCloudBackend().listByProperty("Geek", "name", Op.EQ,
					super.getAccountName(), null, 1, Scope.PAST,
					new CloudCallbackHandler<List<CloudEntity>>() {
						@Override
						public void onComplete(List<CloudEntity> results) {
							if (results.size() > 0) {
								geek = new Geek(results.get(0));
								geek.setGeohash(gh.encode(lat, lon));
								getCloudBackend().update(geek.asEntity(),
										handler);
							} else {
								getCloudBackend().insert(newGeek.asEntity(),
										handler);
							}
						}
					});
		} else {
			geek.setGeohash(gh.encode(lat, lon));
			getCloudBackend().update(geek.asEntity(), handler);
		}
	}

	private void handleEndpointException(IOException e) {
		Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
		// btSend.setEnabled(true);
	}

}
