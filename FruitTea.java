import java.awt.Color;

public class FruitTea extends MenuItem {
    /** Creates a fruit tea product using the shared MenuItem details. */
    public FruitTea(String id, String name, double price, String imageFile) {
        super(id, name, price, imageFile);
    }

    /** Returns the category used by the menu filter and product card. */
    @Override
    public String getCategory() {
        return "Fruit Tea";
    }

    /** Returns the color used to identify fruit tea in the menu. */
    @Override
    public Color getCategoryColor() {
        return new Color(45, 132, 177);
    }
}
