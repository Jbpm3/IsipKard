package ph.edu.usc24100596;

public class Card {
    private int id;
    private int deckId;
    private String front;
    private String back;
    private int interval;
    private boolean learned;

    public Card(int id, int deckId, String front, String back) {
        this.id = id;
        this.deckId = deckId;
        this.front = front;
        this.back = back;
        this.interval = 0;
        this.learned = false;
    }

    // Getters
    public int getId() { return id; }
    public int getDeckId() { return deckId; }
    public String getFront() { return front; }
    public String getBack() { return back; }
    public int getInterval() { return interval; }
    public boolean isLearned() { return learned; }
    // Setters
    public void setInterval(int interval) { this.interval = interval; }
    public void setLearned(boolean learned) { this.learned = learned; }
}