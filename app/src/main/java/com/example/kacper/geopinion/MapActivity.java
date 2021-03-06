package com.example.kacper.geopinion;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.example.kacper.geopinion.Model.Category;
import com.example.kacper.geopinion.Model.FoursquareSearch;
import com.example.kacper.geopinion.Model.Venue;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.orhanobut.hawk.Hawk;
import retrofit2.Call;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback,GoogleMap.OnMarkerClickListener {
    private List<Marker> markers = new ArrayList<>();
    private DatabaseManager manager= new DatabaseManager(this);
    private LocationManager locationManager,locationManager2;
    private LocationListener locationListener;
    private Location myLocation= new Location(LOCATION_SERVICE);
    private GoogleMap map;
    private String geoLocation = "";
    private List<Venue> item_list = new ArrayList<>();
    private boolean venueFound=false;
    private boolean cameraIsSet=false;
    private int index;
    private boolean locationEstablished =false;
    private boolean snackBarHidden=false;
    private Button button;
    //   if (mGoogleApiClient == null) {

    //         mGoogleApiClient = new GoogleApiClient.Builder(this)

    // //                 .addConnectionCallbacks(this)

    //                  .addOnConnectionFailedListener(this)//                 .addApi(LocationServices.API)

    //                 .build();

    //}

    private Snackbar internetSnackbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ////

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        button=(Button)(findViewById(R.id.expressOpinionButton));
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.i("LOCATION CHANGED","!");
                   Log.i("ACCURACY:",String.valueOf(location.getAccuracy()));
                   if (location.getAccuracy()<1000) {
                       Log.i("ACCURACY GRANTED!","");
                       myLocation= new Location(LOCATION_SERVICE);
                       myLocation.set(location);
                       moveCameraToLocation();
                       locationManager.removeUpdates(locationListener);
                       if (!locationEstablished){
                           locationManager.requestLocationUpdates("gps", 3000, 20, locationListener);
                           locationEstablished =true;
                       }

                       Snackbar mySnackbar= Snackbar.make(findViewById(R.id.map), "Lokalizacja ustalona!",Snackbar.LENGTH_SHORT);
                       mySnackbar.show();
                       startSearchingForVenues();
                       snackBarHidden=false;
                   }
                   else
                   {
                       locationManager.requestLocationUpdates("gps", 100, 1, locationListener);
                       if (!snackBarHidden) {
                           Snackbar mySnackbar = Snackbar.make(findViewById(R.id.map), "Ustalam dokładną lokalizację...", Snackbar.LENGTH_INDEFINITE);
                           mySnackbar.show();
                           snackBarHidden=true;
                       }
                       locationEstablished =false;
                   }
              }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
                makeProviderEnabledSnackbar();
            }

            @Override
            public void onProviderDisabled(String provider) {
                makeProviderDisabledSnackbar();
            }
        };


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //requestPermissions wymaga od nas conajmniej api23. W przeciwnym razie, nie są wymagane dodatkowe uprawnienia.
            Log.i("PERMISSIONS","GO");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.

                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}, 10);
                Log.i("PERMISSIONS","NO LOCATION");

                makeProviderDisabledSnackbar();
                return;
            }


        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {

            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("PERMISSION GRANTED","!");
                    locationManager.requestLocationUpdates("gps", 1000, 10, locationListener);

                }
        }

    }

    
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);

            map=googleMap;
            map.setOnMarkerClickListener(this);
            locationManager.requestLocationUpdates("gps",0,0,locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,locationListener);

            myLocation=locationManager.getLastKnownLocation("gps");

   /*         myLocation=locationManager.getLastKnownLocation("gps");
            if (myLocation!=null && myLocation.getAccuracy()<50){
                moveCameraToLocation();
                startSearchingForVenues();
                locationManager.requestLocationUpdates("gps",3000,20,locationListener);

            }
            else{
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,locationListener);

            }
        */} else {
            Log.i("ERROR","NO PERRMISIONS");
            makeProviderDisabledSnackbar();
        }


    }


    public void onButtonClick(View view) {
        button.setEnabled(false);
        startOpinionActivity(index);

    }



    @Override
    public boolean onMarkerClick(Marker marker) {
        for (int i=0;i<markers.size();i++){
        if (marker.equals(markers.get(i))) {
marker.hideInfoWindow();
            Opinion opinion = new Opinion(Integer.valueOf(Hawk.get("user_id").toString()), item_list.get(i).getId());

            if (!manager.checkIfOpinionExists(opinion)) {
                if (Integer.valueOf(item_list.get(i).getLocation().getDistance()) < 200) {

                    Log.i("INFO: ", "MARKER " + item_list.get(i).getName());
                    Log.i("BUTTON ENABLED: ", String.valueOf(button.isEnabled()));
                    button.setEnabled(true);
                    Log.i("BUTTON ENABLED: ", String.valueOf(button.isEnabled()));

                    button.setText(String.format("Oceń lokal- %s", item_list.get(i).getName()));
                    index = i;
                } else {
                    makeToast(getString(R.string.tooFarAwayFromVenue));
                    button.setText(getString(R.string.tooFarAway));
                    button.setEnabled(false);

                }
            }
            else{
                button.setEnabled(false);
                button.setText(R.string.opinionExists);


            }
        }

        }

        return false;
    }


    private void  makeProviderDisabledSnackbar(){
        Snackbar mySnackbar= Snackbar.make(findViewById(R.id.map), "Aplikacja wymaga włączenia lokalizacji",Snackbar.LENGTH_INDEFINITE);
        mySnackbar.setAction("OK",new CheckLocationListener());
        mySnackbar.show();

    }
    private void makeProviderEnabledSnackbar(){

        Snackbar mySnackbar= Snackbar.make(findViewById(R.id.map),"Ustalam dokładną lokalizację..." ,Snackbar.LENGTH_INDEFINITE);
        mySnackbar.show();

    }
    private void makeInternetDisabledSnackbar(){
        internetSnackbar=Snackbar.make(findViewById(R.id.map),"Wymagane jest połączenie z internetem!",Snackbar.LENGTH_LONG);
        internetSnackbar.show();
        }

