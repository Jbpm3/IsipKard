package ph.edu.usc24100596;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import java.util.ArrayList;
import java.util.List;
import android.database.Cursor;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "isipkard.db";
    private static final int DATABASE_VERSION = 5;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Create users table
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "email TEXT UNIQUE, " +
                "password TEXT" +
                ")");

        db.execSQL("CREATE TABLE Categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "category_name TEXT UNIQUE" +
                ")");

        db.execSQL(
                "CREATE TABLE decks (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "category_name TEXT," +
                        "deck_name TEXT)"
        );
        db.execSQL("CREATE TABLE cards (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "deck_id INTEGER NOT NULL," +
                "front TEXT," +
                "back TEXT," +
                "interval INTEGER DEFAULT 0," +
                "FOREIGN KEY(deck_id) REFERENCES decks(id) ON DELETE CASCADE" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            // Safely add interval column to existing cards table
            try { db.execSQL("ALTER TABLE cards ADD COLUMN interval INTEGER DEFAULT 0"); } catch (Exception ignored) {}
        } else {
            db.execSQL("DROP TABLE IF EXISTS users");
            db.execSQL("DROP TABLE IF EXISTS Categories");
            db.execSQL("DROP TABLE IF EXISTS decks");
            db.execSQL("DROP TABLE IF EXISTS cards");
            onCreate(db);
        }
    }

    // Insert a new user (register)
    public boolean insertUser(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("email", email);
        cv.put("password", password);
        long result = db.insert("users", null, cv);
        return result != -1;
    }

    // Check user login
    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM users WHERE email=? AND password=?";
        Cursor cursor = db.rawQuery(query, new String[]{email, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    //check if email already exists (for registration)
    public boolean checkEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE email=?", new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    //Categories
    public boolean addCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("category_name", name);
        long result = db.insert("Categories", null, cv);
        return result != -1;
    }

    // Fetch all categories
    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT category_name FROM Categories", null);
        if (cursor.moveToFirst()) {
            do {
                categories.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return categories;
    }

    public boolean addDeck(String category, String deck){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("category_name", category);
        values.put("deck_name",deck);

        long result = db.insert("decks",null,values);
        return result != -1;
    }

    public boolean deleteCategory(String categoryName){

        SQLiteDatabase db = this.getWritableDatabase();

        int rows = db.delete(
                "Categories",
                "category_name=?",
                new String[]{categoryName}
        );
        return rows > 0;
    }

    //DECKS

    public List<String> getDecksForCategory(String category){

        List<String> decks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT deck_name FROM decks WHERE category_name=?",
                new String[]{category}
        );

        if(cursor.moveToFirst()){
            do{
                decks.add(cursor.getString(0));
            }while(cursor.moveToNext());
        }

        cursor.close();
        return decks;
    }
    public boolean deleteDeck(String categoryName, String deckName){

        SQLiteDatabase db = this.getWritableDatabase();

        int rows = db.delete(
                "decks",
                "category_name=? AND deck_name=?",
                new String[]{categoryName, deckName}
        );

        return rows > 0;
    }

    //CARDS
    public boolean addCard(String deckName, String categoryName, String front, String back) {
        int deckId = getDeckId(deckName, categoryName);
        if (deckId == -1) return false;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("deck_id", deckId);
        values.put("front", front);
        values.put("back", back);

        long result = db.insert("cards", null, values);
        return result != -1;
    }

    // Helper method to get deck ID by name
    public int getDeckId(String deckName, String categoryName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM decks WHERE deck_name=? AND category_name=?",
                new String[]{deckName, categoryName});
        int id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        cursor.close();
        return id;
    }

    public List<String> getAllDeckNames() {
        List<String> decks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT deck_name FROM decks", null);
        if (cursor.moveToFirst()) {
            do {
                decks.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return decks;
    }

    public int getDeckIdName(String deckName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM decks WHERE deck_name=?",
                new String[]{deckName});
        int id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        cursor.close();
        return id;
    }


    // Get the number of cards in a deck
    public int getCardCountForDeck(String deckName, String categoryName) {
        int deckId = getDeckId(deckName, categoryName);
        if(deckId == -1) return 0;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM cards WHERE deck_id=?",
                new String[]{String.valueOf(deckId)}
        );
        int count = 0;
        if(cursor.moveToFirst()){
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public boolean deleteCard(int cardId){
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete("cards","id=?", new String[]{String.valueOf(cardId)});
        return rows > 0;
    }

    // Update front and back text of an existing card
    public boolean updateCard(int cardId, String front, String back) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("front", front);
        values.put("back", back);
        int rows = db.update("cards", values, "id=?", new String[]{String.valueOf(cardId)});
        return rows > 0;
    }

    // Fetch cards for a single deck with sort order applied
    public List<Card> getCardsForDeckSorted(int deckId, String sortOrder) {
        List<Card> cards = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String orderBy;
        switch (sortOrder) {
            case "difficulty": orderBy = "c.interval ASC"; break; // lowest interval = hardest first
            case "alpha":      orderBy = "c.front COLLATE NOCASE ASC"; break;
            default:           orderBy = "c.id ASC"; break;
        }

        Cursor cursor = db.rawQuery(
                "SELECT c.id, c.deck_id, c.front, c.back, c.interval " +
                        "FROM cards c WHERE c.deck_id=? ORDER BY " + orderBy,
                new String[]{String.valueOf(deckId)}
        );

        if (cursor.moveToFirst()) {
            do {
                Card card = new Card(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), cursor.getString(3));
                card.setInterval(cursor.getInt(4));
                cards.add(card);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return cards;
    }

    // Fetch ALL cards across all decks with sort order applied
    public List<Card> getAllCardsSorted(String sortOrder) {
        List<Card> cards = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String orderBy;
        switch (sortOrder) {
            case "difficulty": orderBy = "c.interval ASC"; break;
            case "alpha":      orderBy = "c.front COLLATE NOCASE ASC"; break;
            default:           orderBy = "c.id ASC"; break;
        }

        Cursor cursor = db.rawQuery(
                "SELECT c.id, c.deck_id, c.front, c.back, c.interval " +
                        "FROM cards c ORDER BY " + orderBy,
                null
        );

        if (cursor.moveToFirst()) {
            do {
                Card card = new Card(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), cursor.getString(3));
                card.setInterval(cursor.getInt(4));
                cards.add(card);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return cards;
    }

}