package ph.edu.usc24100596;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class StudyActivity extends AppCompatActivity {

    TextView questionText, answerText, showAnswer;
    LinearLayout showAnswerBar, ratingBar;
    Button btnAgain, btnHard, btnGood, btnEasy;
    ImageButton btnBack;
    ArrayList<Card> deck = new ArrayList<>();
    ArrayList<Card> studyQueue = new ArrayList<>();
    boolean confirmRating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study);

        // Initialize views
        questionText = findViewById(R.id.questionText);
        answerText = findViewById(R.id.answerText);
        showAnswer = findViewById(R.id.showAnswer);
        showAnswerBar = findViewById(R.id.showAnswerContainer);
        ratingBar = findViewById(R.id.ratingContainer);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnAgain = findViewById(R.id.btnAgain);
        btnHard = findViewById(R.id.btnHard);
        btnGood = findViewById(R.id.btnGood);
        btnEasy = findViewById(R.id.btnEasy);

        // Load deck
        String categoryName = getIntent().getStringExtra("categoryName");
        String deckName = getIntent().getStringExtra("deckName");
        confirmRating = getIntent().getBooleanExtra("confirmRating", false);
        loadDeck(categoryName, deckName);

        if (deck.isEmpty()) {
            Toast.makeText(this, "No cards found in this deck!", Toast.LENGTH_SHORT).show();
            questionText.setText("No cards to display!");
            showAnswerBar.setVisibility(View.GONE);
            ratingBar.setVisibility(View.GONE);
            return;
        }

        // Initialize study queue
        studyQueue.addAll(deck);
        displayCard(studyQueue.get(0));

        // Show answer listener
        showAnswer.setOnClickListener(v -> revealAnswer());

        // Rating button listeners
        btnAgain.setOnClickListener(v -> rateCard(studyQueue.get(0), "Again"));
        btnHard.setOnClickListener(v -> rateCard(studyQueue.get(0), "Hard"));
        btnGood.setOnClickListener(v -> rateCard(studyQueue.get(0), "Good"));
        btnEasy.setOnClickListener(v -> rateCard(studyQueue.get(0), "Easy"));
    }

    private void loadDeck(String categoryName, String deckName) {
        deck.clear();

        if (categoryName == null || deckName == null) {
            questionText.setText("Deck not specified!");
            showAnswerBar.setVisibility(View.GONE);
            ratingBar.setVisibility(View.GONE);
            return;
        }

        DBHelper dbHelper = new DBHelper(this);
        int deckId = dbHelper.getDeckId(deckName, categoryName);

        if (deckId == -1) {
            questionText.setText("Deck not found!");
            showAnswerBar.setVisibility(View.GONE);
            ratingBar.setVisibility(View.GONE);
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, deck_id, front, back FROM cards WHERE deck_id=?",
                new String[]{String.valueOf(deckId)}
        );

        if (cursor.moveToFirst()) {
            do {
                deck.add(new Card(
                        cursor.getInt(0),
                        cursor.getInt(1),
                        cursor.getString(2),
                        cursor.getString(3)
                ));
            } while (cursor.moveToNext());
        }

        cursor.close();
    }

    private void revealAnswer() {
        questionText.setVisibility(View.GONE);
        answerText.setVisibility(View.VISIBLE);
        showAnswerBar.setVisibility(View.GONE);
        ratingBar.setVisibility(View.VISIBLE);

        updateRatingButtons(studyQueue.get(0));
    }

    private void updateRatingButtons(Card card) {
        if (card.isLearned()) {
            btnEasy.setVisibility(View.GONE); // mastered, no "Easy" button
        } else {
            btnEasy.setVisibility(View.VISIBLE);
        }

        // Optional: disable buttons if studyQueue is empty
        boolean hasCards = !studyQueue.isEmpty();
        btnAgain.setEnabled(hasCards);
        btnHard.setEnabled(hasCards);
        btnGood.setEnabled(hasCards);
        btnEasy.setEnabled(hasCards && !card.isLearned());
    }

    private void displayCard(Card card) {
        questionText.setText(card.getFront());
        answerText.setText(card.getBack());

        questionText.setVisibility(View.VISIBLE);
        answerText.setVisibility(View.GONE);
        showAnswerBar.setVisibility(View.VISIBLE);
        ratingBar.setVisibility(View.GONE);
    }

    private void onCardRated(Card card, String rating) {
        studyQueue.remove(0);

        switch (rating) {
            case "Again":
                card.setInterval(0);
                studyQueue.add(0, card);
                break;
            case "Hard":
                card.setInterval(card.getInterval() + 1);
                studyQueue.add(Math.min(2, studyQueue.size()), card);
                break;
            case "Good":
                card.setInterval(card.getInterval() + 2);
                studyQueue.add(Math.min(4, studyQueue.size()), card);
                break;
            case "Easy":
                card.setInterval(card.getInterval() + 5);
                card.setLearned(true);
                break;
        }

        if (!studyQueue.isEmpty()) {
            displayCard(studyQueue.get(0));
        } else {
            Toast.makeText(this, "All cards completed!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void rateCard(Card card, String rating) {
        if (confirmRating) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Rating")
                    .setMessage("Mark this card as \"" + rating + "\"?")
                    .setPositiveButton("Yes", (dialog, which) -> onCardRated(card, rating))
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            onCardRated(card, rating);
        }
    }
}