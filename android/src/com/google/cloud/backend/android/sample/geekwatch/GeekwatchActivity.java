package com.google.cloud.backend.android.sample.geekwatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.cloud.backend.android.CloudQuery;
import com.google.cloud.backend.android.CloudQuery.Scope;
import com.google.cloud.backend.android.F;
import com.google.cloud.backend.android.F.Op;
import com.google.cloud.backend.android.R;

public class GeekwatchActivity extends CloudBackendActivity implements
                OnCameraChangeListener, OnMyLocationChangeListener {

        private GoogleMap mMap;
        private TextView locText;
        private String mCurrentRegionHash;
        private Location mCurrentLocation;
        private Location mLastLocation;
        private Geek mSelf;
        // Indicates that we're still waiting for an accurate location fix
        private boolean mWaitingForLoc = true;
        // TODO create UI for declaring interests
        private String mInterests = "Android";
        private static final Geohasher gh = new Geohasher();
        private static final String KEY_CURRENT_LOC = "mCurrentLocation";
        private static final String KEY_ZOOM = "zoom";
		private List<Geek> mGeeks = new ArrayList<Geek>();

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_geekwatch);
                locText = (TextView) findViewById(R.id.loc);
        }

        @Override
        protected void onPause() {
                super.onPause();
                // save current location
                SharedPreferences.Editor ed = getPreferences(MODE_PRIVATE).edit();
                if (mMap != null) {
                        CameraPosition camPos = mMap.getCameraPosition();
                        ed.putString(KEY_CURRENT_LOC, gh.encode(camPos.target));
                        ed.putFloat(KEY_ZOOM, camPos.zoom);
                }
                ed.commit();
        }

		@Override
		protected void onResume() {
			super.onResume();
			setUpMapIfNeeded();
			if (mSelf == null) {
				mSelf = new Geek(super.getAccountName(), mInterests, null);
			}
			SharedPreferences prefs = getPreferences(MODE_PRIVATE);
			String locHash = prefs.getString(KEY_CURRENT_LOC, "9q8yy");
			LatLng camPos = gh.decode(locHash);
			float zoom = prefs.getFloat(KEY_ZOOM, 16f);
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(camPos, zoom));
			startUpdateTimer();
			this.mWaitingForLoc = true;
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
                mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(getLayoutInflater()));
                mMap.getUiSettings().setCompassEnabled(true);
                mMap.setMyLocationEnabled(true);
                mMap.setOnCameraChangeListener(this);
                mMap.setOnMyLocationChangeListener(this);
        }

        @Override
        public void onMyLocationChange(Location location) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                locText.setText("Current loc:\n" + lat + "\n" + lon + "\n"
                                + gh.encode(lat, lon));
                // on start or first reliable fix, center the map
                boolean firstGoodFix = mWaitingForLoc && location.getAccuracy() < 30.;
                if (mCurrentLocation == null || firstGoodFix) {
                        LatLng myLocation = new LatLng(lat, lon);
                        // center map on new location
                        // TODO animate vs. move?
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(myLocation));
                }
                mCurrentLocation = location;
                if (firstGoodFix) {
                        sendMyLocation(location);
                        mWaitingForLoc = false;
                }
        }

        @Override
        public void onCameraChange(CameraPosition position) {
                LatLngBounds visibleBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                findGeeks(visibleBounds);
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
                                mGeeks = Geek.fromEntities(results);
                                drawMarkers();
                        }

                        @Override
                        public void onError(IOException exception) {
                                handleEndpointException(exception);
                        }
                };

                // Remove previous query
            		getCloudBackend().clearAllSubscription();
                // execute the query with the handler
                    // TODO this doesn't work as a standing query

            	CloudQuery cq = new CloudQuery("Geek");
                cq.setLimit(50);
                cq.setScope(Scope.FUTURE_AND_PAST);
                getCloudBackend().list(cq, handler);
        }

        private void drawMarkers() {
                mMap.clear();
                for (Geek geek : mGeeks) {
                        LatLng pos = gh.decode(geek.getGeohash());
                        // choose marker color
                        float markerColor;
                        String title;
                        if (geek.equals(mSelf)) {
                        		markerColor = BitmapDescriptorFactory.HUE_AZURE;
                        		title = "Ubergeek";
                        } else {
                        		markerColor = BitmapDescriptorFactory.HUE_RED;
                        		title = geek.getInterest() + " Geek";
                        }
                        mMap.addMarker(new MarkerOptions()
                                        .position(pos)
                                        .title(title)
                                        .snippet("" + geek.getUpdatedAt().getTime())
                                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
                }
        }

		/**
         * Send location to server if we've moved >30m
         */
        void sendMyLocation() {
                if (mCurrentLocation != null) {
                        if (mLastLocation == null
                                        || mLastLocation.distanceTo(mCurrentLocation) > 30.) {
                                sendMyLocation(mCurrentLocation);
                        }
                }
        }

        void sendMyLocation(final Location loc) {
                final double lat = loc.getLatitude();
                final double lon = loc.getLongitude();
                // create a CloudEntity with the new post
                // create a response handler that will receive the result or an error
                final CloudCallbackHandler<CloudEntity> handler = new CloudCallbackHandler<CloudEntity>() {
                        @Override
                        public void onComplete(final CloudEntity result) {
                                // Update mLastLocation only after success so timer will keep
                                // trying otherwise
                                mLastLocation = loc;
                                mSelf = new Geek(result);
                                mGeeks.remove(mSelf);
                                mGeeks.add(mSelf);
                                drawMarkers();
                        }

                        @Override
                        public void onError(final IOException exception) {
                                handleEndpointException(exception);
                        }
                };

                // execute the insertion with the handler
                // query for existing username before inserting
                if (mSelf == null || mSelf.asEntity().getId() == null) {
                        getCloudBackend().listByProperty("Geek", "name", Op.EQ,
                                        super.getAccountName(), null, 1, Scope.PAST,
                                        new CloudCallbackHandler<List<CloudEntity>>() {
                                                @Override
                                                public void onComplete(List<CloudEntity> results) {
                                                        if (results.size() > 0) {
                                                                mSelf = new Geek(results.get(0));
                                                                mSelf.setGeohash(gh.encode(lat, lon));
                                                                getCloudBackend().update(mSelf.asEntity(),
                                                                                handler);
                                                        } else {
                                                                final Geek newGeek = new Geek(
                                                                                GeekwatchActivity.super
                                                                                                .getAccountName(),
                                                                                                "Cloud",
                                                                                                gh.encode(lat, lon));
                                                                getCloudBackend().insert(newGeek.asEntity(),
                                                                                handler);
                                                        }
                                                }
                                        });
                } else {
                        mSelf.setGeohash(gh.encode(lat, lon));
                        getCloudBackend().update(mSelf.asEntity(), handler);
                }
        }

        private void handleEndpointException(IOException e) {
//        		if (e instanceof UserRecoverableAuthIOException) {
//        			UserRecoverableAuthIOException authE = (UserRecoverableAuthIOException) e;
//        			startActivityForResult(authE.getIntent(), 2);
//        		}
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        }

}