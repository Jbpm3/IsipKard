package ph.edu.usc24100596;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

    public class DeckScreenActivity extends AppCompatActivity {

        TextView deckTitle;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.deck_screen);

            deckTitle = findViewById(R.id.deckTitle);

            String deckName = getIntent().getStringExtra("deck_name");
            deckTitle.setText(deckName);
        }
    }
