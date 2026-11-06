import java.util.ArrayList;
import java.util.List;

public class CartItem {
    // Product options determine whether matching items can share a cart row.
    private final MenuItem product;
    private final ArrayList<AddOn> addOns;
    private final String sugarLevel;
    private final String cupSize;
    private int quantity;

    /** Creates a cart item without drink customizations. */
    public CartItem(MenuItem product, List<AddOn> addOns) {
        this(product, addOns, "N/A", "N/A");
    }

    /** Creates a cart item with its selected add-ons, sugar level, and cup size. */
    public CartItem(MenuItem product, List<AddOn> addOns, String sugarLevel, String cupSize) {
        this.product = product;
        this.addOns = new ArrayList<AddOn>(addOns);
        this.sugarLevel = sugarLevel;
        this.cupSize = cupSize;
        this.quantity = 1;
    }

    /** Returns the main product in this cart row. */
    public MenuItem getProduct() {
        return product;
    }

    /** Returns a copy of the add-ons list to protect the internal list from changes. */
    public ArrayList<AddOn> getAddOns() {
        return new ArrayList<AddOn>(addOns);
    }

    /** Returns the quantity of this cart item. */
    public int getQuantity() {
        return quantity;
    }

    /** Returns the selected sugar level, or N/A for non-drink items. */
    public String getSugarLevel() {
        return sugarLevel;
    }

    /** Returns the selected cup size, or N/A for non-drink items. */
    public String getCupSize() {
        return cupSize;
    }

    /** Increases the cart item quantity by one. */
    public void increaseQuantity() {
        quantity++;
    }

    /** Decreases the quantity by one, but never below one. */
    public void decreaseQuantity() {
        if (quantity > 1) {
            quantity--;
        }
    }

    /** Calculates the unit price, including size pricing and selected add-ons. */
    public double getUnitPrice() {
        double unitPrice = product.getPrice();
        if ("12 oz".equals(cupSize)) {
            unitPrice = 69.0;
        } else if ("16 oz".equals(cupSize)) {
            unitPrice = 89.0;
        }
        for (AddOn addOn : addOns) {
            unitPrice += addOn.getPrice();
        }
        return unitPrice;
    }

    /** Calculates the unit price multiplied by the quantity. */
    public double getSubtotal() {
        return getUnitPrice() * quantity;
    }

    /** Creates a short list of selected add-ons for the UI and receipt. */
    public String getOptionsText() {
        if (addOns.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder("+ ");
        for (int i = 0; i < addOns.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(addOns.get(i).getName());
        }
        return text.toString();
    }

    /** Combines the cup size, sugar level, and add-ons into a customization label. */
    public String getCustomizationText() {
        StringBuilder text = new StringBuilder();
        if (!"N/A".equals(cupSize)) {
            text.append(cupSize).append(" · Sugar ").append(sugarLevel);
        }
        if (!addOns.isEmpty()) {
            if (text.length() > 0) {
                text.append(" · ");
            }
            text.append(getOptionsText());
        }
        return text.length() == 0 ? "No add-ons" : text.toString();
    }

    /** Checks the product and add-ons using default values for drink options. */
    public boolean matches(MenuItem candidate, List<AddOn> candidateAddOns) {
        return matches(candidate, candidateAddOns, "N/A", "N/A");
    }

    /** Checks whether the product and all selected options match exactly. */
    public boolean matches(MenuItem candidate, List<AddOn> candidateAddOns,
            String candidateSugarLevel, String candidateCupSize) {
        if (!product.getId().equals(candidate.getId()) || addOns.size() != candidateAddOns.size()) {
            return false;
        }
        if (!sugarLevel.equals(candidateSugarLevel) || !cupSize.equals(candidateCupSize)) {
            return false;
        }
        for (int i = 0; i < addOns.size(); i++) {
            if (!addOns.get(i).getId().equals(candidateAddOns.get(i).getId())) {
                return false;
            }
        }
        return true;
    }
}
