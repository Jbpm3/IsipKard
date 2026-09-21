package ph.edu.usc24100596;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;

import java.util.ArrayList;
import java.util.List;

public class LibraryHomeActivity extends AppCompatActivity {

    private ImageButton fabAddCard, fabCreateDeck, fabCreateCategory;
    private boolean isFabExpanded = false;
    private boolean confirmRating = false; // off by default

    private LinearLayout deckList;
    private TextView tvEmptyState, tvYourDecks;

    private DBHelper dbHelper;

    private SearchView searchView;
    private ImageButton SearchButton;

    private List<String> allCategories = new ArrayList<>();
    private String currentCategory = null;

    private View rootLayout; // root clickable layout

    View sidebarOverlay;

    private final ActivityResultLauncher<Intent> createCardLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            if (currentCategory != null) {
                                openDecksForCategory(currentCategory);
                            } else {
                                loadCategoriesFromDB();
                            }
                        }
                    }
            );


    ImageButton menuIcon;
    FrameLayout sidebarContainer;
    private boolean isSidebarVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_home);

        // ROOT LAYOUT
        rootLayout = findViewById(R.id.rootLayout);
        // collapse FAB menu if anywhere in rootLayout is clicked
        rootLayout.setOnClickListener(v -> {
            if (isFabExpanded) {
                collapseFabMenu();
            }
            if (sidebarContainer.getVisibility() == View.VISIBLE) {

                int[] location = new int[2];
                sidebarContainer.getLocationOnScreen(location);

                int sidebarLeft = location[0];
                int sidebarRight = sidebarLeft + sidebarContainer.getWidth();

                float clickX = v.getX();

                // if click is outside sidebar width
                if (clickX > sidebarRight) {
                    closeSidebar();
                }
            }
        });

        menuIcon = findViewById(R.id.menuIcon);
        sidebarContainer = findViewById(R.id.sidebar_container);

        menuIcon.setOnClickListener(v -> toggleSidebar());

        //SIDEBAR

        FrameLayout decksContainer = findViewById(R.id.decks_header_container);
        TextView decksLabel = findViewById(R.id.decks_label);

        FrameLayout cardBrowserContainer = findViewById(R.id.card_browser_container);
        TextView cardBrowserLabel = findViewById(R.id.card_browser_label);

        cardBrowserContainer.setOnClickListener(v -> {
            Intent intent = new Intent(LibraryHomeActivity.this, CardBrowserActivity.class);
            startActivity(intent);
            closeSidebar();
        });

        // END OF SIDEBAR

        sidebarOverlay = findViewById(R.id.sidebarOverlay);

        sidebarOverlay.setOnClickListener(v -> closeSidebar());

        fabAddCard = findViewById(R.id.fabAddCard);
        fabCreateDeck = findViewById(R.id.fabCreateDeck);
        fabCreateCategory = findViewById(R.id.fabCreateCategory);

        searchView = findViewById(R.id.searchView);
        SearchButton = findViewById(R.id.SearchButton);

        // Ellipsis menu
        ImageButton ellipsisButton = findViewById(R.id.EllipsisButton);
        ellipsisButton.setOnClickListener(v -> showLibraryOptionsMenu(v));

        dbHelper = new DBHelper(this);

        deckList = findViewById(R.id.deckList);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvYourDecks = findViewById(R.id.tvYourDecks);

        // MAKE ScrollView clickable to propagate touches to root
        ScrollView scrollView = findViewById(R.id.contentScroll);
        scrollView.setClickable(true);
        scrollView.setFocusable(true);

        // FAB MAIN
        fabAddCard.setOnClickListener(v -> {
            if (isFabExpanded) {
                if (currentCategory != null) {
                    Intent intent = new Intent(this, CreateCardActivity.class);
                    intent.putExtra("category_name", currentCategory);
                    createCardLauncher.launch(intent);
                } else {
                    Toast.makeText(this, "Select a category first", Toast.LENGTH_SHORT).show();
                }
                collapseFabMenu();
            } else {
                expandFabMenu();
            }
        });

        fabCreateDeck.setOnClickListener(v -> {
            collapseFabMenu();
            showCreateDeckDialog();
        });

        fabCreateCategory.setOnClickListener(v -> {
            collapseFabMenu();
            showCreateCategoryDialog();
        });

        // SEARCH BUTTON
        SearchButton.setOnClickListener(v -> {
            if(searchView.getVisibility() == View.GONE){
                searchView.setVisibility(View.VISIBLE);
                searchView.requestFocus();
            } else {
                searchView.setVisibility(View.GONE);
                searchView.setQuery("",false);
                loadCategoriesFromDB();
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                deckList.removeAllViews();
                for(String category : allCategories){
                    if(category.toLowerCase().contains(newText.toLowerCase())){
                        addCategoryToUI(category);
                    }
                }
                return true;
            }
        });

        loadCategoriesFromDB();
    }

    private void expandFabMenu() {
        fabCreateDeck.setVisibility(View.VISIBLE);
        fabCreateCategory.setVisibility(View.VISIBLE);
        fabAddCard.setImageResource(R.drawable.add_card);
        isFabExpanded = true;
    }

    private void collapseFabMenu() {
        fabCreateDeck.setVisibility(View.GONE);
        fabCreateCategory.setVisibility(View.GONE);
        fabAddCard.setImageResource(R.drawable.add);
        isFabExpanded = false;
    }

    private void toggleSidebar() {
        if (sidebarContainer.getVisibility() == View.GONE) {

            sidebarOverlay.setVisibility(View.VISIBLE);

            sidebarContainer.setVisibility(View.VISIBLE);
            sidebarContainer.setTranslationX(-sidebarContainer.getWidth());

            sidebarContainer.animate()
                    .translationX(0)
                    .setDuration(300)
                    .start();

        } else {
            closeSidebar();
        }
    }

    private void closeSidebar() {
        sidebarContainer.animate()
                .translationX(-sidebarContainer.getWidth())
                .setDuration(300)
                .withEndAction(() -> {
                    sidebarContainer.setVisibility(View.GONE);
                    sidebarOverlay.setVisibility(View.GONE);
                })
                .start();
    }

    private void loadCategoriesFromDB() {
        deckList.removeAllViews();
        allCategories = dbHelper.getAllCategories();

        if (allCategories.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            for (String cat : allCategories) {
                addCategoryToUI(cat);
            }
        }
    }

    // --- CATEGORY & DECK METHODS ---
    private void addCategoryToUI(String categoryName) {
        View categoryView = getLayoutInflater().inflate(R.layout.category_item, deckList, false);
        TextView tvCategoryName = categoryView.findViewById(R.id.tvCategoryName);
        tvCategoryName.setText(categoryName);

        categoryView.setOnClickListener(v -> openDecksForCategory(categoryName));
        categoryView.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            showDeleteCategoryDialog(categoryName, categoryView);
            return true;
        });

        deckList.addView(categoryView);
        tvEmptyState.setVisibility(View.GONE);
    }

    private void showDeleteCategoryDialog(String categoryName, View categoryView){
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Category")
                .setMessage("Delete \"" + categoryName + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if(dbHelper.deleteCategory(categoryName)){
                        deckList.removeView(categoryView);
                        Toast.makeText(this,"Category deleted",Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCreateCategoryDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_create_category);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText etCategoryName = dialog.findViewById(R.id.etCategoryName);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnOK = dialog.findViewById(R.id.btnOK);

        btnCancel.setOnClickListener(view -> dialog.dismiss());
        btnOK.setOnClickListener(view -> {
            String name = etCategoryName.getText().toString().trim();
            if(!name.isEmpty() && dbHelper.addCategory(name)){
                addCategoryToUI(name);
                Toast.makeText(this,"Category added",Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else etCategoryName.setError("Please enter a name");
        });

        dialog.show();
    }

    private void showCreateDeckDialog() {
        if(currentCategory == null){
            Toast.makeText(this,"Select a category first",Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_create_deck);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText etDeckName = dialog.findViewById(R.id.etDeckName);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnOK = dialog.findViewById(R.id.btnOK);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnOK.setOnClickListener(v -> {
            String deckName = etDeckName.getText().toString().trim();
            if(!deckName.isEmpty() && dbHelper.addDeck(currentCategory, deckName)){
                addDeckToUI(deckName);
                dialog.dismiss();
            } else etDeckName.setError("Enter deck name");
        });

        dialog.show();
    }

    private void addDeckToUI(String deckName){
        View deckView = getLayoutInflater().inflate(R.layout.deck_item, deckList, false);
        TextView tvDeckName = deckView.findViewById(R.id.tvDeckName);
        int cardCount = dbHelper.getCardCountForDeck(deckName, currentCategory);
        tvDeckName.setText(deckName + " (" + cardCount + " cards)");

        deckView.setOnClickListener(v -> openDeck(deckName));
        deckList.addView(deckView);
        tvEmptyState.setVisibility(View.GONE);
    }

    private void openDeck(String deckName) {
        // Hide main content (categories/decks list)
        findViewById(R.id.contentContainer).setVisibility(View.GONE);

        // Inflate deck layout
        ViewGroup root = findViewById(android.R.id.content);
        View deckView = getLayoutInflater().inflate(R.layout.deck_screen, root, false);
        root.addView(deckView);

        // Set deck title
        TextView tvDeckTitle = deckView.findViewById(R.id.deckTitle);
        tvDeckTitle.setText(deckName);

        // Setup back button
        ImageButton btnBack = deckView.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            root.removeView(deckView);
            findViewById(R.id.contentContainer).setVisibility(View.VISIBLE);
        });

        // Swipe right to go back to library
        attachSwipeBack(deckView, () -> {
            root.removeView(deckView);
            findViewById(R.id.contentContainer).setVisibility(View.VISIBLE);
        });

        // --- FAB Menu inside deck ---
        ImageButton fabAddCard = deckView.findViewById(R.id.fabAddCard);
        ImageButton fabCreateDeck = deckView.findViewById(R.id.fabCreateDeck);
        ImageButton fabCreateCategory = deckView.findViewById(R.id.fabCreateCategory);

        final boolean[] isFabExpanded = {false};

        fabAddCard.setOnClickListener(v -> {
            if (isFabExpanded[0]) {
                // Add card to current deck
                Intent intent = new Intent(LibraryHomeActivity.this, CreateCardActivity.class);
                intent.putExtra("deck_name", deckName);
                createCardLauncher.launch(intent);
                collapseFabMenu(fabAddCard, fabCreateDeck, fabCreateCategory, isFabExpanded);
            } else {
                expandFabMenu(fabAddCard, fabCreateDeck, fabCreateCategory, isFabExpanded);
            }
        });

        fabCreateDeck.setOnClickListener(v -> {
            collapseFabMenu(fabAddCard, fabCreateDeck, fabCreateCategory, isFabExpanded);
            showCreateDeckDialog();
        });

        fabCreateCategory.setOnClickListener(v -> {
            collapseFabMenu(fabAddCard, fabCreateDeck, fabCreateCategory, isFabExpanded);
            showCreateCategoryDialog();
        });

        // --- Deck Buttons ---
        TextView studyButton = deckView.findViewById(R.id.studyButton);
        studyButton.setOnClickListener(v -> {
            Intent intent = new Intent(LibraryHomeActivity.this, StudyActivity.class);
            intent.putExtra("deckName", deckName);
            intent.putExtra("categoryName", currentCategory);
            intent.putExtra("confirmRating", confirmRating);
            startActivity(intent);
        });

        TextView timedModeButton = deckView.findViewById(R.id.timedModeButton);
        timedModeButton.setOnClickListener(v -> {
            Intent intent = new Intent(LibraryHomeActivity.this, TimedModeActivity.class);
            intent.putExtra("deckName", deckName);
            intent.putExtra("categoryName", currentCategory);
            intent.putExtra("confirmRating", confirmRating);
            startActivity(intent);
        });

        TextView editCardsButton = deckView.findViewById(R.id.editCardsButton);
        editCardsButton.setOnClickListener(v ->
                openEditCards(deckName, root, deckView)
        );

        // --- Handle device back press ---
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (deckView.getParent() != null) {
                    // Remove deck layout
                    root.removeView(deckView);

                    // Show main content again
                    findViewById(R.id.contentContainer).setVisibility(View.VISIBLE);
                } else {
                    // Otherwise go back to previous activity
                    finish();
                }
            }
        });
    }

    // Helper methods for FAB menu
    private void expandFabMenu(ImageButton mainFab, ImageButton fabDeck, ImageButton fabCategory, boolean[] isFabExpanded) {
        fabDeck.setVisibility(View.VISIBLE);
        fabCategory.setVisibility(View.VISIBLE);
        mainFab.setImageResource(R.drawable.add_card);
        isFabExpanded[0] = true;
    }

    private void collapseFabMenu(ImageButton mainFab, ImageButton fabDeck, ImageButton fabCategory, boolean[] isFabExpanded) {
        fabDeck.setVisibility(View.GONE);
        fabCategory.setVisibility(View.GONE);
        mainFab.setImageResource(R.drawable.add);
        isFabExpanded[0] = false;
    }

    private void openDecksForCategory(String categoryName) {
        currentCategory = categoryName;
        tvYourDecks.setText(categoryName);
        deckList.removeAllViews();

        List<String> decks = dbHelper.getDecksForCategory(categoryName);
        if(decks.isEmpty()){
            tvEmptyState.setText("No decks yet");
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            for(String deck : decks) addDeckToUI(deck);
        }

        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(currentCategory != null){
                    currentCategory = null;
                    tvYourDecks.setText("Your Decks");
                    loadCategoriesFromDB();
                    tvEmptyState.setText("No categories yet");
                } else finish();
            }
        });
    }

    // --- Edit Cards Screen ---
    private void openEditCards(String deckName, ViewGroup root, View deckView) {
        // Hide the deck screen
        deckView.setVisibility(View.GONE);

        // Inflate edit cards layout (a simple scrollable list)
        View editView = getLayoutInflater().inflate(R.layout.activity_edit_cards, root, false);
        root.addView(editView);

        TextView tvTitle = editView.findViewById(R.id.tvEditCardsTitle);
        tvTitle.setText("Edit Cards – " + deckName);

        LinearLayout cardListContainer = editView.findViewById(R.id.editCardListContainer);

        // Back button returns to deck screen
        ImageButton btnBack = editView.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            root.removeView(editView);
            deckView.setVisibility(View.VISIBLE);
        });

        // Swipe right to go back to deck screen
        attachSwipeBack(editView, () -> {
            root.removeView(editView);
            deckView.setVisibility(View.VISIBLE);
        });

        // Load and display cards
        refreshEditCardList(deckName, cardListContainer, root, editView, deckView);
    }

    private void refreshEditCardList(String deckName, LinearLayout container, ViewGroup root, View editView, View deckView) {
        container.removeAllViews();

        int deckId = dbHelper.getDeckIdName(deckName);
        if (deckId == -1) return;

        android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
        android.database.Cursor cursor = db.rawQuery(
                "SELECT id, front, back FROM cards WHERE deck_id=?",
                new String[]{String.valueOf(deckId)}
        );

        if (!cursor.moveToFirst()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No cards in this deck.");
            tvEmpty.setTextSize(15f);
            tvEmpty.setTextColor(0xFF888888);
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            tvEmpty.setPadding(0, 40, 0, 0);
            container.addView(tvEmpty);
            cursor.close();
            return;
        }

        do {
            int cardId   = cursor.getInt(0);
            String front = cursor.getString(1);
            String back  = cursor.getString(2);

            View row = getLayoutInflater().inflate(R.layout.edit_card_item, container, false);

            EditText etFront  = row.findViewById(R.id.etCardFront);
            EditText etBack   = row.findViewById(R.id.etCardBack);
            Button   btnSave  = row.findViewById(R.id.btnSaveCard);
            Button   btnDelete = row.findViewById(R.id.btnDeleteCard);

            etFront.setText(front);
            etBack.setText(back);

            btnSave.setOnClickListener(v -> {
                String newFront = etFront.getText().toString().trim();
                String newBack  = etBack.getText().toString().trim();
                if (newFront.isEmpty()) { etFront.setError("Required"); return; }
                if (newBack.isEmpty())  { etBack.setError("Required");  return; }
                dbHelper.updateCard(cardId, newFront, newBack);
                Toast.makeText(this, "Card saved", Toast.LENGTH_SHORT).show();
            });

            btnDelete.setOnClickListener(v ->
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Delete Card")
                            .setMessage("Are you sure you want to delete this card?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                dbHelper.deleteCard(cardId);
                                refreshEditCardList(deckName, container, root, editView, deckView);
                                Toast.makeText(this, "Card deleted", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show()
            );

            container.addView(row);
        } while (cursor.moveToNext());

        cursor.close();
    }

    // --- Library Options (Ellipsis) Menu ---
    private void showLibraryOptionsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Confirm before rating")
                .setCheckable(true)
                .setChecked(confirmRating);

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                confirmRating = !confirmRating;
                item.setChecked(confirmRating);
                String state = confirmRating ? "On" : "Off";
                Toast.makeText(this, "Confirm before rating: " + state, Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        popup.show();
    }

    // --- Swipe-right-to-go-back helper ---
    private void attachSwipeBack(View view, Runnable onSwipeBack) {
        final float[] startX = {0f};
        final float[] startY = {0f};
        final float SWIPE_THRESHOLD = 120f;
        final float SWIPE_MAX_VERTICAL = 100f;

        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX[0] = event.getX();
                    startY[0] = event.getY();
                    return false; // don't consume — lets buttons, edittext, scroll still work

                case MotionEvent.ACTION_UP:
                    float dx = event.getX() - startX[0];
                    float dy = Math.abs(event.getY() - startY[0]);
                    if (dx > SWIPE_THRESHOLD && dy < SWIPE_MAX_VERTICAL) {
                        onSwipeBack.run();
                        return true;
                    }
                    return false;
            }
            return false;
        });
    }
}