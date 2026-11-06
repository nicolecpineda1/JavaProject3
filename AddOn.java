import java.awt.Color;

public class AddOn extends MenuItem {
    /** Creates an add-on that can be added to a customized drink. */
    public AddOn(String id, String name, double price, String imageFile) {
        super(id, name, price, imageFile);
    }

    /** Returns the add-on category shown in the inventory. */
    @Override
    public String getCategory() {
        return "Add-ons";
    }

    /** Returns the color used to identify add-ons in the UI. */
    @Override
    public Color getCategoryColor() {
        return new Color(84, 104, 186);
    }
}
