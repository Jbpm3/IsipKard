package ph.edu.usc24100596;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import java.util.ArrayList;
import java.util.List;

public class CardBrowserActivity extends AppCompatActivity {
    private LinearLayout contentContainer;
    private DBHelper dbHelper;
    private View rootLayout;
    private FrameLayout sidebarContainer;
    private ImageButton menuIcon;
    private View sidebarOverlay;
    private Spinner deckTitleSpinner;
    private TextView tvCardCount;
    private SearchView searchView;
    private ImageButton searchButton;

    // Holds the full current card list so search can filter without re-querying
    private List<Card> currentCardList = new ArrayList<>();
    private String currentSortOrder = "difficulty"; // default sort
    private String currentSelectedDeck = "All Decks"; // track selected deck

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_browser_sidebar);

        // Initialize views
        rootLayout = findViewById(R.id.rootLayout);
        menuIcon = findViewById(R.id.menuIcon);
        sidebarContainer = findViewById(R.id.sidebar_container);
        sidebarOverlay = findViewById(R.id.sidebarOverlay);
        contentContainer = findViewById(R.id.contentContainer);
        deckTitleSpinner = findViewById(R.id.deckTitleSpinner);
        tvCardCount = findViewById(R.id.tvCardCount);
        searchView = findViewById(R.id.searchView);
        searchButton = findViewById(R.id.SearchButton);

        // Initialize database helper
        dbHelper = new DBHelper(this);

        // --- Search Button toggles SearchView ---
        searchButton.setOnClickListener(v -> {
            if (searchView.getVisibility() == View.GONE) {
                searchView.setVisibility(View.VISIBLE);
                searchView.setIconified(false); // expand and focus
                searchView.requestFocus();
            } else {
                searchView.setVisibility(View.GONE);
                searchView.setQuery("", false);
                searchView.clearFocus();
                // Restore full list when search is dismissed
                renderCards(currentCardList, "No cards yet");
            }
        });

        // --- SearchView listener ---
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterCards(query.trim());
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCards(newText.trim());
                return true;
            }
        });

        // Dismiss search when X is pressed inside SearchView
        searchView.setOnCloseListener(() -> {
            searchView.setVisibility(View.GONE);
            renderCards(currentCardList, "No cards yet");
            return false;
        });

        // --- Setup Deck Spinner ---
        List<String> allDecks = dbHelper.getAllDeckNames();
        List<String> spinnerItems = new ArrayList<>();
        spinnerItems.add("All Decks");
        spinnerItems.addAll(allDecks);

        ArrayAdapter<String> deckAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                spinnerItems
        );
        deckAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deckTitleSpinner.setAdapter(deckAdapter);

        deckTitleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Clear search when switching decks
                searchView.setQuery("", false);
                searchView.setVisibility(View.GONE);
                currentSelectedDeck = spinnerItems.get(position);
                if (currentSelectedDeck.equals("All Decks")) {
                    loadAllCards();
                } else {
                    loadCardsFromDeck(currentSelectedDeck);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // --- Sidebar ---
        menuIcon.setOnClickListener(v -> toggleSidebar());
        rootLayout.setOnClickListener(v -> {
            if (sidebarContainer.getVisibility() == View.VISIBLE) closeSidebar();
        });
        sidebarOverlay.setOnClickListener(v -> closeSidebar());

        FrameLayout decksContainer = findViewById(R.id.decks_header_container);
        FrameLayout cardBrowserContainer = findViewById(R.id.card_browser_container);

        decksContainer.setOnClickListener(v -> {
            startActivity(new Intent(CardBrowserActivity.this, LibraryHomeActivity.class));
            closeSidebar();
        });

        cardBrowserContainer.setOnClickListener(v -> {
            startActivity(new Intent(CardBrowserActivity.this, CardBrowserActivity.class));
            closeSidebar();
        });

        // --- Sort Spinner ---
        Spinner spinnerSort = findViewById(R.id.spinnerSort);
        String[] sortOptions = {
                "Sort: Difficulty",
                "Sort: Alphabetical"
        };
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_sort_item,
                sortOptions
        );
        sortAdapter.setDropDownViewResource(R.layout.spinner_sort_dropdown);
        spinnerSort.setAdapter(sortAdapter);

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: currentSortOrder = "difficulty"; break;
                    case 1: currentSortOrder = "alpha";      break;
                }
                // Re-load with new sort, respecting current deck selection
                searchView.setQuery("", false);
                if (currentSelectedDeck.equals("All Decks")) {
                    loadAllCards();
                } else {
                    loadCardsFromDeck(currentSelectedDeck);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Load cards initially
        loadAllCards();
    }

    // --- Load all cards ---
    private void loadAllCards() {
        currentCardList.clear();
        currentCardList.addAll(dbHelper.getAllCardsSorted(currentSortOrder));
        renderCards(currentCardList, "No cards yet");
    }

    // --- Load cards from selected deck ---
    private void loadCardsFromDeck(String deckName) {
        currentCardList.clear();

        int deckId = dbHelper.getDeckIdName(deckName);
        if (deckId == -1) {
            renderCards(currentCardList, "No cards found in " + deckName);
            return;
        }

        currentCardList.addAll(dbHelper.getCardsForDeckSorted(deckId, currentSortOrder));
        renderCards(currentCardList, "No cards found in " + deckName);
    }

    // --- Filter cards by keyword (searches both front and back) ---
    private void filterCards(String query) {
        if (query.isEmpty()) {
            renderCards(currentCardList, "No cards yet");
            return;
        }

        String lowerQuery = query.toLowerCase();
        List<Card> filtered = new ArrayList<>();

        for (Card card : currentCardList) {
            if (card.getFront().toLowerCase().contains(lowerQuery)
                    || card.getBack().toLowerCase().contains(lowerQuery)) {
                filtered.add(card);
            }
        }

        renderCards(filtered, "No cards match \"" + query + "\"");
    }

    // --- Render a list of cards to the UI ---
    private void renderCards(List<Card> cards, String emptyMessage) {
        // Remove only card views, keeping sortBar and tvEmptyState intact
        int childCount = contentContainer.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = contentContainer.getChildAt(i);
            if (child.getId() != R.id.sortBar && child.getId() != R.id.tvEmptyState) {
                contentContainer.removeViewAt(i);
            }
        }

        TextView tvEmptyState = findViewById(R.id.tvEmptyState);

        if (cards.isEmpty()) {
            tvEmptyState.setText(emptyMessage);
            tvEmptyState.setVisibility(View.VISIBLE);
            updateCardCount(0);
            return;
        }

        tvEmptyState.setVisibility(View.GONE);
        for (Card card : cards) addCardToUI(card);
        updateCardCount(cards.size());
    }

    private void updateCardCount(int count) {
        tvCardCount.setText(count == 1 ? "1 Card Shown" : count + " Cards Shown");
    }

    private void addCardToUI(Card card) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.card_preview, contentContainer, false);
        TextView frontText = cardView.findViewById(R.id.frontText);
        TextView backText = cardView.findViewById(R.id.backText);

        frontText.setText(card.getFront());
        backText.setText(card.getBack());

        contentContainer.addView(cardView);
    }

    private List<Card> getCardsForDeck(int deckId) {
        List<Card> cards = new ArrayList<>();
        android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
        android.database.Cursor cursor = db.rawQuery(
                "SELECT id, front, back FROM cards WHERE deck_id=?",
                new String[]{String.valueOf(deckId)}
        );
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String front = cursor.getString(1);
                String back = cursor.getString(2);
                cards.add(new Card(id, deckId, front, back));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return cards;
    }

    // --- Sidebar Animations ---
    private void toggleSidebar() {
        if (sidebarContainer.getVisibility() == View.GONE) {
            sidebarOverlay.setVisibility(View.VISIBLE);
            sidebarContainer.setVisibility(View.VISIBLE);
            sidebarContainer.setTranslationX(-sidebarContainer.getWidth());
            sidebarContainer.animate().translationX(0).setDuration(300).start();
        } else closeSidebar();
    }

    private void closeSidebar() {
        sidebarContainer.animate().translationX(-sidebarContainer.getWidth()).setDuration(300).withEndAction(() -> {
            sidebarContainer.setVisibility(View.GONE);
            sidebarOverlay.setVisibility(View.GONE);
        }).start();
    }
}