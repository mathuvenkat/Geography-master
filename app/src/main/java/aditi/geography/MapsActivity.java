package aditi.geography;

import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static String TAG = "MapsActivity";

    private GoogleMap mMap;
    private TextToSpeech tts;
    Marker locationMarker = null;

    Properties properties = new Properties();
    private String urlString = "https://restcountries.eu/rest/v1/all";
    String propFileUSA = "states.properties";
    String currencyCodes = "currencycodes.csv";

    Map<String, String> countryCapitalMap = new HashMap<>();
    Map<String, String> currencyMap = new HashMap<>();
    private String selectedStateOrCountry;
    private String selectedCountry;
    private String selectedCountryCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        init();

        //Same activity is used for the game and explore pages.
        TextView questionText = (TextView) findViewById(R.id.textView2);
        questionText.setVisibility(View.INVISIBLE);

        TextView scoreText = (TextView) findViewById(R.id.score);
        scoreText.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onPause() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (SSLUtils.conn != null) {
            SSLUtils.conn.disconnect();
        }
        super.onStop();
    }

    private void init() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(propFileUSA);
        try {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (Exception e) {
            //Log.e(TAG, "Could not load prop file");
        }

        String line;
        String arr[];
        stream = getClass().getClassLoader().getResourceAsStream(currencyCodes);
        BufferedReader br = null;
        try {
            if (stream != null) {
                br = new BufferedReader(new InputStreamReader(stream));
                while ((line = br.readLine()) != null) {
                    arr = line.split(",");
                    currencyMap.put(arr[0], arr[1]);
                }
            }
        } catch (Exception e) {

        } finally {
            try {
                br.close();
                stream.close();
            } catch (Exception e) {

            }
        }
        new LongRunningGetIO().execute();
    }

    /**
     * Make rest call to api to get countries/capitals and populate map , initialize tts
     */
    private void background() {
        tts = new TextToSpeech(MapsActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.ERROR) {
                    //Log.e(TAG, "Failed to initialize text to speech");
                }
            }
        });

        InputStream inputStream = null;
        try {
            inputStream = SSLUtils.invokeHttpsApi(urlString);
        } catch (Exception e) {
            //Log.e(TAG, "Unable to talk to rest countries api", e);
        }
        try {
            SSLUtils.parseJsonCountriesOutput(inputStream, countryCapitalMap, currencyMap);
        } catch (Exception e) {
            //Log.e(TAG, "unable to parse json output from countries api");
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {

            }
        }
    }


    /*
    Convert text to speech
     */
    private void ConvertTextToSpeech() {
        if (properties.get(selectedStateOrCountry) != null) {
            tts.speak(String.format("This is %s %s", selectedStateOrCountry, properties.get(selectedStateOrCountry)),
                    TextToSpeech.QUEUE_FLUSH, null);

        } else if (countryCapitalMap.get(selectedStateOrCountry) != null) {
            tts.speak(countryCapitalMap.get(selectedStateOrCountry), TextToSpeech.QUEUE_FLUSH, null);
        } else {
            tts.speak(selectedStateOrCountry, TextToSpeech.QUEUE_FLUSH, null);
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        googleMap.setOnMapClickListener(new OnMapClickListener() {

            @Override
            public void onMapClick(LatLng arg0) {

                if (locationMarker != null) {
                    locationMarker.remove();
                }

                LatLng latlongMarker = new LatLng(arg0.latitude, arg0.longitude);
                // TODO Auto-generated method stub
                try {
                    Geocoder geo = new Geocoder(MapsActivity.this, Locale.getDefault());
                    List<Address> add = geo.getFromLocation(arg0.latitude, arg0.longitude, 1);

                    if (add.size() > 0) {
                        selectedCountry = add.get(0).getCountryName();
                        selectedStateOrCountry = selectedCountry;
                        selectedCountryCode = add.get(0).getCountryCode().toLowerCase();
                        //For usa go with states . All other countries - it gives the capital
                        if (selectedCountry.equalsIgnoreCase("United States") ||
                                selectedCountry.equalsIgnoreCase("US")) {
                            selectedStateOrCountry = add.get(0).getAdminArea();
                        }

                        locationMarker = mMap.addMarker(new MarkerOptions().
                                position(latlongMarker).title(selectedCountry));

                        //resize image in Async task
                        new processBitMap().execute();

                        locationMarker.showInfoWindow();
                        ConvertTextToSpeech();
                    }
                } catch (Exception e) {
                    //Log.e(TAG, "Failed to initialize map", e);
                }
            }
        });
        mMap = googleMap;

        // Add a marker in California and move the camera
        LatLng californiaMarker = new LatLng(37, -122);
        locationMarker = mMap.addMarker(new MarkerOptions().position(californiaMarker).title("Click anywhere to get more info"));
        locationMarker.showInfoWindow();
        mMap.moveCamera(CameraUpdateFactory.newLatLng(californiaMarker));
    }

    public void setLocationMarker(BitmapDescriptor icon) {
        locationMarker.setIcon(icon);
        locationMarker.showInfoWindow();
    }


    /**
     * AsyncTask for setting bitmap icon flag on the marker window
     */
    private class processBitMap extends AsyncTask<Void, Void, BitmapDescriptor> {
        protected BitmapDescriptor doInBackground(Void... params) {
            BitmapDescriptor resized = null;
            Bitmap bitmap = null;
            String uri = "@drawable/" + selectedCountry.toLowerCase();

            //for displaying map icon
            if (selectedCountry.contains(" ")) {
                uri = "@drawable/" + selectedCountryCode;
            }
            int imageRes = getResources().getIdentifier(uri, null, getPackageName());

            if (imageRes != 0) {
                bitmap = Utils.decodeSampledBitmapFromResource(getResources(), imageRes, 250, 150);
                resized = BitmapDescriptorFactory.fromBitmap(bitmap);
            }
            return resized;
        }

        protected void onPostExecute(BitmapDescriptor resized) {
            if (resized != null) {
                setLocationMarker(resized);
            }
        }

    }

    /*
    Async Task of connecting to rest api.
     */
    private class LongRunningGetIO extends AsyncTask<Void, Void, String> {
        protected String doInBackground(Void... params) {
            try {
                //Log.d(TAG, "invoking capitals rest api");
                background();
            } catch (Exception e) {
                //Log.e(TAG, "invoking capitals rest api failed", e);
            }
            return null;
        }
    }
}
