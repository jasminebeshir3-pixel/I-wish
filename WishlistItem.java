package org.example.person3;

/** One row from the current user's own wish list ("wishlist_items" joined with "items"). */
public class WishlistItem {
    private final int id;              // wishlist_items.id — needed for UPDATE / DELETE
    private final String name;         // the catalog item's name
    private final double price;        // target amount the user wants for this item
    private final double amountRaised; // total contributed so far
    private final boolean completed;   // true once amountRaised >= price

    public WishlistItem(int id, String name, double price, double amountRaised, boolean completed) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.amountRaised = amountRaised;
        this.completed = completed;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public double getAmountRaised() { return amountRaised; }
    public boolean isCompleted() { return completed; }
}
