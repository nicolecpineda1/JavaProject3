import java.awt.Color;

public abstract class MenuItem {
    // Shared details for drinks, snacks, and add-ons.
    private final String id;
    private final String name;
    private final double price;
    private final String imageFile;

    /** Sets the product ID, name, price, and image file name. */
    protected MenuItem(String id, String name, double price, String imageFile) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageFile = imageFile;
    }

    /** Returns the unique ID used to track stock. */
    public String getId() {
        return id;
    }

    /** Returns the name shown in the UI and on receipts. */
    public String getName() {
        return name;
    }

    /** Returns the price per unit. */
    public double getPrice() {
        return price;
    }

    /** Returns the product image path relative to the project folder. */
    public String getImageFile() {
        return imageFile;
    }

    /** Requires each product type to provide its category. */
    public abstract String getCategory();

    /** Requires each product type to provide its menu category color. */
    public abstract Color getCategoryColor();
}
