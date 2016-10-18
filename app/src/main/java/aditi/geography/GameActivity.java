package aditi.geography;

import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


public class GameActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    TextToSpeech tts;
    Properties properties = new Properties();
    Marker locationMarker = null;

    int score;


    String selectedCountryCode;
    String selectedCountry;
    String propFile = "questions.properties";
    String latLongCountryCode = "latlongcountry.csv";

    public static List<String> list = new ArrayList<String>();
    public static int listIndex = 0;

    private static String TAG = "GameActivity";

    private static int numClicksPerQuestion = 0;
    private static Map<String, LatLong> map = new HashMap<>();


    private class LatLong {
        Double lat;
        Double lon;
        String name;

        public LatLong(Double lat, Double lon, String name) {
            this.lat = lat;
            this.lon = lon;
            this.name = name;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        init();
    }

    private void init() {
        new LongRunningGetIO().execute();
    }

    private void initBackgroundTask() {
        //init tts
        tts = new TextToSpeech(GameActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.ERROR) {
                    //Log.e(TAG, "Failed to initialize text to speech");
                }
            }
        });

        InputStream stream;
        stream = getClass().getClassLoader().getResourceAsStream(propFile);
        try {
            if (stream != null) {
                properties.load(stream);
                for (Object obj : properties.keySet()) {
                    list.add("Locate " + (String) obj + " on the map");
                }
            }
        } catch (Exception e) {
            //Log.e(TAG, "Could not load prop file");
        }
        TextView questionText = (TextView) findViewById(R.id.textView2);
        questionText.setVisibility(View.VISIBLE);
        questionText.getLayoutParams().height = 200;
        questionText.setTextColor(Color.BLACK);
        questionText.setTextSize(questionText.getTextSize() + 15);
        numClicksPerQuestion = 0;

        questionText.setText(list.get(listIndex));
        tts.speak(list.get(listIndex), TextToSpeech.QUEUE_FLUSH, null);


        String line;
        String arr[];
        stream = getClass().getClassLoader().getResourceAsStream(latLongCountryCode);
        try {
            if (stream != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(stream));
                while ((line = br.readLine()) != null) {
                    arr = line.split(",");
                    map.put(arr[0], new LatLong(Double.parseDouble(arr[1]),
                            Double.parseDouble(arr[2]),
                            arr[3]));
                }
            }

        } catch (Exception e) {
        }
    }

    /*
    Async Task of connecting to rest api.
     */
    private class LongRunningGetIO extends AsyncTask<Void, Void, String> {
        protected String doInBackground(Void... params) {
            try {

                initBackgroundTask();
            } catch (Exception e) {
                //Log.e(TAG, "invoking capitals rest api failed", e);
            }
            return null;
        }
    }

    @Override
    protected void onPause() {
        //Log.d(TAG, "Invoking onPause");
        Music.stop(this);
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onPause();

    }

    @Override
    protected void onStop() {
        Music.stop(this);
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onStop();
    }

    /**
     * Play music in thread
     *
     * @param context
     * @param resource
     */
    public void playMusic(final Context context, final int resource) {
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              Music.play(context, resource);

                          }
                      }
        );
    }

    /*
    Convert text to speech
     */
    private void ConvertTextToSpeech() {
        TextView questionText = (TextView) findViewById(R.id.textView2);
        String question = (String) questionText.getText();
        String comparisonString[] = question.split(" ");
        String mappedCountryCode = (String) properties.get(comparisonString[1]);

        if (selectedCountryCode.equals(mappedCountryCode)) {
            playMusic(this, R.raw.correct);


            if (listIndex + 1 < list.size()) {
                listIndex++;
            } else {
                listIndex = 0;
            }
            score++;
            if (score % 5 == 0) {

                playMusic(this, R.raw.cheering);
            }
            questionText.setText(list.get(listIndex));
            numClicksPerQuestion = 0;
        } else {
            playMusic(this, R.raw.wrong);
            if (numClicksPerQuestion >= 5) {
                //show the answer and go to next question
                tts.speak("Sorry no more guesses for this question", TextToSpeech.QUEUE_FLUSH, null);
                LatLng latlongMarker = new LatLng(map.get(mappedCountryCode).lat,
                        map.get(mappedCountryCode).lon);
                locationMarker.remove();
                locationMarker = mMap.addMarker(new MarkerOptions().
                        position(latlongMarker).title("This is " + map.get(mappedCountryCode).name));
                locationMarker.showInfoWindow();
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latlongMarker));

                if (listIndex + 1 < list.size()) {
                    listIndex++;
                } else {
                    listIndex = 0;
                }
                questionText.setText(list.get(listIndex));
                numClicksPerQuestion = 0;

            } else {
                numClicksPerQuestion++;
            }
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

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng arg0) {

                if (locationMarker != null) {
                    locationMarker.remove();
                }
                LatLng latlongMarker = new LatLng(arg0.latitude, arg0.longitude);
                try {
                    Geocoder geo = new Geocoder(GameActivity.this, Locale.getDefault());
                    List<Address> add = geo.getFromLocation(arg0.latitude, arg0.longitude, 1);


                    if (add.size() > 0) {
                        selectedCountry = add.get(0).getCountryName();
                        selectedCountryCode = add.get(0).getCountryCode();
                        locationMarker = mMap.addMarker(new MarkerOptions().
                                position(latlongMarker).title(selectedCountry));


                        locationMarker.showInfoWindow();
                        ConvertTextToSpeech();
                    }
                } catch (Exception e) {
                }
            }
        });
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    }
}
