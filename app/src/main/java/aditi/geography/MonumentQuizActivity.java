package aditi.geography;

/**
 * Created by mvenkatesan on 11/22/16.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
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


public class MonumentQuizActivity extends FragmentActivity implements OnMapReadyCallback {

    private static String TAG = "GameActivity";

    private GoogleMap mMap;
    private TextToSpeech tts;
    private Marker locationMarker = null;
    private static Set<String> countryNamesForQuiz;
    Iterator<String> setIter;
    private static Map<String, LatLong> monumentNameToDetailsMap = new HashMap<>();
    ImageView imageView = null;

    String selectedMonument;

    String selectedCountryCode;
    String selectedCountry;
    String latLongCountryCode = "monuments.csv";
    int score;
    int numQuestions;
    private static int numClicksPerQuestion;


    TextView questionText;
    TextView scoreText;
    private static String questionString = "Where is the ";
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("MonumentQuiz Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    private class LatLong {
        Double lat;
        Double lon;
        String nameOfCountry;
        String countryCode;

        public LatLong(Double lat, Double lon, String name, String countryCode) {
            this.lat = lat;
            this.lon = lon;
            this.nameOfCountry = name;
            this.countryCode = countryCode;
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
        questionText.setTextColor(Color.BLACK);

        scoreText = (TextView) findViewById(R.id.score);
        scoreText.setVisibility(View.VISIBLE);
        scoreText.setTextColor(Color.BLACK);

        imageView = (ImageView) findViewById(R.id.monument_img);
        imageView.setVisibility(View.VISIBLE);

        init();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void setQuestionText(String question) {
        questionText.setText(questionString + question);
        scoreText.setText(score + "/" + numQuestions);

        if (locationMarker != null) {
            locationMarker.setVisible(false);
        }
        new processBitMap().execute();
        numQuestions++;
    }


    /**
     * AsyncTask for setting bitmap icon monument on the image window
     */
    private class processBitMap extends AsyncTask<Void, Void, Bitmap> {
        protected Bitmap doInBackground(Void... params) {
            BitmapDescriptor resized = null;
            Bitmap bitmap = null;
            String formattedSelection = selectedMonument.replaceAll(" ", "");
            String uri = "@drawable/" + formattedSelection.toLowerCase();

            int imageRes = getResources().getIdentifier(uri, null, getPackageName());
            if (imageRes != 0) {
                bitmap = Utils.decodeSampledBitmapFromResource(getResources(), imageRes, 250, 150);
            }
            return bitmap;
        }

        protected void onPostExecute(Bitmap resized) {
            if (resized != null) {
                setMonumentImage(resized);
            }
        }
    }

    private void setMonumentImage(Bitmap resized) {
        //recycle previous image
        BitmapDrawable currentImg = (BitmapDrawable) imageView.getDrawable();
        if (currentImg != null) {
            Bitmap current = currentImg.getBitmap();
            if(current != null){
                current.recycle();
            }
        }
        imageView.setImageBitmap(resized);
    }

    private void init() {
        InputStream stream;
        tts = new TextToSpeech(MonumentQuizActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.ERROR) {
                    Log.e(TAG, "Failed to initialize text to speech");
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
                    try {
                        monumentNameToDetailsMap.put(arr[4], new LatLong(Double.parseDouble(arr[1]),
                                Double.parseDouble(arr[2]),
                                arr[3], arr[0]));
                    } catch (Exception e) {
                        continue;
                    }
                }
                countryNamesForQuiz = monumentNameToDetailsMap.keySet();
                setIter = countryNamesForQuiz.iterator();

                String next = setIter.next();
                selectedMonument = next;
                setQuestionText(next);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            try {
                br.close();
                stream.close();
            } catch (Exception e) {

            }
        }
    }


    @Override
    protected void onResume(){
        tts = new TextToSpeech(MonumentQuizActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.ERROR) {
                    //Log.e(TAG, "Failed to initialize text to speech");
                }
            }
        });
        super.onResume();
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
        super.onStop();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
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
        LatLong expectedDetails = monumentNameToDetailsMap.get(selectedMonument);
        String expectedCountryCode = expectedDetails.countryCode;

        if (selectedCountryCode.equals(expectedCountryCode)) {
            locationMarker.setTitle(selectedMonument + " is in " +
                    expectedDetails.nameOfCountry);
            locationMarker.showInfoWindow();
            playMusic(this, R.raw.correct);
            if (!setIter.hasNext()) {
                setIter = countryNamesForQuiz.iterator();
            }
            score++;
            if (score % 5 == 0) {
                playMusic(this, R.raw.cheering);
            }
            numClicksPerQuestion = 0;

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    String text = setIter.next();
                    setQuestionText(text);
                    selectedMonument = text;
                }
            }, 1000);

        } else {
            playMusic(this, R.raw.wrong);
            Log.d("NumChances", Integer.toString(numClicksPerQuestion));
            if (numClicksPerQuestion >= 5) {
                //show the answer and go to next question
                tts.speak("Sorry no more guesses for this question", TextToSpeech.QUEUE_FLUSH,
                        null);
                LatLng latlongMarker = new LatLng(expectedDetails.lat,
                        expectedDetails.lon);
                locationMarker.remove();
                locationMarker = mMap.addMarker(new MarkerOptions().
                        position(latlongMarker).title(selectedMonument + " is in "
                        + expectedDetails.nameOfCountry));
                locationMarker.showInfoWindow();
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latlongMarker));
                if (!setIter.hasNext()) {
                    setIter = countryNamesForQuiz.iterator();
                }

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        String text = setIter.next();
                        setQuestionText(text);
                        selectedMonument = text;
                    }
                }, 3000);
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
                    Geocoder geo = new Geocoder(MonumentQuizActivity.this, Locale.getDefault());
                    List<Address> add = geo.getFromLocation(arg0.latitude, arg0.longitude, 1);
                    if (add.size() > 0) {
                        selectedCountry = add.get(0).getCountryName();
                        selectedCountryCode = add.get(0).getCountryCode();
                        locationMarker = mMap.addMarker(new MarkerOptions().
                                position(latlongMarker).title(selectedCountry));

                        locationMarker.setVisible(true);
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

