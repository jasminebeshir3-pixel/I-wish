package org.example.person3;

/** One row from the admin-managed catalog (the "items" table). */
public class CatalogItem {
    private final int id;
    private final String name;
    private final String description;
    private final double defaultPrice;

    public CatalogItem(int id, String name, String description, double defaultPrice) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultPrice = defaultPrice;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getDefaultPrice() { return defaultPrice; }

    /** Used by the JComboBox in AddEditItemDialog so items show up nicely. */
    @Override
    public String toString() {
        return name + " ($" + defaultPrice + ")";
    }
}
