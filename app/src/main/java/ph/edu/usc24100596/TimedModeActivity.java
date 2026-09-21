package ph.edu.usc24100596;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import android.widget.EditText;
import android.text.InputType;

import androidx.appcompat.app.AppCompatActivity;

public class TimedModeActivity extends AppCompatActivity {

    TextView option5, option10, option30, optionCustom;
    ImageButton btnBack;

    // Receive selected category and deck from previous screen
    String categoryName;
    String deckName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timed_mode);

        btnBack = findViewById(R.id.btnBack);
        option5 = findViewById(R.id.option5sec);
        option10 = findViewById(R.id.option10sec);
        option30 = findViewById(R.id.option30sec);
        optionCustom = findViewById(R.id.optionCustom);

        // Get actual selected category and deck from Intent extras
        categoryName = getIntent().getStringExtra("categoryName");
        deckName = getIntent().getStringExtra("deckName");

        btnBack.setOnClickListener(v -> finish());

        option5.setOnClickListener(v -> startStudy(5));
        option10.setOnClickListener(v -> startStudy(10));
        option30.setOnClickListener(v -> startStudy(30));
        optionCustom.setOnClickListener(v -> showCustomTimeDialog());
    }

    private void startStudy(int seconds) {
        if (categoryName == null || deckName == null) {
            Toast.makeText(this, "Deck not specified!", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, TimedStudyActivity.class);
        intent.putExtra("categoryName", categoryName);
        intent.putExtra("deckName", deckName);
        intent.putExtra("seconds", seconds);
        intent.putExtra("confirmRating", getIntent().getBooleanExtra("confirmRating", false));
        startActivity(intent);
    }

    private void showCustomTimeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Custom Time");

        EditText input = new EditText(this);
        input.setHint("Enter seconds");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Start", (dialog, which) -> {
            String value = input.getText().toString().trim();

            if (value.isEmpty()) {
                Toast.makeText(this, "Please enter a time", Toast.LENGTH_SHORT).show();
                return;
            }

            int seconds;
            try {
                seconds = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (seconds < 1) {
                Toast.makeText(this, "Time must be at least 1 second", Toast.LENGTH_SHORT).show();
                return;
            }

            startStudy(seconds);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}