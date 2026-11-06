import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Sale {
    // Date and time format used in the order history.
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private final String receipt;
    private final double total;
    private final String orderType;
    private final String employeeName;
    private final LocalDateTime completedAt;

    /** Stores the receipt, total, order type, employee, and completion time. */
    public Sale(String receipt, double total, String orderType, String employeeName) {
        this.receipt = receipt;
        this.total = total;
        this.orderType = orderType;
        this.employeeName = employeeName;
        this.completedAt = LocalDateTime.now();
    }

    /** Returns the receipt text for this order. */
    public String getReceipt() {
        return receipt;
    }

    /** Returns the total recorded for this sale. */
    public double getTotal() {
        return total;
    }

    /** Returns whether the order is dine-in or take-out. */
    public String getOrderType() {
        return orderType;
    }

    /** Returns the employee who completed this sale. */
    public String getEmployeeName() {
        return employeeName;
    }

    /** Returns the completion time as readable text. */
    public String getCompletedAtText() {
        return completedAt.format(DATE_FORMAT);
    }
}