private void moveCameraToLocation(){
        if(!cameraIsSet) {
            LatLng location = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
            Log.i("LOCATION", String.valueOf(location));
            LatLng latlng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17));
            cameraIsSet=true;
        }
}





    public class ExploreAsyncTask extends AsyncTask<Void, Void, List<Venue>> {

        ExploreAsyncTask() {
            super();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<Venue> doInBackground(Void... voids) {
            FoursquareIntegration foursquareIntegration = FoursquareIntegration.retrofit.create(FoursquareIntegration.class);
            String client_ID = "CWPUNU2P4XDRSU1ANMXKR2F4UXEDUY1AMOM1DNBGA0HVXORI";
            String client_Secret = "LL5SY2U5G2N0B4GXS244NUHJ43SSN3UCCQX1NP5NXF52HIPV";
            String v = "20171103";

             String limit="50";
             String intent="checkin";
            final Call<FoursquareSearch> call = foursquareIntegration.requestFoursquareModel(client_ID, client_Secret,geoLocation,intent, v,limit);

            try {
               FoursquareSearch explore =  call.execute().body();
          //     FoursquareSearch explore2= call.execute().body();
             //   Log.i("RESPONSE",String.valueOf(explore.getResponse().getGroups()));
//                Log.i("RESPONSE",String.valueOf(explore.getResponse().getGroups().get(0).getItems()));

                  item_list = explore.getResponse().getVenues();
            } catch (IOException e) {
                venueFound=false;

                e.printStackTrace();
                makeInternetDisabledSnackbar();
                Log.i("PROPABLY NO INTERNET","!");

            }
            return item_list;
        }

        @Override
        protected void onPostExecute(List<Venue> item_s) {
            if (venueFound && internetSnackbar!=null){
                internetSnackbar.dismiss();
            }
            super.onPostExecute(item_s); 
                for (int i = 0; i < item_s.size(); i++) {
                     double lat = item_list.get(i).getLocation().getLat();
                    double lng = item_list.get(i).getLocation().getLng();
                    LatLng venueLatLng = new LatLng(lat, lng);

                    Marker marker = map.addMarker(new MarkerOptions().position(venueLatLng).title(item_list.get(i).getName()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    if(item_list.get(i).getCategories().size()==0)
                        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
else                   if (item_list.get(i).getCategories().get(0).getId().equals( "4bf58dd8d48988d16d941735")) {
                       marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    }
 else                   if (item_list.get(i).getCategories().get(0).getId().equals("4bf58dd8d48988d100951735")) {
                        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

                    }
else                    if (item_list.get(i).getCategories().get(0).getId().equals("4bf58dd8d48988d13b941735")) {
                        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));

                    }
    else                if (item_list.get(i).getCategories().get(0).getId().equals("4bf58dd8d48988d1fa931735")) {
                        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));

                    }






                    markers.add(marker);

                }
            //    Log.i("MARKER:" ,markers.get(1).getTitle());

        }

    }
    public class CheckLocationListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            Intent intent= new Intent (Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            if(!locationManager.isProviderEnabled("gps")){
                makeProviderDisabledSnackbar();
            }
        }

    }

    private void startSearchingForVenues(){
    Log.i("METHOD: ","startSearchingForVenues");

         if (!venueFound) {
            geoLocation = myLocation.getLatitude() + "," + myLocation.getLongitude();
            ExploreAsyncTask exploreAsyncTask = new ExploreAsyncTask();
            exploreAsyncTask.execute();
            venueFound=true;

         }

    }

    private void makeToast(String text){
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();

    }
    private void startOpinionActivity(int index){
        Hawk.put("venue_id",item_list.get(index).getId());
        Hawk.put("venue_name",item_list.get(index).getName());
        List<Category> venue_category= item_list.get(index).getCategories();
        StringBuilder categoryString = new StringBuilder();
        for (int i=0;i<venue_category.size();i++){
            if(i==venue_category.size()-1){
                categoryString.append(venue_category.get(i).getName());
            }
            else
                categoryString.append(venue_category.get(i).getName()).append(", ");

        }
        Hawk.put("venue_category", categoryString.toString());
 //       Hawk.put("venue_photo_url",item_list.get(index).getTip().getPhotourl());
        Intent intent = new Intent(this,ExpressOpinionActivity.class);
        startActivity(intent);

            }
}

