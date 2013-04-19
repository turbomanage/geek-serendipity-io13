package com.google.cloud.backend.android.sample.geekwatch;

import java.io.IOException;
import java.util.List;

import android.content.SharedPreferences;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.cloud.backend.android.CloudBackendActivity;
import com.google.cloud.backend.android.CloudCallbackHandler;
import com.google.cloud.backend.android.CloudEntity;
import com.google.cloud.backend.android.CloudQuery;
import com.google.cloud.backend.android.CloudQuery.Order;
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
        private String mInterests = "Cloud";
        private static final Geohasher gh = new Geohasher();
        private static final String KEY_CURRENT_LOC = "mCurrentLocation";
        private static final String KEY_ZOOM = "zoom";
        private static final String KEY_GEEK_ID = "geekId";

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
                        sinceWhen.setText(sinceWhen(info[0]));
                        interest.setText(info[1]);
                        return view;
                }

                @Override
                public View getInfoWindow(Marker marker) {
                        // use default window
                        return null;
                }

                private String sinceWhen(String timestamp) {
                        if (timestamp == null) {
                                return "";
                        }
                        long secs = (System.currentTimeMillis() - Long.valueOf(timestamp)) / 1000;
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

        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_geekwatch);
                locText = (TextView) findViewById(R.id.loc);
                // Show the Up button in the action bar.
                getActionBar().setDisplayHomeAsUpEnabled(true);
                startUpdateTimer();
        }

        @Override
        protected void onPause() {
                super.onPause();
                // save current location
                SharedPreferences.Editor ed = getPreferences(MODE_PRIVATE).edit();
                ed.putString(KEY_CURRENT_LOC, gh.encode(mCurrentLocation));
                if (mMap != null) {
                        CameraPosition camPos = mMap.getCameraPosition();
                        ed.putFloat(KEY_ZOOM, camPos.zoom);
                }
                ed.commit();
        }

        @Override
        protected void onResume() {
                super.onResume();
                setUpMapIfNeeded();
                // Show in saved location on unlock, resume
                if (mCurrentLocation != null) {
                        LatLng latLng = new LatLng(mCurrentLocation.getLatitude(),
                                        mCurrentLocation.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                } else {
                        if (mSelf == null) {
                                mSelf = new Geek(super.getAccountName(), mInterests, null);
                        }
                        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                        String locHash = prefs.getString(KEY_CURRENT_LOC, "");
                        double[] latLon = gh.decode(locHash);
                        float zoom = prefs.getFloat(KEY_ZOOM, 16f);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                                        latLon[0], latLon[1]), zoom));
                }
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
                mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());
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
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
                }
                mCurrentLocation = location;
                if (firstGoodFix) {
                        sendMyLocation();
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
                                List<Geek> geeks = Geek.fromEntities(results);
                                addMarkersToMap(geeks);
                        }

                        @Override
                        public void onError(IOException exception) {
                                handleEndpointException(exception);
                        }
                };

                // execute the query with the handler
                CloudQuery cq = new CloudQuery("Geek");
                // cq.setFilter(F.eq("label", "friends"));
           

                 cq.setFilter(F.eq(Geek.KEY_GEOHASH, visibleRegionHash));
                 cq.setLimit(50);
                 cq.setScope(Scope.FUTURE_AND_PAST);
                 getCloudBackend().list(cq, handler);

                
                // TODO Pass visibleRegionHash as a parameter to a standing query
                //getCloudBackend().listByKind("Geek", CloudEntity.PROP_CREATED_AT,
                  //              Order.DESC, 50, Scope.FUTURE_AND_PAST, handler);
        }

        private void addMarkersToMap(List<Geek> geeks) {
                mMap.clear();
                for (Geek geek : geeks) {
                        double[] latlon = gh.decode(geek.getGeohash());
                        LatLng pos = new LatLng(latlon[0], latlon[1]);
                        // choose marker color
                        float markerColor;
                        if (geek.equals(mSelf)) {
                                markerColor = BitmapDescriptorFactory.HUE_AZURE;
                        } else {
                                markerColor = BitmapDescriptorFactory.HUE_RED;
                        }
                        Object geekMarker = mMap.addMarker(new MarkerOptions()
                                        .position(pos)
                                        .title(geek.getName())
                                        .snippet(
                                                        geek.getUpdatedAt().getTime() + ":"
                                                                        + geek.getInterest())
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
                                mSelf = new Geek(result);
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
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                // btSend.setEnabled(true);
        }

}