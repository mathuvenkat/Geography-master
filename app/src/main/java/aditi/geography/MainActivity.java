package aditi.geography;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.content.Intent;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button newGameButton = (Button) this.findViewById(R.id.newGame_button);
        newGameButton.setOnClickListener(this);

        Button exitGameButton = (Button) this.findViewById(R.id.exit_button);
        exitGameButton.setOnClickListener(this);

        Button playgameButton = (Button) this.findViewById(R.id.playgame);
        playgameButton.setOnClickListener(this);


        Button monumentButton = (Button) this.findViewById(R.id.monuments);
        monumentButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.newGame_button:
                Intent intent = new Intent(MainActivity.this , MapsActivity.class);
                startActivity(intent);
                break;


            case R.id.exit_button:
                finish();
                break;

            case R.id.playgame:
                Intent gameintent = new Intent(MainActivity.this , GameActivity.class);
                startActivity(gameintent);
                break;


            case R.id.monuments:
                Intent gameMonumentintent = new Intent(MainActivity.this , MonumentQuizActivity.class);
                startActivity(gameMonumentintent);
                break;

        }

    }
}
