package ph.edu.usc24100596;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class CreateCardActivity extends AppCompatActivity {

    private Spinner spinnerDecks;
    private EditText etQuestion, etAnswer;
    private Button btnSaveCard;

    private DBHelper dbHelper;
    private List<String> allDecks;
    private String selectedDeck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_card);

        spinnerDecks = findViewById(R.id.spinnerDecks);
        etQuestion = findViewById(R.id.etQuestion);
        etAnswer = findViewById(R.id.etAnswer);
        btnSaveCard = findViewById(R.id.btnSaveCard);

        dbHelper = new DBHelper(this);

        // Get the category passed from LibraryHomeActivity
        String selectedCategory = getIntent().getStringExtra("category_name");
        if (selectedCategory == null) {
            Toast.makeText(this, "No category selected", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if category is missing
            return;
        }

        // Load decks from DB for this category
        loadDecks(selectedCategory);

        // Save Card Button
        btnSaveCard.setOnClickListener(v -> saveCard(selectedCategory));

        // Back button on top bar
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void loadDecks(String category) {
        allDecks = dbHelper.getDecksForCategory(category);
        if (allDecks.isEmpty()) {
            Toast.makeText(this, "No decks available in this category. Please create a deck first.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no decks
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, allDecks);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDecks.setAdapter(adapter);
        }
    }

    private void saveCard(String category) {
        String front = etQuestion.getText().toString().trim();
        String back = etAnswer.getText().toString().trim();
        String selectedDeck = spinnerDecks.getSelectedItem().toString();

        if (front.isEmpty()) {
            etQuestion.setError("Enter a question");
            return;
        }
        if (back.isEmpty()) {
            etAnswer.setError("Enter an answer");
            return;
        }

        boolean added = dbHelper.addCard(selectedDeck, category, front, back);

        if (added) {
            Toast.makeText(this, "Card added successfully!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Failed to add card.", Toast.LENGTH_SHORT).show();
        }
    }
}