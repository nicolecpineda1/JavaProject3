import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

public final class MilkTeaPOS extends JFrame {
    private static final long serialVersionUID = 1L;

    // Colors and default values shared across the POS screen.
    private static final Color BACKGROUND = new Color(250, 247, 241);
    private static final Color NAVY = new Color(78, 45, 25);
    private static final Color BLUE = new Color(189, 111, 43);
    private static final Color PALE_BLUE = new Color(252, 243, 230);
    private static final Color BORDER = new Color(232, 215, 192);
    private static final Color MUTED = new Color(128, 103, 78);
    private static final Color ALERT_RED = new Color(190, 40, 50);
    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int DEFAULT_INITIAL_STOCK = 20;
    private static final String ALL_EMPLOYEES_FILTER = "All employees";
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH"));
    private static final DateTimeFormatter EXPENSE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    // Data collections for the menu, inventory, cart, and completed orders.
    private final ArrayList<MenuItem> menuItems = new ArrayList<MenuItem>();
    private final ArrayList<AddOn> availableAddOns = new ArrayList<AddOn>();
    private final Map<String, Integer> stockByItemId = new HashMap<String, Integer>();
    private final LinkedList<CartItem> cartItems = new LinkedList<CartItem>();
    private final LinkedList<Sale> completedOrders = new LinkedList<Sale>();
    private final ArrayList<CapitalExpense> capitalExpenses = new ArrayList<CapitalExpense>();
    private final JPanel menuGrid = new JPanel(new GridLayout(0, 3, 14, 14));
    private final JPanel filterButtons = new JPanel();
    private final ArrayList<JButton> categoryButtons = new ArrayList<JButton>();
    private final JPanel cartContents = new JPanel();
    private final JTextField menuSearchField = new JTextField();
    private final JPanel pageContainer = new JPanel(new CardLayout());
    private final JPanel dashboardPanel = new JPanel(new BorderLayout(0, 18));
    private final JPanel inventoryPanel = new JPanel(new BorderLayout(0, 14));
    private final JPanel capitalPanel = new JPanel(new BorderLayout(0, 14));
    private final EnumMap<LoginFrame.UserRole, JTextField> accountUsernameFields =
            new EnumMap<LoginFrame.UserRole, JTextField>(LoginFrame.UserRole.class);
    private final EnumMap<LoginFrame.UserRole, JPasswordField> accountPasswordFields =
            new EnumMap<LoginFrame.UserRole, JPasswordField>(LoginFrame.UserRole.class);
    private final EnumMap<LoginFrame.UserRole, JPasswordField> accountConfirmPasswordFields =
            new EnumMap<LoginFrame.UserRole, JPasswordField>(LoginFrame.UserRole.class);
    private final ArrayList<EmployeeAccountEditor> employeeAccountEditors =
            new ArrayList<EmployeeAccountEditor>();
    private final JPanel employeeAccountList = new JPanel();
    private final JLabel employeeAccountCountLabel = new JLabel();
    private final JComboBox<EmployeeFilterOption> dashboardEmployeeFilter =
            new JComboBox<EmployeeFilterOption>();
    private final JLabel totalLabel = new JLabel();
    private final Runnable logoutAction;
    private final LoginFrame.AccountManagementListener accountManagementListener;

    // UI components that need updates while the app is running.
    private JButton historyButton;
    private JButton logoutButton;
    private JButton dashboardButton;
    private JButton pointOfSaleButton;
    private JButton inventoryButton;
    private JButton capitalButton;
    private JButton accountsButton;
    private JButton addEmployeeAccountButton;
    private JPanel categorySection;
    private JToggleButton dineInButton;
    private JToggleButton takeOutButton;
    private String selectedCategory = "All";
    private String selectedOrderType = "Take out";
    private LoginFrame.UserRole currentRole;
    private String currentUsername;
    private int nextReceiptNumber = 1;
    private double startingCapital;
    private boolean updatingDashboardEmployeeFilter;

    private static final class EmployeeFilterOption {
        private final String username;

        private EmployeeFilterOption(String username) {
            this.username = username;
        }

        @Override
        public String toString() {
            return username == null ? ALL_EMPLOYEES_FILTER : username;
        }
    }

    private static final class EmployeeAccountEditor {
        private final JTextField usernameField;
        private final JPasswordField passwordField;
        private final JPasswordField confirmPasswordField;

        private EmployeeAccountEditor(
                JTextField usernameField,
                JPasswordField passwordField,
                JPasswordField confirmPasswordField) {
            this.usernameField = usernameField;
            this.passwordField = passwordField;
            this.confirmPasswordField = confirmPasswordField;
        }
    }

    private static final class CapitalExpense {
        private final String category;
        private final String description;
        private final double amount;
        private final LocalDateTime recordedAt;

        private CapitalExpense(String category, String description, double amount) {
            this.category = category;
            this.description = description;
            this.amount = amount;
            this.recordedAt = LocalDateTime.now();
        }
    }

