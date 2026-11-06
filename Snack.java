import java.awt.Color;

public class Snack extends MenuItem {
    /** Creates a snack product using the shared MenuItem details. */
    public Snack(String id, String name, double price, String imageFile) {
        super(id, name, price, imageFile);
    }

    /** Returns the category used by the Snacks & Bites sidebar filter. */
    @Override
    public String getCategory() {
        return "Snacks & Bites";
    }

    /** Returns the color used to identify snacks in the menu. */
    @Override
    public Color getCategoryColor() {
        return new Color(189, 111, 43);
    }
}
