package aditi.geography;

import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class GameActivity extends FragmentActivity implements OnMapReadyCallback {

    private static String TAG = "GameActivity";

    private GoogleMap mMap;
    private TextToSpeech tts;
    private Marker locationMarker = null;
    private static Set<String> countryNamesForQuiz;
    Iterator<String> setIter;
    private static Map<String, LatLong> countryCodetoDetails = new HashMap<>();
    private static Map<String, String> countryNamesToCodes = new HashMap<>();


    String selectedCountryCode;
    String selectedCountry;
    String latLongCountryCode = "latlongcountry.csv";
    int score;
    int numQuestions;
    private static int numClicksPerQuestion;


    TextView questionText;
    TextView scoreText;
    private static String questionString = "Locate ";

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
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        numClicksPerQuestion = 0;
        questionText = (TextView) findViewById(R.id.textView2);
        questionText.setVisibility(View.VISIBLE);
        questionText.getLayoutParams().height = 200;
        questionText.setTextColor(Color.BLACK);
        questionText.setTextSize(questionText.getTextSize() + 15);

        scoreText = (TextView) findViewById(R.id.score);
        scoreText.setVisibility(View.VISIBLE);
        scoreText.getLayoutParams().height = 75;
        scoreText.setTextColor(Color.BLACK);
        scoreText.setTextSize(scoreText.getTextSize() + 5);

        init();
    }

    private void setQuestionText(String question) {
        questionText.setText(questionString + question);
        scoreText.setText(score + "/" + numQuestions);
        numQuestions++;
    }

    private void init() {

        InputStream stream;
        tts = new TextToSpeech(GameActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.ERROR) {
                    //Log.e(TAG, "Failed to initialize text to speech");
                }
            }
        });


        String line;
        String arr[];
        stream = getClass().getClassLoader().getResourceAsStream(latLongCountryCode);
        BufferedReader br = null;
        try {
            if (stream != null) {
                 br = new BufferedReader(new InputStreamReader(stream));
                while ((line = br.readLine()) != null) {
                    arr = line.split(",");
                    countryCodetoDetails.put(arr[0], new LatLong(Double.parseDouble(arr[1]),
                            Double.parseDouble(arr[2]),
                            arr[3]));
                    countryNamesToCodes.put(arr[3], arr[0]);
                }

                countryNamesForQuiz = countryNamesToCodes.keySet();
                setIter = countryNamesForQuiz.iterator();

                String next = setIter.next();
                tts.speak(next, TextToSpeech.QUEUE_FLUSH, null);
                setQuestionText(next);
            }
        } catch (Exception e) {
        } finally {
            try {
                br.close();
                stream.close();
            } catch (Exception e) {

            }
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
        String tmp[] = question.split(questionString);
        String expectedCountryCode = countryNamesToCodes.get(tmp[1]);

        Log.d("Expected code", expectedCountryCode);
        Log.d("Expected name", tmp[1]);
        Log.d("selected code", selectedCountryCode);


        if (selectedCountryCode.equals(expectedCountryCode)) {
            playMusic(this, R.raw.correct);
            if (!setIter.hasNext()) {
                setIter = countryNamesForQuiz.iterator();
            }
            score++;
            if (score % 5 == 0) {
                playMusic(this, R.raw.cheering);
            }
            setQuestionText(setIter.next());
            numClicksPerQuestion = 0;
        } else {
            playMusic(this, R.raw.wrong);
            if (numClicksPerQuestion >= 5) {
                //show the answer and go to next question
                tts.speak("Sorry no more guesses for this question", TextToSpeech.QUEUE_FLUSH, null);
                LatLng latlongMarker = new LatLng(countryCodetoDetails.get(expectedCountryCode).lat,
                        countryCodetoDetails.get(expectedCountryCode).lon);
                locationMarker.remove();
                locationMarker = mMap.addMarker(new MarkerOptions().
                        position(latlongMarker).title("This is " + countryCodetoDetails.get(expectedCountryCode).name));
                locationMarker.showInfoWindow();
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latlongMarker));
                if (!setIter.hasNext()) {
                    setIter = countryNamesForQuiz.iterator();
                }
                setQuestionText(setIter.next());
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
