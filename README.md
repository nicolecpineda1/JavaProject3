# JavaCup Point of Sale

A warm caramel-and-cream themed Java Swing point-of-sale app inspired by the
Snacks & Bites category color, with a prominent JavaCup brand and
`IMAGES/LOGO.jpg` header image. It includes image-based milk tea
and fruit tea menus, drink sugar levels (0%, 25%, 50%, 75%, and 100%), 12 oz / 16 oz
cup sizes, a sidebar dashboard and menu-category navigation, add-on selection, cart
quantity controls and item removal, Dine in / Take out order types, cash checkout
with change, receipts, recent sales, and completed-order history.
The POS menu can be searched by item name, category, or item ID, with results
combined with the selected category filter.

The app opens to a login screen. Choose **Admin** or **Employee** before entering
that account's credentials:

| Role | Username | Password |
|---|---|---|
| Admin | `BINOS` | `BINOS` |
| Employee | `NIKKI` | `NIKKI` |

The login button is centered. Employee login is locked for 30 seconds after three
consecutive incorrect attempts; Admin login remains available during an Employee lockout.

Admins can access the dashboard, sales/order history, inventory, capital, and the
**Accounts** page, but cannot create orders. The Accounts page lets the admin update
the username and password for the admin or any employee/staff account without
the old staff password, and add up to three employee/staff accounts total.
Each employee account can also be deleted from the Accounts page after confirmation.
Usernames must be unique. Password changes are optional for existing accounts;
leave both password fields blank to change only the username, or enter and confirm
a new password to change both. New employee accounts require a username and password.
Account changes apply immediately and last until the app closes; restarting restores
the defaults. The Inventory page also shows the employee-attributed sales total and
completed orders for the current app session. Admins can click the dashboard's
**MENU ITEMS** card to view an image gallery of the menu, or click an item name in
the inventory list to preview its image, price, and stock. The admin order history
lists completed orders with date/time, employee, order type, and total; selecting an
order shows its full receipt. The admin-only **CAPITAL** page tracks starting
puhunan, itemized ingredient and other expenses, total sales, net profit (sales minus
expenses), and available capital (starting capital plus sales minus expenses).
Capital and expenses are kept for the current app session only. Employees can access
the point of sale to prepare and sell orders, but cannot access the dashboard, sales
history, inventory, capital, or Accounts. **LOG OUT** returns to the login screen without
ending the current app session.

Both cup sizes currently use the listed menu price.
The admin dashboard's **Sales for** selector filters total sales, completed-order
count, and recent orders by employee, or shows all active employees together.
Deleting an employee removes them and their sales from the dashboard; the order
history still retains completed order records.
Dashboard sales and order metrics include completed orders from the current app session.
The Inventory page in the sidebar tracks menu and add-on quantities. Items start with
20 units for the current session, trigger a red low-stock alert at 5 units or fewer,
and are deducted when an order is paid. Use Update on the Inventory page to set a
quantity; items with no stock cannot be added to an order.
    
## Run

Keep the `.java` files in the project folder and all `.jpg` image assets in the
`IMAGES` subfolder. From the project folder, compile and launch the app:

```powershell
javac *.java
java Amain
```

The menu is held in an `ArrayList`; the active cart and completed orders use
`LinkedList`. These are in-memory collections, so orders are cleared when the app
closes.

## OOP concepts used

- **Encapsulation:** model fields are private and accessed through methods.
- **Inheritance:** `MilkTea`, `FruitTea`, and `AddOn` extend `MenuItem`.
- **Abstraction:** `MenuItem` is an abstract base class.
- **Polymorphism:** menu items provide their own category and category color.
