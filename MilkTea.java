import java.awt.Color;

public class MilkTea extends MenuItem {
    /** Creates a milk tea product using the shared MenuItem details. */
    public MilkTea(String id, String name, double price, String imageFile) {
        super(id, name, price, imageFile);
    }

    /** Returns the category used by the menu filter and product card. */
    @Override
    public String getCategory() {
        return "Milk Tea";
    }

    /** Returns the color used to identify milk tea in the menu. */
    @Override
    public Color getCategoryColor() {
        return new Color(48, 102, 166);
    }
}