    /**
     * Sets up the window, menu data, and main POS panels.
     * Builds the screen and opens the page allowed for the signed-in role.
     */
    public MilkTeaPOS(LoginFrame.UserRole userRole, String username, Runnable logoutAction,
            LoginFrame.AccountManagementListener accountManagementListener) {
        if (userRole == null || username == null || username.trim().isEmpty()
                || logoutAction == null || accountManagementListener == null) {
            throw new IllegalArgumentException(
                    "A user role, username, logout action, and account manager are required.");
        }
        this.currentRole = userRole;
        this.currentUsername = username;
        this.logoutAction = logoutAction;
        this.accountManagementListener = accountManagementListener;
        dashboardEmployeeFilter.addActionListener(event -> {
            if (!updatingDashboardEmployeeFilter) {
                refreshDashboard();
            }
        });
        setTitle("JavaCup | Point of Sale");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 680));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        getContentPane().setBackground(BACKGROUND);
        loadMenu();
        buildInterface();
        refreshMenu();
        refreshCart();
    }

    /** Updates page access when another account signs in without ending the app session. */
    public void setUserRole(LoginFrame.UserRole userRole, String username) {
        if (userRole == null || username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("A user role and username are required.");
        }
        currentRole = userRole;
        currentUsername = username;
        dashboardButton.setVisible(isAdmin());
        inventoryButton.setVisible(isAdmin());
        capitalButton.setVisible(isAdmin());
        accountsButton.setVisible(isAdmin());
        historyButton.setVisible(isAdmin());
        pointOfSaleButton.setVisible(!isAdmin());
        categorySection.setVisible(!isAdmin());
        showPage(isAdmin() ? "inventory" : "pos");
    }

    private boolean isAdmin() {
        return currentRole == LoginFrame.UserRole.ADMIN;
    }

    /**
     * Adds products to the menu and assigns their starting stock.
     * Each item starts with DEFAULT_INITIAL_STOCK units.
     */
    private void loadMenu() {
        menuItems.add(new MilkTea("MT01", "Hokkaido", 89, "IMAGES/HOKAIDO.jpg"));
        menuItems.add(new MilkTea("MT02", "Okinawa", 89, "IMAGES/OKINAWA.jpg"));
        menuItems.add(new MilkTea("MT03", "Wintermelon", 85, "IMAGES/WintermelonC.jpg"));
        menuItems.add(new MilkTea("MT04", "Taro", 85, "IMAGES/TARO.jpg"));
        menuItems.add(new MilkTea("MT05", "Matcha", 95, "IMAGES/Matcha.jpg"));
        menuItems.add(new MilkTea("MT06", "Dark Chocolate", 95, "IMAGES/Darkchoco.jpg"));
        menuItems.add(new MilkTea("MT07", "Cookies & Cream", 95, "IMAGES/CNC.jpg"));
        menuItems.add(new MilkTea("MT08", "Cheesecake", 99, "IMAGES/Cheesecake.jpg"));
        menuItems.add(new MilkTea("MT09", "Cream Cheese", 99, "IMAGES/Creamcheese.jpg"));

        menuItems.add(new FruitTea("FT01", "Mango", 85, "IMAGES/MANGO.jpg"));
        menuItems.add(new FruitTea("FT02", "Strawberry", 89, "IMAGES/Strawberry.jpg"));
        menuItems.add(new FruitTea("FT03", "Lychee", 85, "IMAGES/LYCHEE.jpg"));
        menuItems.add(new FruitTea("FT04", "Peach", 85, "IMAGES/peach.jpg"));
        menuItems.add(new FruitTea("FT05", "Green Apple", 85, "IMAGES/GreenAplle.jpg"));

        menuItems.add(new Snack("SN01", "Cake Slice", 80, "IMAGES/cakeslic.jpg"));
        menuItems.add(new Snack("SN02", "Pancake", 75, "IMAGES/pancake.jpg"));
        menuItems.add(new Snack("SN03", "Churros", 70, "IMAGES/churros.jpg"));
        menuItems.add(new Snack("SN04", "Waffles", 100, "IMAGES/waffles.jpg"));
        menuItems.add(new Snack("SN05", "Nachos", 90, "IMAGES/NCHOS.jpg"));
        menuItems.add(new Snack("SN06", "Mojos", 85, "IMAGES/MOJOS.jpg"));
        menuItems.add(new Snack("SN07", "Fries", 50, "IMAGES/fries.jpg"));
        menuItems.add(new Snack("SN08", "Popper Fries", 75, "IMAGES/popersfries.jpg"));

        AddOn pearls = new AddOn("AO01", "Pearl", 15, "IMAGES/Pearl.jpg");
        AddOn nata = new AddOn("AO02", "Nata de Coco", 15, "IMAGES/Nata.jpg");
        AddOn creamCheese = new AddOn("AO03", "Cream Cheese", 15, "IMAGES/Creamcheese.jpg");
        availableAddOns.add(pearls);
        availableAddOns.add(nata);
        availableAddOns.add(creamCheese);
        menuItems.add(pearls);
        menuItems.add(nata);
        menuItems.add(creamCheese);
        for (MenuItem item : menuItems) {
            stockByItemId.put(item.getId(), DEFAULT_INITIAL_STOCK);
        }
    }

    /**
     * Builds the main app layout: header at the top,
     * sidebar on the left, and page content in the center.
     */
    private void buildInterface() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(18, 20, 18, 20));
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setOpaque(false);
        content.add(buildSidebar(), BorderLayout.WEST);

        pageContainer.setOpaque(false);
        pageContainer.add(dashboardPanel, "dashboard");
        pageContainer.add(buildPointOfSalePanel(), "pos");
        pageContainer.add(inventoryPanel, "inventory");
        pageContainer.add(capitalPanel, "capital");
        pageContainer.add(buildAccountsPanel(), "accounts");
        content.add(pageContainer, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        dashboardButton.setVisible(isAdmin());
        inventoryButton.setVisible(isAdmin());
        capitalButton.setVisible(isAdmin());
        accountsButton.setVisible(isAdmin());
        pointOfSaleButton.setVisible(!isAdmin());
        categorySection.setVisible(!isAdmin());
        refreshDashboard();
        showPage(isAdmin() ? "inventory" : "pos");
    }

    /**
     * Builds sidebar navigation for the dashboard, POS, inventory,
     * and menu category filters.
     */
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(18, 14, 18, 14)));
        sidebar.setPreferredSize(new Dimension(205, 0));

        JLabel brand = new JLabel("JavaCup");
        brand.setFont(new Font("SansSerif", Font.BOLD, 22));
        brand.setForeground(NAVY);
        brand.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(22));
        addSidebarSectionLabel(sidebar, "WORKSPACE");

        dashboardButton = createSidebarButton("Dashboard");
        dashboardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showPage("dashboard");
            }
        });
        sidebar.add(dashboardButton);

        pointOfSaleButton = createSidebarButton("Point of Sale");
        pointOfSaleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showPage("pos");
            }
        });
        sidebar.add(pointOfSaleButton);
        inventoryButton = createSidebarButton("Inventory");
        inventoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showPage("inventory");
            }
        });
        sidebar.add(inventoryButton);
        capitalButton = createSidebarButton("CAPITAL");
        capitalButton.addActionListener(event -> showPage("capital"));
        sidebar.add(capitalButton);
        accountsButton = createSidebarButton("Accounts");
        accountsButton.addActionListener(event -> showPage("accounts"));
        sidebar.add(accountsButton);
        categorySection = new JPanel();
        categorySection.setOpaque(false);
        categorySection.setLayout(new BoxLayout(categorySection, BoxLayout.Y_AXIS));
        categorySection.add(Box.createVerticalStrut(20));
        addSidebarSectionLabel(categorySection, "MENU CATEGORIES");
        filterButtons.setOpaque(false);
        filterButtons.setLayout(new BoxLayout(filterButtons, BoxLayout.Y_AXIS));
        addFilterButton(filterButtons, "All");
        addFilterButton(filterButtons, "Milk Tea");
        addFilterButton(filterButtons, "Fruit Tea");
        addFilterButton(filterButtons, "Snacks & Bites");
        categorySection.add(filterButtons);
        sidebar.add(categorySection);
        sidebar.add(Box.createVerticalGlue());

        JLabel note = new JLabel("<html>Sales and orders are kept<br>for this session.</html>");
        note.setFont(new Font("SansSerif", Font.PLAIN, 10));
        note.setForeground(MUTED);
        note.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(note);
        return sidebar;
    }

    /** Adds a small heading to separate sidebar sections. */
    private void addSidebarSectionLabel(JPanel sidebar, String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setForeground(MUTED);
        label.setBorder(new EmptyBorder(0, 8, 8, 0));
        label.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(label);
    }

    private JPanel buildAccountsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Account management");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(NAVY);
        JLabel subtitle = new JLabel(
                "Update the login username and password for each account.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        heading.add(title);
        heading.add(Box.createVerticalStrut(5));
        heading.add(subtitle);
        panel.add(heading, BorderLayout.NORTH);

        JPanel accountCards = new JPanel(new GridLayout(1, 2, 16, 0));
        accountCards.setOpaque(false);
        accountCards.add(createAccountCard(LoginFrame.UserRole.ADMIN));
        accountCards.add(createEmployeeAccountsPanel());
        panel.add(accountCards, BorderLayout.CENTER);

        JLabel note = new JLabel(
                "Account changes apply immediately and remain active until the app closes.");
        note.setFont(new Font("SansSerif", Font.PLAIN, 12));
        note.setForeground(MUTED);
        note.setBorder(new EmptyBorder(12, 0, 0, 0));
        panel.add(note, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createAccountCard(LoginFrame.UserRole role) {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(22, 22, 22, 22)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel roleTitle = new JLabel(role == LoginFrame.UserRole.ADMIN
                ? "Admin account" : "Employee / Staff account");
        roleTitle.setFont(new Font("SansSerif", Font.BOLD, 19));
        roleTitle.setForeground(NAVY);
        roleTitle.setAlignmentX(LEFT_ALIGNMENT);
        card.add(roleTitle);
        card.add(Box.createVerticalStrut(8));

        JLabel roleDescription = new JLabel(role == LoginFrame.UserRole.ADMIN
                ? "Administrator access" : "Point-of-sale staff access");
        roleDescription.setFont(new Font("SansSerif", Font.PLAIN, 12));
        roleDescription.setForeground(MUTED);
        roleDescription.setAlignmentX(LEFT_ALIGNMENT);
        card.add(roleDescription);
        card.add(Box.createVerticalStrut(22));

        JTextField usernameField = new JTextField(
                accountManagementListener.getUsername(role), 18);
        JPasswordField passwordField = new JPasswordField(18);
        JPasswordField confirmPasswordField = new JPasswordField(18);
        styleAccountField(usernameField);
        styleAccountField(passwordField);
        styleAccountField(confirmPasswordField);
        accountUsernameFields.put(role, usernameField);
        accountPasswordFields.put(role, passwordField);
        accountConfirmPasswordFields.put(role, confirmPasswordField);

        addAccountField(card, "Username", usernameField);
        addAccountField(card, "New password (optional)", passwordField);
        addAccountField(card, "Confirm new password", confirmPasswordField);

        JButton saveButton = createAccountActionButton("SAVE ACCOUNT");
        saveButton.addActionListener(event -> saveAccountCredentials(role));
        card.add(Box.createVerticalStrut(5));
        card.add(saveButton);
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel createEmployeeAccountsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(18, 18, 18, 18)));

        JPanel heading = new JPanel(new BorderLayout(0, 8));
        heading.setOpaque(false);
        JPanel headingText = new JPanel();
        headingText.setOpaque(false);
        headingText.setLayout(new BoxLayout(headingText, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Employee / Staff accounts");
        title.setFont(new Font("SansSerif", Font.BOLD, 19));
        title.setForeground(NAVY);
        JLabel description = new JLabel("Point-of-sale staff access · up to "
                + LoginFrame.MAX_EMPLOYEE_ACCOUNTS + " accounts");
        description.setFont(new Font("SansSerif", Font.PLAIN, 12));
        description.setForeground(MUTED);
        headingText.add(title);
        headingText.add(Box.createVerticalStrut(5));
        headingText.add(description);
        heading.add(headingText, BorderLayout.CENTER);
        employeeAccountCountLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        employeeAccountCountLabel.setForeground(MUTED);
        heading.add(employeeAccountCountLabel, BorderLayout.SOUTH);
        panel.add(heading, BorderLayout.NORTH);

        employeeAccountList.setOpaque(false);
        employeeAccountList.setLayout(new BoxLayout(employeeAccountList, BoxLayout.Y_AXIS));
        JScrollPane accountScrollPane = new JScrollPane(employeeAccountList);
        accountScrollPane.setBorder(BorderFactory.createEmptyBorder());
        accountScrollPane.getViewport().setBackground(Color.WHITE);
        accountScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(accountScrollPane, BorderLayout.CENTER);

        addEmployeeAccountButton = createAccountActionButton("ADD EMPLOYEE ACCOUNT");
        addEmployeeAccountButton.addActionListener(event -> addEmployeeAccount());
        panel.add(addEmployeeAccountButton, BorderLayout.SOUTH);
        rebuildEmployeeAccountEditors();
        return panel;
    }

    private void rebuildEmployeeAccountEditors() {
        employeeAccountEditors.clear();
        employeeAccountList.removeAll();
        List<String> usernames = accountManagementListener.getEmployeeUsernames();
        for (int index = 0; index < usernames.size(); index++) {
            JPanel card = new JPanel();
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    new EmptyBorder(12, 12, 12, 12)));
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

            JLabel title = new JLabel("Employee / Staff account " + (index + 1));
            title.setFont(new Font("SansSerif", Font.BOLD, 15));
            title.setForeground(NAVY);
            title.setAlignmentX(LEFT_ALIGNMENT);
            card.add(title);
            card.add(Box.createVerticalStrut(10));

            JTextField usernameField = new JTextField(usernames.get(index), 18);
            JPasswordField passwordField = new JPasswordField(18);
            JPasswordField confirmPasswordField = new JPasswordField(18);
            styleAccountField(usernameField);
            styleAccountField(passwordField);
            styleAccountField(confirmPasswordField);
            employeeAccountEditors.add(new EmployeeAccountEditor(
                    usernameField, passwordField, confirmPasswordField));
            addAccountField(card, "Username", usernameField);
            addAccountField(card, "New password (optional)", passwordField);
            addAccountField(card, "Confirm new password", confirmPasswordField);

            JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
            actions.setOpaque(false);
            JButton saveButton = createAccountActionButton("SAVE ACCOUNT");
            final int accountIndex = index;
            saveButton.addActionListener(event -> saveEmployeeCredentials(accountIndex));
            JButton deleteButton = createDeleteAccountButton();
            deleteButton.addActionListener(event -> deleteEmployeeAccount(accountIndex));
            actions.add(saveButton);
            actions.add(deleteButton);
            actions.setAlignmentX(LEFT_ALIGNMENT);
            card.add(actions);
            card.setAlignmentX(LEFT_ALIGNMENT);
            employeeAccountList.add(card);
            employeeAccountList.add(Box.createVerticalStrut(10));
        }
        employeeAccountCountLabel.setText(usernames.size() + " of "
                + LoginFrame.MAX_EMPLOYEE_ACCOUNTS + " accounts");
        addEmployeeAccountButton.setEnabled(usernames.size() < LoginFrame.MAX_EMPLOYEE_ACCOUNTS);
        employeeAccountList.revalidate();
        employeeAccountList.repaint();
    }

    private JButton createAccountActionButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(BLUE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        return button;
    }

    private JButton createDeleteAccountButton() {
        JButton button = new JButton("DELETE ACCOUNT");
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(ALERT_RED);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void addAccountField(JPanel parent, String labelText, JTextField field) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(NAVY);
        label.setAlignmentX(LEFT_ALIGNMENT);
        field.setAlignmentX(LEFT_ALIGNMENT);
        parent.add(label);
        parent.add(Box.createVerticalStrut(5));
        parent.add(field);
        parent.add(Box.createVerticalStrut(13));
    }

    private void styleAccountField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(260, 42));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setBackground(new Color(255, 252, 247));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 205, 178)),
                new EmptyBorder(8, 10, 8, 10)));
    }

    private void saveAccountCredentials(LoginFrame.UserRole role) {
        String username = accountUsernameFields.get(role).getText().trim();
        String password = new String(accountPasswordFields.get(role).getPassword());
        String confirmPassword =
                new String(accountConfirmPasswordFields.get(role).getPassword());
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Username cannot be empty.",
                    "Invalid account details", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (password.isEmpty() != confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Enter and confirm a new password, or leave both password fields blank "
                            + "to keep the current password.",
                    "Invalid account details", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this,
                    "The new passwords do not match.",
                    "Invalid account details", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            accountManagementListener.updateCredentials(role, username, password);
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(),
                    "Account not updated", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (role == currentRole) {
            currentUsername = username;
        }
        refreshAccountUsernames();
        accountPasswordFields.get(role).setText("");
        accountConfirmPasswordFields.get(role).setText("");
        JOptionPane.showMessageDialog(this,
                "The " + (role == LoginFrame.UserRole.ADMIN ? "admin" : "employee")
                        + " account was updated"
                        + (password.isEmpty() ? "." : ", including its password."),
                "Account updated", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveEmployeeCredentials(int accountIndex) {
        EmployeeAccountEditor editor = employeeAccountEditors.get(accountIndex);
        String username = editor.usernameField.getText().trim();
        String password = new String(editor.passwordField.getPassword());
        String confirmPassword = new String(editor.confirmPasswordField.getPassword());
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Username cannot be empty.",
                    "Invalid account details", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (password.isEmpty() != confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Enter and confirm a new password, or leave both password fields blank "
                            + "to keep the current password.",
                    "Invalid account details", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this,
                    "The new passwords do not match.",
                    "Invalid account details", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            accountManagementListener.updateEmployeeCredentials(
                    accountIndex, username, password);
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(),
                    "Account not updated", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshAccountUsernames();
        editor.passwordField.setText("");
        editor.confirmPasswordField.setText("");
        JOptionPane.showMessageDialog(this,
                "The employee account was updated"
                        + (password.isEmpty() ? "." : ", including its password."),
                "Account updated", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addEmployeeAccount() {
        List<String> usernames = accountManagementListener.getEmployeeUsernames();
        if (usernames.size() >= LoginFrame.MAX_EMPLOYEE_ACCOUNTS) {
            JOptionPane.showMessageDialog(this,
                    "The maximum of " + LoginFrame.MAX_EMPLOYEE_ACCOUNTS
                            + " employee accounts has been reached.",
                    "Account limit reached", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JTextField usernameField = new JTextField(18);
        JPasswordField passwordField = new JPasswordField(18);
        JPasswordField confirmPasswordField = new JPasswordField(18);
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.add(new JLabel("Username"));
        form.add(usernameField);
        form.add(new JLabel("Password"));
        form.add(passwordField);
        form.add(new JLabel("Confirm password"));
        form.add(confirmPasswordField);
        int choice = JOptionPane.showConfirmDialog(this, form,
                "Add employee account", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "A username and password are required.",
                    "Invalid account details", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this,
                    "The passwords do not match.",
                    "Invalid account details", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            accountManagementListener.addEmployeeAccount(username, password);
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(),
                    "Account not added", JOptionPane.ERROR_MESSAGE);
            return;
        }
        rebuildEmployeeAccountEditors();
        JOptionPane.showMessageDialog(this,
                "The employee account was added.",
                "Account added", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteEmployeeAccount(int accountIndex) {
        List<String> usernames = accountManagementListener.getEmployeeUsernames();
        if (accountIndex < 0 || accountIndex >= usernames.size()) {
            JOptionPane.showMessageDialog(this,
                    "That employee account is no longer available. Refresh the Accounts page.",
                    "Account not deleted", JOptionPane.ERROR_MESSAGE);
            rebuildEmployeeAccountEditors();
            return;
        }
        String username = usernames.get(accountIndex);
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete the employee account for \"" + username + "\"?\n"
                        + "This account will no longer be able to log in.",
                "Confirm account deletion", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            accountManagementListener.deleteEmployeeAccount(accountIndex);
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(),
                    "Account not deleted", JOptionPane.ERROR_MESSAGE);
            return;
        }
        rebuildEmployeeAccountEditors();
        JOptionPane.showMessageDialog(this,
                "The employee account \"" + username + "\" was deleted.",
                "Account deleted", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshAccountUsernames() {
        JTextField adminUsernameField = accountUsernameFields.get(LoginFrame.UserRole.ADMIN);
        if (adminUsernameField != null) {
            adminUsernameField.setText(
                    accountManagementListener.getUsername(LoginFrame.UserRole.ADMIN));
        }
        List<String> usernames = accountManagementListener.getEmployeeUsernames();
        if (usernames.size() != employeeAccountEditors.size()) {
            rebuildEmployeeAccountEditors();
            return;
        }
        for (int index = 0; index < usernames.size(); index++) {
            employeeAccountEditors.get(index).usernameField.setText(usernames.get(index));
        }
    }

    /** Creates a consistently styled button for the sidebar. */
    private JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMargin(new Insets(10, 12, 10, 8));
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setPreferredSize(new Dimension(175, 42));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        return button;
    }

    /**
     * Combines the menu and current order on the POS page:
     * the menu is in the center and the cart is on the right.
     */
    private JPanel buildPointOfSalePanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setOpaque(false);
        panel.add(buildMenuPanel(), BorderLayout.CENTER);
        JPanel orderPanel = buildOrderPanel();
        orderPanel.setPreferredSize(new Dimension(350, 0));
        panel.add(orderPanel, BorderLayout.EAST);
        return panel;
    }

    /**
     * Shows the selected page and updates the navigation style.
     * Refreshes the dashboard or inventory when either page is opened.
     */
    private void showPage(String page) {
        if (isAdmin() && !"dashboard".equals(page) && !"inventory".equals(page)
                && !"capital".equals(page) && !"accounts".equals(page)) {
            page = "inventory";
        } else if (!isAdmin() && !"pos".equals(page)) {
            page = "pos";
        }
        ((CardLayout) pageContainer.getLayout()).show(pageContainer, page);
        styleSidebarButton(dashboardButton, "dashboard".equals(page));
        styleSidebarButton(pointOfSaleButton, "pos".equals(page));
        styleSidebarButton(inventoryButton, "inventory".equals(page));
        styleSidebarButton(capitalButton, "capital".equals(page));
        styleSidebarButton(accountsButton, "accounts".equals(page));
        updateInventoryAlert();
        if ("dashboard".equals(page)) {
            refreshDashboard();
        } else if ("inventory".equals(page)) {
            refreshInventory();
        } else if ("capital".equals(page)) {
            refreshCapital();
        } else if ("accounts".equals(page)) {
            refreshAccountUsernames();
        }
    }

    /** Sets the sidebar button colors based on whether its page is selected. */
    private void styleSidebarButton(JButton button, boolean selected) {
        styleButton(button, selected ? PALE_BLUE : Color.WHITE,
                selected ? BLUE : NAVY);
    }

    /**
     * Shows a low-stock count on the Inventory button when needed.
     */
    private void updateInventoryAlert() {
        if (inventoryButton == null) {
            return;
        }
        int lowStockCount = getLowStockCount();
        if (lowStockCount > 0) {
            inventoryButton.setText("<html>Inventory <span style='color:#BE2832'>"
                    + "&#9679; " + lowStockCount + " LOW</span></html>");
            inventoryButton.setToolTipText(lowStockCount
                    + " item(s) at or below " + LOW_STOCK_THRESHOLD + " in stock");
        } else {
            inventoryButton.setText("Inventory");
            inventoryButton.setToolTipText("View and update item stock");
        }
    }

    /** Counts menu items whose stock is at or below the low-stock threshold. */
    private int getLowStockCount() {
        int count = 0;
        for (MenuItem item : menuItems) {
            if (getStock(item) <= LOW_STOCK_THRESHOLD) {
                count++;
            }
        }
        return count;
    }

    /** Gets an item's current stock using its unique ID. */
    private int getStock(MenuItem item) {
        Integer stock = stockByItemId.get(item.getId());
        return stock == null ? 0 : stock.intValue();
    }

    /**
     * Rebuilds the inventory list using the current menu and stock data.
     */
    private void refreshInventory() {
        if (!isAdmin()) {
            return;
        }
        inventoryPanel.removeAll();
        inventoryPanel.setOpaque(false);
        inventoryPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Inventory");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(NAVY);
        JLabel subtitle = new JLabel("Track menu and add-on stock. Low stock is "
                + LOW_STOCK_THRESHOLD + " or fewer.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        heading.add(title);
        heading.add(Box.createVerticalStrut(5));
        heading.add(subtitle);
        inventoryPanel.add(heading, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setBackground(Color.WHITE);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setBorder(new EmptyBorder(8, 16, 8, 16));
        for (final MenuItem item : menuItems) {
            rows.add(createInventoryRow(item));
        }
        JScrollPane scroll = new JScrollPane(rows);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        JPanel salesPanel = createEmployeeSalesPanel();
        JSplitPane inventoryAndSales = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, scroll, salesPanel);
        inventoryAndSales.setResizeWeight(0.58);
        inventoryAndSales.setBorder(BorderFactory.createEmptyBorder());
        inventoryPanel.add(inventoryAndSales, BorderLayout.CENTER);
        updateInventoryAlert();
        inventoryPanel.revalidate();
        inventoryPanel.repaint();
    }

    /** Rebuilds the admin-only capital, sales, and expense summary. */
    private void refreshCapital() {
        if (!isAdmin()) {
            return;
        }
        capitalPanel.removeAll();
        capitalPanel.setOpaque(false);
        capitalPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setOpaque(false);
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("CAPITAL");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(NAVY);
        JLabel subtitle = new JLabel("Track starting puhunan, ingredient costs, other expenses, and sales.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(5));
        titleBlock.add(subtitle);
        header.add(titleBlock, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton setStartingCapital = new JButton("Set Starting Capital");
        setStartingCapital.addActionListener(event -> promptForStartingCapital());
        JButton addExpense = new JButton("Add Expense");
        addExpense.addActionListener(event -> promptForCapitalExpense());
        JButton removeExpense = new JButton("Remove Selected");
        actions.add(setStartingCapital);
        actions.add(addExpense);
        actions.add(removeExpense);
        header.add(actions, BorderLayout.SOUTH);
        capitalPanel.add(header, BorderLayout.NORTH);

        double salesTotal = getTotalSales();
        double ingredientExpenses = getTotalExpenses("Ingredients");
        double otherExpenses = getTotalExpenses("Other expenses");
        double expensesTotal = ingredientExpenses + otherExpenses;
        double netProfit = salesTotal - expensesTotal;
        double availableCapital = startingCapital + netProfit;
        JPanel summary = new JPanel(new GridLayout(0, 4, 10, 10));
        summary.setOpaque(false);
        summary.add(createMetricCard("STARTING CAPITAL", CURRENCY.format(startingCapital),
                "Admin-entered puhunan"));
        summary.add(createMetricCard("TOTAL SALES", CURRENCY.format(salesTotal),
                "Completed employee orders"));
        summary.add(createMetricCard("INGREDIENT COSTS", CURRENCY.format(ingredientExpenses),
                "Ingredient expenses"));
        summary.add(createMetricCard("OTHER EXPENSES", CURRENCY.format(otherExpenses),
                "Other recorded gastos"));
        summary.add(createMetricCard("TOTAL EXPENSES", CURRENCY.format(expensesTotal),
                "Ingredients + other expenses"));
        summary.add(createMetricCard("NET PROFIT", CURRENCY.format(netProfit),
                "Sales minus expenses"));
        summary.add(createMetricCard("AVAILABLE CAPITAL", CURRENCY.format(availableCapital),
                "Starting capital + sales − expenses"));

        DefaultTableModel expenseModel = new DefaultTableModel(
                new Object[] {"Date", "Category", "Description", "Amount"}, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (int i = capitalExpenses.size() - 1; i >= 0; i--) {
            CapitalExpense expense = capitalExpenses.get(i);
            expenseModel.addRow(new Object[] {
                expense.recordedAt.format(EXPENSE_DATE_FORMAT),
                expense.category,
                expense.description,
                CURRENCY.format(expense.amount)
            });
        }
        JTable expenseTable = new JTable(expenseModel);
        expenseTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        expenseTable.setFillsViewportHeight(true);
        expenseTable.setRowHeight(25);
        removeExpense.addActionListener(event -> {
            if (!isAdmin()) {
                return;
            }
            int selectedRow = expenseTable.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Select an expense to remove.",
                        "No expense selected", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Remove the selected expense from the capital records?",
                    "Confirm expense removal", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            int modelRow = expenseTable.convertRowIndexToModel(selectedRow);
            int expenseIndex = capitalExpenses.size() - 1 - modelRow;
            capitalExpenses.remove(expenseIndex);
            refreshCapital();
        });

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        content.add(summary, BorderLayout.NORTH);
        JPanel expenseHistory = new JPanel(new BorderLayout(0, 8));
        expenseHistory.setBackground(Color.WHITE);
        expenseHistory.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(12, 14, 12, 14)));
        JLabel expenseTitle = new JLabel("Expense history");
        expenseTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        expenseTitle.setForeground(NAVY);
        expenseHistory.add(expenseTitle, BorderLayout.NORTH);
        JScrollPane expenseScroll = new JScrollPane(expenseTable);
        expenseScroll.setBorder(BorderFactory.createEmptyBorder());
        expenseScroll.getViewport().setBackground(Color.WHITE);
        expenseHistory.add(expenseScroll, BorderLayout.CENTER);
        content.add(expenseHistory, BorderLayout.CENTER);
        capitalPanel.add(content, BorderLayout.CENTER);
        capitalPanel.revalidate();
        capitalPanel.repaint();
    }

    private double getTotalSales() {
        double total = 0;
        for (Sale sale : completedOrders) {
            total += sale.getTotal();
        }
        return total;
    }

    private double getTotalExpenses(String category) {
        double total = 0;
        for (CapitalExpense expense : capitalExpenses) {
            if (category == null || category.equals(expense.category)) {
                total += expense.amount;
            }
        }
        return total;
    }

    private void promptForStartingCapital() {
        if (!isAdmin()) {
            return;
        }
        String input = JOptionPane.showInputDialog(this,
                "Enter the starting capital / puhunan amount:",
                String.valueOf(startingCapital));
        if (input == null) {
            return;
        }
        Double amount = parseNonNegativeAmount(input);
        if (amount == null) {
            JOptionPane.showMessageDialog(this, "Enter a valid amount that is zero or greater.",
                    "Invalid capital", JOptionPane.ERROR_MESSAGE);
            return;
        }
        startingCapital = amount.doubleValue();
        refreshCapital();
    }

    private void promptForCapitalExpense() {
        if (!isAdmin()) {
            return;
        }
        JComboBox<String> category = new JComboBox<String>(
                new String[] {"Ingredients", "Other expenses"});
        JTextField description = new JTextField(20);
        JTextField amount = new JTextField(12);
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        fields.add(new JLabel("Expense type:"));
        fields.add(category);
        fields.add(new JLabel("Description:"));
        fields.add(description);
        fields.add(new JLabel("Amount (PHP):"));
        fields.add(amount);
        int result = JOptionPane.showConfirmDialog(this, fields, "Add capital expense",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        String expenseDescription = description.getText().trim();
        Double expenseAmount = parseNonNegativeAmount(amount.getText());
        if (expenseDescription.isEmpty() || expenseAmount == null
                || expenseAmount.doubleValue() <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Enter a description and a valid amount greater than zero.",
                    "Invalid expense", JOptionPane.ERROR_MESSAGE);
            return;
        }
        capitalExpenses.add(new CapitalExpense(
                (String) category.getSelectedItem(), expenseDescription,
                expenseAmount.doubleValue()));
        refreshCapital();
    }

    private Double parseNonNegativeAmount(String input) {
        try {
            double amount = Double.parseDouble(input.trim().replace(",", ""));
            if (Double.isNaN(amount) || Double.isInfinite(amount) || amount < 0) {
                return null;
            }
            return Double.valueOf(amount);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * Creates an inventory row with the item name, category,
     * stock count, and a button to update its stock.
     */
    private JPanel createInventoryRow(final MenuItem item) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(12, 4, 12, 4)));

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(item.getName());
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        name.setForeground(NAVY);
        name.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        name.setToolTipText("Click to view this menu item's image");
        name.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (isAdmin()) {
                    showMenuItemImage(item);
                }
            }
        });
        JLabel category = new JLabel(item.getCategory());
        category.setFont(new Font("SansSerif", Font.PLAIN, 11));
        category.setForeground(MUTED);
        details.add(name);
        details.add(Box.createVerticalStrut(4));
        details.add(category);
        row.add(details, BorderLayout.CENTER);

        JPanel stockControls = new JPanel(new BorderLayout(12, 0));
        stockControls.setOpaque(false);
        final JLabel quantity = new JLabel();
        quantity.setHorizontalAlignment(SwingConstants.RIGHT);
        quantity.setFont(new Font("SansSerif", Font.BOLD, 14));
        updateInventoryQuantityLabel(quantity, item);
        stockControls.add(quantity, BorderLayout.CENTER);
        JButton update = new JButton("Update");
        update.setFont(new Font("SansSerif", Font.BOLD, 11));
        update.setForeground(BLUE);
        update.setFocusPainted(false);
        update.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        update.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (!isAdmin()) {
                    return;
                }
                String input = JOptionPane.showInputDialog(MilkTeaPOS.this,
                        "Enter the new stock quantity for " + item.getName() + ":",
                        String.valueOf(getStock(item)));
                if (input == null) {
                    return;
                }
                try {
                    int newStock = Integer.parseInt(input.trim());
                    if (newStock < 0) {
                        throw new NumberFormatException("Stock cannot be negative");
                    }
                    stockByItemId.put(item.getId(), Integer.valueOf(newStock));
                    refreshInventory();
                    refreshMenu();
                    refreshCart();
                } catch (NumberFormatException exception) {
                    JOptionPane.showMessageDialog(MilkTeaPOS.this,
                            "Enter a whole number that is zero or greater.",
                            "Invalid stock quantity", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        stockControls.add(update, BorderLayout.EAST);
        row.add(stockControls, BorderLayout.EAST);
        return row;
    }

    /** Displays a larger preview of a menu item's image and details for admins. */
    private void showMenuItemImage(MenuItem item) {
        if (!isAdmin()) {
            return;
        }
        File imageFile = new File(item.getImageFile());
        if (!imageFile.isFile()) {
            JOptionPane.showMessageDialog(this,
                    "The image for " + item.getName() + " could not be found: "
                            + item.getImageFile(),
                    "Menu image unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPanel preview = new JPanel(new BorderLayout(0, 12));
        preview.setBackground(Color.WHITE);
        JLabel image = new JLabel(loadImage(item.getImageFile(), 520, 340));
        image.setHorizontalAlignment(SwingConstants.CENTER);
        image.setPreferredSize(new Dimension(520, 340));
        image.setOpaque(true);
        image.setBackground(PALE_BLUE);
        preview.add(image, BorderLayout.CENTER);

        JLabel details = new JLabel("<html><b>" + item.getName() + "</b> &nbsp; "
                + item.getCategory() + "<br>Item ID: " + item.getId()
                + " &nbsp; Price: " + CURRENCY.format(item.getPrice())
                + " &nbsp; Stock: " + getStock(item) + "</html>");
        details.setFont(new Font("SansSerif", Font.PLAIN, 13));
        details.setForeground(NAVY);
        preview.add(details, BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this, preview, item.getName(),
                JOptionPane.PLAIN_MESSAGE);
    }

    /** Updates the stock label and color based on the low-stock threshold. */
    private void updateInventoryQuantityLabel(JLabel quantity, MenuItem item) {
        int stock = getStock(item);
        if (stock <= LOW_STOCK_THRESHOLD) {
            quantity.setText("<html><span style='color:#BE2832'>" + stock
                    + " in stock &nbsp; LOW</span></html>");
            quantity.setForeground(ALERT_RED);
        } else {
            quantity.setText(stock + " in stock");
            quantity.setForeground(NAVY);
        }
    }

    /** Shows admins the employee-attributed sales recorded during this app session. */
    private JPanel createEmployeeSalesPanel() {
        JPanel salesPanel = new JPanel(new BorderLayout(0, 8));
        salesPanel.setBackground(Color.WHITE);
        salesPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(10, 14, 10, 14)));

        double salesTotal = 0;
        for (Sale sale : completedOrders) {
            salesTotal += sale.getTotal();
        }
        JLabel summary = new JLabel("Employee sales this session: "
                + CURRENCY.format(salesTotal) + "  |  " + completedOrders.size() + " orders");
        summary.setFont(new Font("SansSerif", Font.BOLD, 14));
        summary.setForeground(NAVY);
        salesPanel.add(summary, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        if (completedOrders.isEmpty()) {
            JLabel empty = new JLabel("No employee sales recorded yet.");
            empty.setFont(new Font("SansSerif", Font.PLAIN, 12));
            empty.setForeground(MUTED);
            rows.add(empty);
        } else {
            for (int i = completedOrders.size() - 1; i >= 0; i--) {
                Sale sale = completedOrders.get(i);
                JLabel row = new JLabel(sale.getCompletedAtText() + "  |  "
                        + sale.getEmployeeName() + "  |  " + sale.getOrderType()
                        + "  |  " + CURRENCY.format(sale.getTotal()));
                row.setFont(new Font("SansSerif", Font.PLAIN, 12));
                row.setForeground(NAVY);
                row.setBorder(new EmptyBorder(4, 0, 4, 0));
                rows.add(row);
            }
        }
        JScrollPane salesScroll = new JScrollPane(rows);
        salesScroll.setBorder(BorderFactory.createEmptyBorder());
        salesScroll.getViewport().setBackground(Color.WHITE);
        salesPanel.add(salesScroll, BorderLayout.CENTER);
        return salesPanel;
    }

    /**
     * Recalculates and redraws the dashboard metrics and recent orders.
     */
    private void refreshDashboard() {
        dashboardPanel.removeAll();
        dashboardPanel.setOpaque(false);
        dashboardPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(NAVY);
        JLabel subtitle = new JLabel("A quick look at your JavaCup store.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        heading.add(title);
        heading.add(Box.createVerticalStrut(5));
        heading.add(subtitle);
        if (isAdmin()) {
            JPanel employeeFilterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            employeeFilterRow.setOpaque(false);
            JLabel employeeFilterLabel = new JLabel("Sales for:");
            employeeFilterLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            employeeFilterLabel.setForeground(NAVY);
            dashboardEmployeeFilter.setPreferredSize(new Dimension(210, 30));
            employeeFilterRow.add(employeeFilterLabel);
            employeeFilterRow.add(dashboardEmployeeFilter);
            heading.add(Box.createVerticalStrut(10));
            heading.add(employeeFilterRow);
            refreshDashboardEmployeeFilter();
        }
        dashboardPanel.add(heading, BorderLayout.NORTH);

        JPanel dashboardContent = new JPanel(new BorderLayout(0, 16));
        dashboardContent.setOpaque(false);
        JPanel metrics = new JPanel(new GridLayout(1, isAdmin() ? 3 : 4, 12, 0));
        metrics.setOpaque(false);
        String employeeFilter = getSelectedDashboardEmployee();
        List<String> activeEmployeeUsernames = accountManagementListener.getEmployeeUsernames();
        double salesTotal = 0;
        int completedOrderCount = 0;
        for (Sale sale : completedOrders) {
            if (isDashboardSaleVisible(sale, employeeFilter, activeEmployeeUsernames)) {
                salesTotal += sale.getTotal();
                completedOrderCount++;
            }
        }
        String salesDescription = employeeFilter == null
                ? "Completed orders this session" : "Sales by " + employeeFilter;
        metrics.add(createMetricCard("TOTAL SALES", CURRENCY.format(salesTotal),
                salesDescription));
        metrics.add(createMetricCard("COMPLETED ORDERS",
                String.valueOf(completedOrderCount),
                employeeFilter == null ? "Paid orders this session" : "Paid orders by employee"));
        JPanel menuItemsCard = createMetricCard("MENU ITEMS",
                String.valueOf(menuItems.size()), "Drinks, snacks, and add-ons");
        makeMenuItemsCardClickable(menuItemsCard);
        metrics.add(menuItemsCard);
        if (!isAdmin()) {
            int cartQuantity = 0;
            for (CartItem item : cartItems) {
                cartQuantity += item.getQuantity();
            }
            metrics.add(createMetricCard("CURRENT ORDER", CURRENCY.format(getOrderTotal()),
                    cartQuantity + (cartQuantity == 1 ? " item in cart" : " items in cart")));
        }
        dashboardContent.add(metrics, BorderLayout.NORTH);
        dashboardContent.add(createRecentOrdersPanel(employeeFilter), BorderLayout.CENTER);
        dashboardPanel.add(dashboardContent, BorderLayout.CENTER);
        dashboardPanel.revalidate();
        dashboardPanel.repaint();
    }

    /** Creates a dashboard card from a title, value, and description. */
    private JPanel createMetricCard(String labelText, String valueText, String detailText) {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(16, 15, 16, 15)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setForeground(MUTED);
        JLabel value = new JLabel(valueText);
        value.setFont(new Font("SansSerif", Font.BOLD, 22));
        value.setForeground(NAVY);
        JLabel detail = new JLabel(detailText);
        detail.setFont(new Font("SansSerif", Font.PLAIN, 10));
        detail.setForeground(MUTED);
        card.add(label);
        card.add(Box.createVerticalStrut(10));
        card.add(value);
        card.add(Box.createVerticalStrut(5));
        card.add(detail);
        return card;
    }

    private void makeMenuItemsCardClickable(JPanel card) {
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Click to view menu item images");
        MouseAdapter openGallery = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (isAdmin()) {
                    showMenuItemGallery();
                }
            }
        };
        card.addMouseListener(openGallery);
        for (java.awt.Component component : card.getComponents()) {
            component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            component.addMouseListener(openGallery);
        }
    }

    /** Opens an admin-only image gallery from the dashboard's MENU ITEMS card. */
    private void showMenuItemGallery() {
        if (!isAdmin()) {
            return;
        }

        JPanel gallery = new JPanel(new GridLayout(0, 3, 12, 12));
        gallery.setBackground(BACKGROUND);
        gallery.setBorder(new EmptyBorder(12, 12, 12, 12));
        for (MenuItem item : menuItems) {
            JPanel itemCard = new JPanel(new BorderLayout(0, 7));
            itemCard.setBackground(Color.WHITE);
            itemCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    new EmptyBorder(9, 9, 9, 9)));

            JLabel image = new JLabel();
            image.setHorizontalAlignment(SwingConstants.CENTER);
            image.setPreferredSize(new Dimension(180, 118));
            image.setOpaque(true);
            image.setBackground(PALE_BLUE);
            if (new File(item.getImageFile()).isFile()) {
                image.setIcon(loadImage(item.getImageFile(), 180, 118));
            } else {
                image.setText("Image unavailable");
                image.setForeground(MUTED);
            }
            itemCard.add(image, BorderLayout.CENTER);

            JLabel details = new JLabel("<html><b>" + item.getName() + "</b><br>"
                    + item.getCategory() + " · " + CURRENCY.format(item.getPrice())
                    + "</html>");
            details.setFont(new Font("SansSerif", Font.PLAIN, 12));
            details.setForeground(NAVY);
            itemCard.add(details, BorderLayout.SOUTH);
            gallery.add(itemCard);
        }

        JScrollPane scroll = new JScrollPane(gallery);
        scroll.setPreferredSize(new Dimension(680, 460));
        scroll.getViewport().setBackground(BACKGROUND);
        JOptionPane.showMessageDialog(this, scroll, "Menu item images",
                JOptionPane.PLAIN_MESSAGE);
    }

    /** Displays up to the five most recent completed orders. */
    private JPanel createRecentOrdersPanel(String employeeFilter) {
        JPanel recentOrders = new JPanel(new BorderLayout(0, 12));
        recentOrders.setBackground(Color.WHITE);
        recentOrders.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(16, 16, 16, 16)));
        JLabel title = new JLabel(employeeFilter == null
                ? "Recent orders" : "Recent orders · " + employeeFilter);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(NAVY);
        recentOrders.add(title, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        List<String> activeEmployeeUsernames = accountManagementListener.getEmployeeUsernames();
        int matchingOrders = 0;
        for (Sale sale : completedOrders) {
            if (isDashboardSaleVisible(sale, employeeFilter, activeEmployeeUsernames)) {
                matchingOrders++;
            }
        }
        if (matchingOrders == 0) {
            JLabel empty = new JLabel(employeeFilter == null
                    ? "No completed orders yet. Your sales will appear here."
                    : "No completed orders for " + employeeFilter + " yet.");
            empty.setFont(new Font("SansSerif", Font.PLAIN, 13));
            empty.setForeground(MUTED);
            rows.add(empty);
        } else {
            int shown = 0;
            for (int i = completedOrders.size() - 1; i >= 0 && shown < 5; i--) {
                Sale sale = completedOrders.get(i);
                if (!isDashboardSaleVisible(sale, employeeFilter, activeEmployeeUsernames)) {
                    continue;
                }
                JPanel row = new JPanel(new BorderLayout(12, 0));
                row.setOpaque(false);
                row.setBorder(new EmptyBorder(10, 0, 10, 0));
                JLabel details = new JLabel("<html><b>" + sale.getOrderType()
                        + "</b><br><span style='color:#697A91'>"
                        + sale.getCompletedAtText() + " · " + sale.getEmployeeName()
                        + "</span></html>");
                details.setFont(new Font("SansSerif", Font.PLAIN, 12));
                JLabel total = new JLabel(CURRENCY.format(sale.getTotal()));
                total.setFont(new Font("SansSerif", Font.BOLD, 14));
                total.setForeground(NAVY);
                row.add(details, BorderLayout.CENTER);
                row.add(total, BorderLayout.EAST);
                rows.add(row);
                rows.add(new javax.swing.JSeparator());
                shown++;
            }
        }
        recentOrders.add(rows, BorderLayout.CENTER);
        return recentOrders;
    }

    private String getSelectedDashboardEmployee() {
        EmployeeFilterOption selected =
                (EmployeeFilterOption) dashboardEmployeeFilter.getSelectedItem();
        return selected == null ? null : selected.username;
    }

    private void refreshDashboardEmployeeFilter() {
        String previousSelection = getSelectedDashboardEmployee();
        ArrayList<EmployeeFilterOption> options = new ArrayList<EmployeeFilterOption>();
        options.add(new EmployeeFilterOption(null));
        for (String username : accountManagementListener.getEmployeeUsernames()) {
            if (!containsEmployeeOption(options, username)) {
                options.add(new EmployeeFilterOption(username));
            }
        }

        updatingDashboardEmployeeFilter = true;
        dashboardEmployeeFilter.removeAllItems();
        for (EmployeeFilterOption option : options) {
            dashboardEmployeeFilter.addItem(option);
        }
        EmployeeFilterOption selection = options.get(0);
        for (EmployeeFilterOption option : options) {
            if (previousSelection == null && option.username == null
                    || previousSelection != null && previousSelection.equals(option.username)) {
                selection = option;
                break;
            }
        }
        dashboardEmployeeFilter.setSelectedItem(selection);
        updatingDashboardEmployeeFilter = false;
    }

    private boolean containsEmployeeOption(
            List<EmployeeFilterOption> options, String username) {
        for (EmployeeFilterOption option : options) {
            if (username.equals(option.username)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDashboardSaleVisible(
            Sale sale, String employeeFilter, List<String> activeEmployeeUsernames) {
        if (employeeFilter != null) {
            return employeeFilter.equals(sale.getEmployeeName())
                    && activeEmployeeUsernames.contains(sale.getEmployeeName());
        }
        return activeEmployeeUsernames.contains(sale.getEmployeeName());
    }

    /**
     * Builds the header with the logo, store name,
     * and a button to open the order history.
     */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 4, 0, BLUE),
                new EmptyBorder(18, 19, 18, 19)));

        JLabel logo = new JLabel(loadImage("IMAGES/LOGO.jpg", 104, 78));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        logo.setForeground(Color.WHITE);
        logo.setOpaque(true);
        logo.setBackground(BLUE);
        logo.setPreferredSize(new Dimension(104, 78));

        JLabel title = new JLabel("JavaCup");
        title.setFont(new Font("SansSerif", Font.BOLD, 42));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("MILK TEA  ·  FRUIT TEA  ·  MADE FRESH");
        subtitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        subtitle.setForeground(new Color(245, 221, 192));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(title);
        titles.add(Box.createVerticalStrut(4));
        titles.add(subtitle);

        JPanel brand = new JPanel(new BorderLayout(13, 0));
        brand.setOpaque(false);
        brand.add(logo, BorderLayout.WEST);
        brand.add(titles, BorderLayout.CENTER);
        header.add(brand, BorderLayout.WEST);

        JLabel openStatus = new JLabel("●  READY TO SERVE");
        openStatus.setFont(new Font("SansSerif", Font.BOLD, 11));
        openStatus.setForeground(new Color(255, 235, 202));
        openStatus.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(openStatus, BorderLayout.CENTER);

        historyButton = new JButton();
        updateHistoryButton();
        historyButton.setPreferredSize(new Dimension(220, 60));
        historyButton.setMinimumSize(new Dimension(200, 56));
        historyButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        historyButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 190, 150), 1),
                new EmptyBorder(7, 13, 7, 13)));
        historyButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        historyButton.setToolTipText("View completed orders");
        historyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showOrderHistory();
            }
        });
        logoutButton = new JButton("LOG OUT");
        logoutButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        logoutButton.setPreferredSize(new Dimension(105, 60));
        logoutButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        styleButton(logoutButton, Color.WHITE, NAVY);
        logoutButton.addActionListener(event -> {
            int result = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to log out?",
                    "Log out", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                logoutAction.run();
            }
        });
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(historyButton);
        actions.add(logoutButton);
        header.add(actions, BorderLayout.EAST);
        historyButton.setVisible(isAdmin());
        return header;
    }

    /** Updates the order-history button with the completed order count. */
    private void updateHistoryButton() {
        historyButton.setText("<html><div style='text-align:center'>ORDER HISTORY"
                + "<br><span style='font-weight:normal'>"
                + completedOrders.size() + " completed orders</span></div></html>");
        styleButton(historyButton, new Color(252, 243, 230), NAVY);
    }

    /**
     * Builds the menu area with a title, short instructions,
     * and a scrollable grid of product cards.
     */
    private JPanel buildMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        JLabel menuTitle = new JLabel("Our menu");
        menuTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        menuTitle.setForeground(NAVY);
        controls.add(menuTitle);
        controls.add(Box.createVerticalStrut(4));
        JLabel description = new JLabel("Choose a drink or snack to start an order.");
        description.setFont(new Font("SansSerif", Font.PLAIN, 12));
        description.setForeground(MUTED);
        controls.add(description);
        controls.add(Box.createVerticalStrut(12));
        menuSearchField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        menuSearchField.setToolTipText("Search menu items by name, category, or item ID");
        menuSearchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        menuSearchField.setPreferredSize(new Dimension(260, 40));
        menuSearchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 10, 8, 10)));
        menuSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshMenu();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshMenu();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshMenu();
            }
        });
        controls.add(menuSearchField);
        panel.add(controls, BorderLayout.NORTH);

        menuGrid.setOpaque(false);
        menuGrid.setBorder(new EmptyBorder(2, 2, 10, 4));
        JScrollPane scroll = new JScrollPane(menuGrid);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BACKGROUND);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Adds a category filter button.
     * Clicking it selects the category and displays matching products.
     */
    private void addFilterButton(JPanel parent, final String category) {
        JButton button = createSidebarButton(category);
        styleSidebarButton(button, category.equals(selectedCategory));
        categoryButtons.add(button);
        button.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        button.setPreferredSize(new Dimension(175, 38));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                selectedCategory = category;
                for (JButton filter : categoryButtons) {
                    styleSidebarButton(filter, filter.getText().equals(selectedCategory));
                }
                showPage("pos");
                refreshMenu();
            }
        });
        parent.add(button);
        parent.add(Box.createVerticalStrut(5));
    }

    /**
     * Builds the cart panel with the order type, cart items,
     * total price, and checkout button.
     */
    private JPanel buildOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(17, 16, 16, 16)));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel title = new JLabel("Current order");
        title.setFont(new Font("SansSerif", Font.BOLD, 19));
        title.setForeground(NAVY);
        JButton clearButton = new JButton("Clear");
        clearButton.setBorderPainted(false);
        clearButton.setContentAreaFilled(false);
        clearButton.setForeground(MUTED);
        clearButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (!cartItems.isEmpty()) {
                    int answer = JOptionPane.showConfirmDialog(MilkTeaPOS.this,
                            "Remove all items from this order?", "Clear order",
                            JOptionPane.YES_NO_OPTION);
                    if (answer == JOptionPane.YES_OPTION) {
                        cartItems.clear();
                        refreshCart();
                    }
                }
            }
        });
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(clearButton, BorderLayout.EAST);
        heading.add(titleRow);
        heading.add(Box.createVerticalStrut(10));

        JLabel orderTypeLabel = new JLabel("ORDER TYPE");
        orderTypeLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        orderTypeLabel.setForeground(MUTED);
        heading.add(orderTypeLabel);
        heading.add(Box.createVerticalStrut(5));

        JPanel orderTypeButtons = new JPanel(new GridLayout(1, 2, 8, 0));
        orderTypeButtons.setOpaque(false);
        dineInButton = new JToggleButton("Dine in");
        takeOutButton = new JToggleButton("Take out");
        dineInButton.setUI(new BasicToggleButtonUI());
        takeOutButton.setUI(new BasicToggleButtonUI());
        ButtonGroup orderTypeGroup = new ButtonGroup();
        orderTypeGroup.add(dineInButton);
        orderTypeGroup.add(takeOutButton);
        takeOutButton.setSelected(true);
        dineInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                selectedOrderType = "Dine in";
                refreshOrderTypeButtons();
            }
        });
        takeOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                selectedOrderType = "Take out";
                refreshOrderTypeButtons();
            }
        });
        orderTypeButtons.add(dineInButton);
        orderTypeButtons.add(takeOutButton);
        heading.add(orderTypeButtons);
        refreshOrderTypeButtons();
        panel.add(heading, BorderLayout.NORTH);

        cartContents.setOpaque(false);
        cartContents.setLayout(new BoxLayout(cartContents, BoxLayout.Y_AXIS));
        JScrollPane cartScroll = new JScrollPane(cartContents);
        cartScroll.setBorder(BorderFactory.createEmptyBorder());
        cartScroll.getViewport().setBackground(Color.WHITE);
        panel.add(cartScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        JLabel note = new JLabel("Prices in Philippine pesos");
        note.setForeground(MUTED);
        note.setFont(new Font("SansSerif", Font.PLAIN, 11));
        bottom.add(note);
        bottom.add(Box.createVerticalStrut(8));

        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        JLabel totalCaption = new JLabel("TOTAL");
        totalCaption.setFont(new Font("SansSerif", Font.BOLD, 13));
        totalCaption.setForeground(MUTED);
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 25));
        totalLabel.setForeground(NAVY);
        totalRow.add(totalCaption, BorderLayout.WEST);
        totalRow.add(totalLabel, BorderLayout.EAST);
        bottom.add(totalRow);
        bottom.add(Box.createVerticalStrut(13));

        JButton checkout = new JButton("CHECK OUT");
        checkout.setFont(new Font("SansSerif", Font.BOLD, 14));
        checkout.setForeground(Color.WHITE);
        checkout.setBackground(BLUE);
        checkout.setFocusPainted(false);
        checkout.setBorderPainted(false);
        checkout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkout.setBorder(new EmptyBorder(13, 12, 13, 12));
        checkout.setAlignmentX(LEFT_ALIGNMENT);
        checkout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        checkout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                checkout();
            }
        });
        bottom.add(checkout);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    /** Updates the Dine in and Take out button styles based on the selection. */
    private void refreshOrderTypeButtons() {
        styleOrderTypeButton(dineInButton, "Dine in".equals(selectedOrderType));
        styleOrderTypeButton(takeOutButton, "Take out".equals(selectedOrderType));
    }

    /** Applies the selected or unselected style to an order-type button. */
    private void styleOrderTypeButton(JToggleButton button, boolean selected) {
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setRolloverEnabled(false);
        button.setBackground(selected ? NAVY : BLUE);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(57, 32, 17), selected ? 2 : 1),
                new EmptyBorder(10, 10, 10, 10)));
    }

    /**
     * Clears and redraws product cards for the selected category.
     * Add-ons are excluded because they are selected when customizing a drink.
     */
    private void refreshMenu() {
        menuGrid.removeAll();
        String query = menuSearchField.getText().trim().toLowerCase(Locale.ROOT);
        int visibleItems = 0;
        for (final MenuItem item : menuItems) {
            if (item instanceof AddOn) {
                continue;
            }
            if (!selectedCategory.equals("All") && !selectedCategory.equals(item.getCategory())) {
                continue;
            }
            if (!query.isEmpty()
                    && !item.getName().toLowerCase(Locale.ROOT).contains(query)
                    && !item.getCategory().toLowerCase(Locale.ROOT).contains(query)
                    && !item.getId().toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            menuGrid.add(createProductCard(item));
            visibleItems++;
        }
        if (visibleItems == 0) {
            JLabel noResults = new JLabel("No menu items found.", SwingConstants.CENTER);
            noResults.setFont(new Font("SansSerif", Font.PLAIN, 14));
            noResults.setForeground(MUTED);
            menuGrid.add(noResults);
        }
        menuGrid.revalidate();
        menuGrid.repaint();
    }

    /**
     * Creates a product card with an image, category, name, price,
     * and Add button.
     */
    private JPanel createProductCard(final MenuItem item) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(10, 10, 11, 10)));

        JLabel image = new JLabel(loadImage(item.getImageFile(), 180, 118));
        image.setHorizontalAlignment(SwingConstants.CENTER);
        image.setPreferredSize(new Dimension(180, 118));
        image.setOpaque(true);
        image.setBackground(PALE_BLUE);
        card.add(image, BorderLayout.NORTH);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel category = new JLabel(item.getCategory().toUpperCase());
        category.setFont(new Font("SansSerif", Font.BOLD, 10));
        category.setForeground(BLUE);
        JLabel name = new JLabel(item.getName());
        name.setFont(new Font("SansSerif", Font.BOLD, 18));
        name.setForeground(NAVY);
        JLabel price = new JLabel(CURRENCY.format(item.getPrice()));
        price.setFont(new Font("SansSerif", Font.BOLD, 16));
        price.setForeground(BLUE);
        details.add(category);
        details.add(Box.createVerticalStrut(5));
        details.add(name);
        details.add(Box.createVerticalStrut(5));
        details.add(price);
        card.add(details, BorderLayout.CENTER);

        JButton add = new JButton("+ Add");
        add.setFont(new Font("SansSerif", Font.BOLD, 12));
        add.setForeground(Color.WHITE);
        add.setBackground(BLUE);
        if (getStock(item) == 0) {
            add.setText("OUT OF STOCK");
            add.setBackground(MUTED);
            add.setEnabled(false);
        }
        add.setFocusPainted(false);
        add.setBorderPainted(false);
        add.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add.setBorder(new EmptyBorder(9, 10, 9, 10));
        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                addToOrder(item);
            }
        });
        card.add(add, BorderLayout.SOUTH);
        return card;
    }

    /**
     * Loads an image from the project folder and scales it
     * to fit the requested dimensions.
     */
    private ImageIcon loadImage(String fileName, int width, int height) {
        File file = new File(fileName);
        if (!file.isFile()) {
            return new ImageIcon();
        }
        ImageIcon source = new ImageIcon(file.getAbsolutePath());
        double scale = Math.min((double) width / source.getIconWidth(),
                (double) height / source.getIconHeight());
        int scaledWidth = Math.max(1, (int) Math.round(source.getIconWidth() * scale));
        int scaledHeight = Math.max(1, (int) Math.round(source.getIconHeight() * scale));
        Image scaled = source.getImage().getScaledInstance(
                scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    /**
     * Adds a product to the order.
     * For drinks, first collects the sugar level, cup size, add-ons, and quantity.
     * For snacks, checks stock and adds the item directly to the cart.
     */
    private void addToOrder(MenuItem item) {
        ArrayList<AddOn> chosenAddOns = new ArrayList<AddOn>();
        String sugarLevel = "N/A";
        String cupSize = "N/A";
        if (item instanceof MilkTea || item instanceof FruitTea) {
            JPanel choices = new JPanel();
            choices.setLayout(new BoxLayout(choices, BoxLayout.Y_AXIS));
            choices.add(new JLabel("Customize " + item.getName()));
            choices.add(Box.createVerticalStrut(8));

            JComboBox<String> sugarSelector = new JComboBox<String>(
                    new String[] {"0%", "25%", "50%", "75%", "100%"});
            sugarSelector.setSelectedItem("100%");
            JComboBox<String> sizeSelector = new JComboBox<String>(
                    new String[] {"12 oz", "16 oz"});
            sizeSelector.setSelectedItem("16 oz");
            JPanel sugarRow = new JPanel(new BorderLayout(12, 0));
            sugarRow.add(new JLabel("Sugar level"), BorderLayout.WEST);
            sugarRow.add(sugarSelector, BorderLayout.EAST);
            JPanel sizeRow = new JPanel(new BorderLayout(12, 0));
            sizeRow.add(new JLabel("Cup size"), BorderLayout.WEST);
            sizeRow.add(sizeSelector, BorderLayout.EAST);
            choices.add(sugarRow);
            choices.add(Box.createVerticalStrut(6));
            choices.add(sizeRow);
            choices.add(Box.createVerticalStrut(9));
            JLabel addOnLabel = new JLabel("ADD-ONS");
            addOnLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
            addOnLabel.setForeground(NAVY);
            choices.add(addOnLabel);
            ArrayList<JCheckBox> checkBoxes = new ArrayList<JCheckBox>();
            for (AddOn addOn : availableAddOns) {
                JCheckBox checkBox = new JCheckBox(
                        addOn.getName() + "  (+" + CURRENCY.format(addOn.getPrice()) + ")");
                checkBoxes.add(checkBox);
                choices.add(checkBox);
            }
            JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
            JPanel quantityRow = new JPanel(new BorderLayout(8, 0));
            quantityRow.add(new JLabel("Quantity"), BorderLayout.WEST);
            quantityRow.add(quantity, BorderLayout.EAST);
            choices.add(Box.createVerticalStrut(8));
            choices.add(quantityRow);

            int answer = JOptionPane.showConfirmDialog(this, choices, "Customize drink",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }
            sugarLevel = (String) sugarSelector.getSelectedItem();
            cupSize = (String) sizeSelector.getSelectedItem();
            for (int i = 0; i < checkBoxes.size(); i++) {
                if (checkBoxes.get(i).isSelected()) {
                    chosenAddOns.add(availableAddOns.get(i));
                }
            }
            int count = ((Integer) quantity.getValue()).intValue();
            if (!canReserveStock(item, chosenAddOns, count)) {
                showInsufficientStock(item, chosenAddOns, count);
                return;
            }
            for (int i = 0; i < count; i++) {
                addCartItem(item, chosenAddOns, sugarLevel, cupSize);
            }
        } else {
            if (!canReserveStock(item, chosenAddOns, 1)) {
                showInsufficientStock(item, chosenAddOns, 1);
                return;
            }
            addCartItem(item, chosenAddOns, sugarLevel, cupSize);
        }
        refreshCart();
    }

    /**
     * Combines an item with an existing cart row when its product
     * and customizations match; otherwise, creates a new row.
     */
    private void addCartItem(MenuItem item, List<AddOn> addOns,
            String sugarLevel, String cupSize) {
        for (CartItem cartItem : cartItems) {
            if (cartItem.matches(item, addOns, sugarLevel, cupSize)) {
                cartItem.increaseQuantity();
                return;
            }
        }
        cartItems.add(new CartItem(item, addOns, sugarLevel, cupSize));
    }

    /**
     * Checks that enough unreserved stock is available for the product
     * and all selected add-ons.
     */
    private boolean canReserveStock(MenuItem item, List<AddOn> addOns, int quantity) {
        if (getStock(item) - getReservedStock(item.getId()) < quantity) {
            return false;
        }
        for (AddOn addOn : addOns) {
            if (getStock(addOn) - getReservedStock(addOn.getId()) < quantity) {
                return false;
            }
        }
        return true;
    }

    /** Counts how many units of an item are already reserved in the cart. */
    private int getReservedStock(String itemId) {
        int reserved = 0;
        for (CartItem cartItem : cartItems) {
            if (itemId.equals(cartItem.getProduct().getId())) {
                reserved += cartItem.getQuantity();
            }
            for (AddOn addOn : cartItem.getAddOns()) {
                if (itemId.equals(addOn.getId())) {
                    reserved += cartItem.getQuantity();
                }
            }
        }
        return reserved;
    }

    /** Shows which product or add-on does not have enough stock. */
    private void showInsufficientStock(MenuItem item, List<AddOn> addOns, int quantity) {
        StringBuilder message = new StringBuilder("Not enough stock for ")
                .append(item.getName()).append(" (").append(quantity).append(" requested).");
        for (AddOn addOn : addOns) {
            if (getStock(addOn) - getReservedStock(addOn.getId()) < quantity) {
                message.append("\n").append(addOn.getName()).append(": ")
                        .append(Math.max(0, getStock(addOn) - getReservedStock(addOn.getId())))
                        .append(" available.");
            }
        }
        if (getStock(item) - getReservedStock(item.getId()) < quantity) {
            message.append("\n").append(item.getName()).append(": ")
                    .append(Math.max(0, getStock(item) - getReservedStock(item.getId())))
                    .append(" available.");
        }
        JOptionPane.showMessageDialog(this, message.toString(),
                "Insufficient stock", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Redraws the cart, shows an empty-cart message when needed,
     * and updates the total price.
     */
    private void refreshCart() {
        cartContents.removeAll();
        if (cartItems.isEmpty()) {
            JLabel empty = new JLabel("<html><div style='text-align:center'>Your order is empty.<br>"
                    + "Add a drink to get started!</div></html>");
            empty.setForeground(MUTED);
            empty.setAlignmentX(CENTER_ALIGNMENT);
            empty.setBorder(new EmptyBorder(28, 4, 20, 4));
            cartContents.add(empty);
        } else {
            for (int i = 0; i < cartItems.size(); i++) {
                cartContents.add(createCartRow(cartItems.get(i)));
                cartContents.add(Box.createVerticalStrut(9));
            }
        }
        totalLabel.setText(CURRENCY.format(getOrderTotal()));
        cartContents.revalidate();
        cartContents.repaint();
    }

    /**
     * Creates a cart row with the item name, customizations, subtotal,
     * quantity controls, and a remove button.
     */
    private JPanel createCartRow(final CartItem cartItem) {
        JPanel row = new JPanel(new BorderLayout(8, 4));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

        JPanel description = new JPanel();
        description.setOpaque(false);
        description.setLayout(new BoxLayout(description, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(cartItem.getProduct().getName());
        name.setFont(new Font("SansSerif", Font.BOLD, 13));
        name.setForeground(NAVY);
        JLabel options = new JLabel(cartItem.getCustomizationText());
        options.setFont(new Font("SansSerif", Font.PLAIN, 10));
        options.setForeground(MUTED);
        JLabel price = new JLabel(CURRENCY.format(cartItem.getSubtotal()));
        price.setFont(new Font("SansSerif", Font.BOLD, 12));
        price.setForeground(NAVY);
        description.add(name);
        description.add(Box.createVerticalStrut(3));
        description.add(options);
        description.add(Box.createVerticalStrut(4));
        description.add(price);
        row.add(description, BorderLayout.CENTER);

        JPanel controls = new JPanel();
        controls.setOpaque(false);
        JButton minus = smallQuantityButton("−");
        JButton plus = smallQuantityButton("+");
        JLabel quantity = new JLabel(String.valueOf(cartItem.getQuantity()));
        quantity.setFont(new Font("SansSerif", Font.BOLD, 12));
        minus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (cartItem.getQuantity() == 1) {
                    cartItems.remove(cartItem);
                } else {
                    cartItem.decreaseQuantity();
                }
                refreshCart();
            }
        });
        plus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (canReserveStock(cartItem.getProduct(), cartItem.getAddOns(), 1)) {
                    cartItem.increaseQuantity();
                    refreshCart();
                } else {
                    showInsufficientStock(cartItem.getProduct(), cartItem.getAddOns(), 1);
                }
            }
        });
        controls.add(minus);
        controls.add(quantity);
        controls.add(plus);
        JButton remove = new JButton("×");
        remove.setToolTipText("Remove this item from the order");
        remove.getAccessibleContext().setAccessibleName("Remove " + cartItem.getProduct().getName());
        remove.setFont(new Font("SansSerif", Font.BOLD, 16));
        remove.setMargin(new Insets(2, 7, 2, 7));
        remove.setFocusPainted(false);
        remove.setBorderPainted(false);
        remove.setBackground(new Color(252, 243, 230));
        remove.setForeground(NAVY);
        remove.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                cartItems.remove(cartItem);
                refreshCart();
            }
        });
        controls.add(remove);
        row.add(controls, BorderLayout.EAST);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(7, 0, 9, 0)));
        return row;
    }

    /** Creates a small button for increasing or decreasing quantity. */
    private JButton smallQuantityButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setMargin(new Insets(2, 7, 2, 7));
        button.setFocusPainted(false);
        button.setForeground(NAVY);
        return button;
    }

    /** Adds together the subtotals of all cart items. */
    private double getOrderTotal() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getSubtotal();
        }
        return total;
    }

    /**
     * Checks the cart and stock, accepts payment,
     * creates a receipt, and completes the order.
     */
    private void checkout() {
        if (isAdmin()) {
            JOptionPane.showMessageDialog(this,
                    "Only employee accounts can create and complete orders.",
                    "Access denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one item before checking out.",
                    "Empty order", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!orderFitsCurrentStock()) {
            JOptionPane.showMessageDialog(this,
                    "The order exceeds current stock. Update the order or inventory before checkout.",
                    "Insufficient stock", JOptionPane.ERROR_MESSAGE);
            return;
        }
        double total = getOrderTotal();
        while (true) {
            String input = JOptionPane.showInputDialog(this,
                    "Amount due: " + CURRENCY.format(total) + "\nEnter cash received:",
                    "Cash payment", JOptionPane.PLAIN_MESSAGE);
            if (input == null) {
                return;
            }
            double cash;
            try {
                cash = Double.parseDouble(input.trim().replace(",", ""));
            } catch (NumberFormatException exception) {
                JOptionPane.showMessageDialog(this, "Enter a valid cash amount.",
                        "Invalid amount", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            if (cash < total) {
                JOptionPane.showMessageDialog(this, "Cash received is less than the total.",
                        "Insufficient payment", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            String receipt = createReceipt(total, cash);
            deductOrderStock();
            completedOrders.add(new Sale(receipt, total, selectedOrderType, currentUsername));
            updateHistoryButton();
            JOptionPane.showMessageDialog(this, receipt, "Payment complete",
                    JOptionPane.INFORMATION_MESSAGE);
            cartItems.clear();
            nextReceiptNumber++;
            refreshCart();
            refreshDashboard();
            refreshInventory();
            refreshMenu();
            return;
        }
    }

    /** Confirms that the reserved order does not exceed current stock. */
    private boolean orderFitsCurrentStock() {
        for (MenuItem item : menuItems) {
            if (getReservedStock(item.getId()) > getStock(item)) {
                return false;
            }
        }
        return true;
    }

    /** Deducts the ordered products and selected add-ons from inventory. */
    private void deductOrderStock() {
        for (CartItem cartItem : cartItems) {
            String productId = cartItem.getProduct().getId();
            stockByItemId.put(productId, Integer.valueOf(
                    getStock(cartItem.getProduct()) - cartItem.getQuantity()));
            for (AddOn addOn : cartItem.getAddOns()) {
                stockByItemId.put(addOn.getId(), Integer.valueOf(
                        getStock(addOn) - cartItem.getQuantity()));
            }
        }
    }

    /**
     * Creates a text receipt listing the items, total,
     * cash received, and change.
     */
    private String createReceipt(double total, double cash) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("          JAVACUP\n");
        receipt.append("     MILK TEA & FRUIT TEA\n");
        receipt.append("--------------------------------\n");
        receipt.append(String.format(Locale.US, "Receipt #%04d%n", nextReceiptNumber));
        receipt.append("Employee: ").append(currentUsername).append('\n');
        receipt.append("Order type: ").append(selectedOrderType).append('\n');
        receipt.append("--------------------------------\n");
        for (CartItem item : cartItems) {
            receipt.append(item.getProduct().getName())
                    .append(" x").append(item.getQuantity()).append('\n');
            if (!"N/A".equals(item.getCupSize())) {
                receipt.append("  ").append(item.getCupSize())
                        .append(" · Sugar ").append(item.getSugarLevel()).append('\n');
            }
            if (!item.getOptionsText().isEmpty()) {
                receipt.append("  ").append(item.getOptionsText()).append('\n');
            }
            receipt.append("  ").append(CURRENCY.format(item.getSubtotal())).append('\n');
        }
        receipt.append("--------------------------------\n");
        receipt.append("TOTAL:    ").append(CURRENCY.format(total)).append('\n');
        receipt.append("CASH:     ").append(CURRENCY.format(cash)).append('\n');
        receipt.append("CHANGE:   ").append(CURRENCY.format(cash - total)).append('\n');
        receipt.append("--------------------------------\n");
        receipt.append("       Salamat po!");
        return receipt.toString();
    }

    /** Displays completed orders and the full receipt selected by the admin. */
    private void showOrderHistory() {
        if (!isAdmin()) {
            return;
        }
        if (completedOrders.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No completed orders yet.",
                    "Order history", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        final ArrayList<Sale> recentSales = new ArrayList<Sale>();
        DefaultTableModel tableModel = new DefaultTableModel(
                new Object[] {"Receipt", "Completed", "Employee", "Order type", "Total"}, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        double salesTotal = 0;
        for (int i = completedOrders.size() - 1; i >= 0; i--) {
            Sale sale = completedOrders.get(i);
            recentSales.add(sale);
            salesTotal += sale.getTotal();
            tableModel.addRow(new Object[] {
                String.format(Locale.US, "#%04d", i + 1),
                sale.getCompletedAtText(),
                sale.getEmployeeName(),
                sale.getOrderType(),
                CURRENCY.format(sale.getTotal())
            });
        }

        JTable ordersTable = new JTable(tableModel);
        ordersTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        ordersTable.setAutoCreateRowSorter(true);
        ordersTable.setFillsViewportHeight(true);
        ordersTable.setRowHeight(25);
        ordersTable.setToolTipText("Select an order to view its full receipt");

        JTextArea receiptDetails = new JTextArea("Select an order to view its details.");
        receiptDetails.setEditable(false);
        receiptDetails.setFont(new Font("Monospaced", Font.PLAIN, 12));
        receiptDetails.setBorder(new EmptyBorder(10, 10, 10, 10));
        ordersTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && ordersTable.getSelectedRow() >= 0) {
                int modelRow = ordersTable.convertRowIndexToModel(ordersTable.getSelectedRow());
                Sale sale = recentSales.get(modelRow);
                StringBuilder details = new StringBuilder();
                details.append("Completed: ").append(sale.getCompletedAtText()).append('\n');
                details.append("Employee: ").append(sale.getEmployeeName()).append('\n');
                details.append("Order type: ").append(sale.getOrderType()).append("\n\n");
                details.append(sale.getReceipt());
                receiptDetails.setText(details.toString());
                receiptDetails.setCaretPosition(0);
            }
        });
        ordersTable.setRowSelectionInterval(0, 0);

        JScrollPane ordersScroll = new JScrollPane(ordersTable);
        JScrollPane receiptScroll = new JScrollPane(receiptDetails);
        JPanel orderHistory = new JPanel(new BorderLayout(0, 10));
        JLabel summary = new JLabel("Completed orders: " + completedOrders.size()
                + "   |   Total sales: " + CURRENCY.format(salesTotal)
                + "   |   Select an order to view its receipt");
        summary.setFont(new Font("SansSerif", Font.BOLD, 13));
        summary.setForeground(NAVY);
        orderHistory.add(summary, BorderLayout.NORTH);

        JSplitPane detailsSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, ordersScroll, receiptScroll);
        detailsSplit.setResizeWeight(0.58);
        orderHistory.add(detailsSplit, BorderLayout.CENTER);
        orderHistory.setPreferredSize(new Dimension(820, 540));
        JOptionPane.showMessageDialog(this, orderHistory,
                "Order history", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Applies consistent background and foreground colors to a button. */
    private void styleButton(JButton button, Color background, Color foreground) {
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
    }
}
