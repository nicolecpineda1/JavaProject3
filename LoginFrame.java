import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public final class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    // Colors and input fields used by the login screen.
    private static final Color BACKGROUND = new Color(250, 247, 241);
    private static final Color NAVY = new Color(78, 45, 25);
    private static final Color BLUE = new Color(189, 111, 43);
    private static final Color BORDER = new Color(232, 215, 192);
    private static final Color MUTED = new Color(128, 103, 78);
    private static final Color PALE_ACCENT = new Color(252, 243, 230);
    public static final int MAX_EMPLOYEE_ACCOUNTS = 3;
    private final EnumMap<UserRole, Account> accounts = new EnumMap<UserRole, Account>(UserRole.class);
    private final ArrayList<Account> employeeAccounts = new ArrayList<Account>();
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JLabel lockoutStatusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final Timer employeeLockoutTimer =
            new Timer(1000, event -> refreshEmployeeLockout());
    private final LoginListener loginListener;
    private ButtonGroup roleGroup;
    private JRadioButton adminChoice;
    private JRadioButton employeeChoice;
    private UserRole selectedRole;
    private JButton loginButton;
    private int failedEmployeeAttempts;
    private long employeeLockoutUntil;

    public enum UserRole {
        ADMIN,
        EMPLOYEE
    }

    public interface LoginListener {
        void onLogin(UserRole role, String username);
    }

    public interface AccountManagementListener {
        String getUsername(UserRole role);

        void updateCredentials(UserRole role, String username, String password);

        List<String> getEmployeeUsernames();

        void updateEmployeeCredentials(int accountIndex, String username, String password);

        void addEmployeeAccount(String username, String password);

        void deleteEmployeeAccount(int accountIndex);
    }

    private static final class Account {
        private String username;
        private String password;

        private Account(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    /** Sets up the login window and makes Enter submit the login form. */
    public LoginFrame(LoginListener loginListener) {
        if (loginListener == null) {
            throw new IllegalArgumentException("A login listener is required.");
        }
        this.loginListener = loginListener;
        accounts.put(UserRole.ADMIN, new Account("BINOS", "BINOS"));
        employeeAccounts.add(new Account("NIKKI", "NIKKI"));
        setTitle("JavaCup | Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(580, 620));
        setSize(680, 700);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        getRootPane().setDefaultButton(loginButton);
    }

    /** Centers the credential card in the login window. */
    private JPanel buildContent() {
        JPanel root = new JPanel(new GridBagLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D decoration = (Graphics2D) graphics.create();
                decoration.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                decoration.setColor(new Color(244, 231, 211));
                decoration.fillOval(getWidth() - 190, -105, 270, 270);
                decoration.setColor(new Color(246, 235, 220));
                decoration.fillOval(-125, getHeight() - 175, 260, 260);
                decoration.setColor(new Color(238, 218, 191));
                decoration.fillOval(74, 86, 10, 10);
                decoration.fillOval(getWidth() - 94, getHeight() - 112, 8, 8);
                decoration.dispose();
            }
        };
        root.setBackground(BACKGROUND);
        root.add(buildLoginPanel());
        return root;
    }

    /** Builds the centered credential card using the POS caramel-and-cream palette. */
    private JPanel buildLoginPanel() {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(
                                0, 0, 5, 5, new Color(232, 215, 192)),
                        BorderFactory.createLineBorder(BORDER)),
                new EmptyBorder(27, 38, 25, 38)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JPanel brandMark = createBrandMark();
        brandMark.setAlignmentX(CENTER_ALIGNMENT);
        card.add(brandMark);
        card.add(Box.createVerticalStrut(12));

        JLabel badge = new JLabel("  JAVACUP  ·  POINT OF SALE  ");
        badge.setAlignmentX(CENTER_ALIGNMENT);
        badge.setFont(new Font("SansSerif", Font.BOLD, 10));
        badge.setForeground(BLUE);
        badge.setOpaque(true);
        badge.setBackground(PALE_ACCENT);
        badge.setBorder(new EmptyBorder(7, 8, 7, 8));
        card.add(badge);
        card.add(Box.createVerticalStrut(16));

        JLabel title = new JLabel("Welcome back");
        title.setAlignmentX(CENTER_ALIGNMENT);
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        title.setForeground(NAVY);
        card.add(title);

        JLabel subtitle = new JLabel("Sign in to continue to your point of sale.");
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        card.add(Box.createVerticalStrut(7));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(20));

        JLabel roleLabel = new JLabel("Choose account type");
        roleLabel.setAlignmentX(CENTER_ALIGNMENT);
        roleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        roleLabel.setForeground(NAVY);
        card.add(roleLabel);
        card.add(Box.createVerticalStrut(8));

        JPanel roleChoices = new JPanel(new GridLayout(1, 2, 10, 0));
        roleChoices.setOpaque(false);
        roleGroup = new ButtonGroup();
        adminChoice = createRoleChoice("Admin", UserRole.ADMIN);
        employeeChoice = createRoleChoice("Employee", UserRole.EMPLOYEE);
        roleGroup.add(adminChoice);
        roleGroup.add(employeeChoice);
        roleChoices.add(adminChoice);
        roleChoices.add(employeeChoice);
        card.add(roleChoices);
        card.add(Box.createVerticalStrut(18));

        JPanel accentRule = new JPanel();
        accentRule.setBackground(BLUE);
        accentRule.setMaximumSize(new Dimension(90, 3));
        accentRule.setPreferredSize(new Dimension(90, 3));
        accentRule.setAlignmentX(CENTER_ALIGNMENT);
        card.add(accentRule);
        card.add(Box.createVerticalStrut(16));

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 7, 0);
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        usernameLabel.setForeground(NAVY);
        fields.add(usernameLabel, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 16, 0);
        styleField(usernameField);
        fields.add(usernameField, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 7, 0);
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        passwordLabel.setForeground(NAVY);
        fields.add(passwordLabel, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 22, 0);
        styleField(passwordField);
        fields.add(passwordField, constraints);
        card.add(fields);

        loginButton = createLoginButton();
        loginButton.setAlignmentX(CENTER_ALIGNMENT);
        loginButton.addActionListener(event -> login());
        loginButton.setPreferredSize(new Dimension(320, 48));
        loginButton.setMaximumSize(new Dimension(320, 48));
        card.add(loginButton);

        lockoutStatusLabel.setAlignmentX(CENTER_ALIGNMENT);
        lockoutStatusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lockoutStatusLabel.setForeground(new Color(190, 40, 50));
        card.add(Box.createVerticalStrut(8));
        card.add(lockoutStatusLabel);

        JLabel hint = new JLabel("Sign in with your admin or employee account.");
        hint.setAlignmentX(CENTER_ALIGNMENT);
        hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hint.setForeground(MUTED);
        card.add(Box.createVerticalStrut(18));
        card.add(hint);

        return card;
    }

    private JPanel createBrandMark() {
        JPanel mark = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D drawing = (Graphics2D) graphics.create();
                drawing.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawing.setColor(BLUE);
                drawing.fillOval(1, 1, getWidth() - 3, getHeight() - 3);
                drawing.setColor(Color.WHITE);
                drawing.setFont(new Font("SansSerif", Font.BOLD, 16));
                String initials = "JC";
                int x = (getWidth() - drawing.getFontMetrics().stringWidth(initials)) / 2;
                int y = (getHeight() - drawing.getFontMetrics().getHeight()) / 2
                        + drawing.getFontMetrics().getAscent();
                drawing.drawString(initials, x, y);
                drawing.dispose();
            }
        };
        mark.setOpaque(false);
        mark.setPreferredSize(new Dimension(48, 48));
        mark.setMaximumSize(new Dimension(48, 48));
        return mark;
    }

    private JRadioButton createRoleChoice(String label, final UserRole role) {
        JRadioButton choice = new JRadioButton(label);
        choice.setFont(new Font("SansSerif", Font.BOLD, 13));
        choice.setForeground(NAVY);
        choice.setBackground(Color.WHITE);
        choice.setOpaque(true);
        choice.setFocusPainted(false);
        choice.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        choice.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(10, 12, 10, 8)));
        choice.addActionListener(event -> selectRole(role));
        return choice;
    }

    private void selectRole(UserRole role) {
        selectedRole = role;
        adminChoice.setSelected(role == UserRole.ADMIN);
        employeeChoice.setSelected(role == UserRole.EMPLOYEE);
        styleRoleChoice(adminChoice, role == UserRole.ADMIN);
        styleRoleChoice(employeeChoice, role == UserRole.EMPLOYEE);
        refreshEmployeeLockout();
        passwordField.setText("");
        usernameField.requestFocusInWindow();
    }

    private void styleRoleChoice(JRadioButton choice, boolean selected) {
        choice.setForeground(selected ? Color.WHITE : NAVY);
        choice.setBackground(selected ? BLUE : Color.WHITE);
        choice.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? NAVY : BORDER, selected ? 2 : 1),
                new EmptyBorder(9, 11, 9, 7)));
    }

    /** Applies a consistent font, size, background, and border to an input field. */
    private void styleField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 15));
        field.setPreferredSize(new Dimension(320, 46));
        field.setBackground(new Color(255, 252, 247));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 205, 178)),
                new EmptyBorder(9, 12, 9, 12)));
    }

    /** Creates the button used to submit the login credentials. */
    private JButton createLoginButton() {
        JButton button = new JButton("LOG IN");
        button.setFont(new Font("SansSerif", Font.BOLD, 15));
        button.setForeground(Color.WHITE);
        button.setBackground(NAVY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(15, 18, 15, 18));
        return button;
    }

    /**
     * Checks the entered credentials.
     * Opens the workspace for the selected role or reports invalid credentials.
     */
    private void login() {
        if (selectedRole == null) {
            JOptionPane.showMessageDialog(this, "Choose Admin or Employee before logging in.",
                    "Choose account type", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (selectedRole == UserRole.EMPLOYEE && employeeLockoutUntil > System.currentTimeMillis()) {
            refreshEmployeeLockout();
            return;
        }
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        Account account = null;
        if (selectedRole == UserRole.ADMIN) {
            account = accounts.get(UserRole.ADMIN);
            if (!account.username.equals(username) || !account.password.equals(password)) {
                account = null;
            }
        } else {
            for (Account employeeAccount : employeeAccounts) {
                if (employeeAccount.username.equals(username)
                        && employeeAccount.password.equals(password)) {
                    account = employeeAccount;
                    break;
                }
            }
        }
        if (account != null) {
            if (selectedRole == UserRole.EMPLOYEE) {
                failedEmployeeAttempts = 0;
            }
            passwordField.setText("");
            setVisible(false);
            loginListener.onLogin(selectedRole, account.username);
            return;
        }
        if (selectedRole == UserRole.EMPLOYEE) {
            failedEmployeeAttempts++;
            if (failedEmployeeAttempts >= 3) {
                employeeLockoutUntil = System.currentTimeMillis() + 30_000L;
                refreshEmployeeLockout();
                employeeLockoutTimer.start();
                passwordField.setText("");
                JOptionPane.showMessageDialog(this,
                        "Too many incorrect employee login attempts. Please wait 30 seconds.",
                        "Employee login locked", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        JOptionPane.showMessageDialog(this, "Incorrect username or password.",
                "Login failed", JOptionPane.ERROR_MESSAGE);
        passwordField.setText("");
        passwordField.requestFocusInWindow();
    }

    private void refreshEmployeeLockout() {
        long remainingMillis = employeeLockoutUntil - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            employeeLockoutUntil = 0;
            failedEmployeeAttempts = 0;
            employeeLockoutTimer.stop();
            lockoutStatusLabel.setText(" ");
        } else {
            long remainingSeconds = (remainingMillis + 999L) / 1000L;
            lockoutStatusLabel.setText(
                    "Employee login locked. Try again in " + remainingSeconds + " seconds.");
        }
        boolean employeeLocked = selectedRole == UserRole.EMPLOYEE
                && employeeLockoutUntil > System.currentTimeMillis();
        loginButton.setEnabled(!employeeLocked);
        loginButton.setText(employeeLocked ? "LOCKED" : "LOG IN");
    }

    public String getAccountUsername(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("An account role is required.");
        }
        return role == UserRole.ADMIN
                ? accounts.get(UserRole.ADMIN).username : employeeAccounts.get(0).username;
    }

    public void updateAccountFromAdmin(UserRole role, String username, String password) {
        if (role == null || username == null || username.trim().isEmpty()
                || password == null) {
            throw new IllegalArgumentException("A username is required.");
        }
        if (role == UserRole.EMPLOYEE) {
            updateEmployeeCredentials(0, username, password);
            return;
        }
        String updatedUsername = username.trim();
        if (isUsernameInUse(updatedUsername, -1)) {
            throw new IllegalArgumentException("That username is already in use.");
        }

        Account account = accounts.get(UserRole.ADMIN);
        account.username = updatedUsername;
        if (!password.isEmpty()) {
            account.password = password;
        }
        if (selectedRole == UserRole.ADMIN) {
            usernameField.setText(updatedUsername);
        }
    }

    public List<String> getEmployeeUsernames() {
        ArrayList<String> usernames = new ArrayList<String>();
        for (Account account : employeeAccounts) {
            usernames.add(account.username);
        }
        return Collections.unmodifiableList(usernames);
    }

    public void updateEmployeeCredentials(int accountIndex, String username, String password) {
        if (accountIndex < 0 || accountIndex >= employeeAccounts.size()
                || username == null || username.trim().isEmpty() || password == null) {
            throw new IllegalArgumentException("A valid employee account and username are required.");
        }
        String updatedUsername = username.trim();
        if (isUsernameInUse(updatedUsername, accountIndex)) {
            throw new IllegalArgumentException("That username is already in use.");
        }

        Account account = employeeAccounts.get(accountIndex);
        account.username = updatedUsername;
        if (!password.isEmpty()) {
            account.password = password;
        }
        resetEmployeeLockout();
        if (selectedRole == UserRole.EMPLOYEE && accountIndex == 0) {
            usernameField.setText(updatedUsername);
        }
    }

    public void addEmployeeAccount(String username, String password) {
        if (employeeAccounts.size() >= MAX_EMPLOYEE_ACCOUNTS) {
            throw new IllegalArgumentException("A maximum of "
                    + MAX_EMPLOYEE_ACCOUNTS + " employee accounts is allowed.");
        }
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("A username and password are required.");
        }
        String newUsername = username.trim();
        if (isUsernameInUse(newUsername, -1)) {
            throw new IllegalArgumentException("That username is already in use.");
        }
        employeeAccounts.add(new Account(newUsername, password));
        resetEmployeeLockout();
    }

    public void deleteEmployeeAccount(int accountIndex) {
        if (accountIndex < 0 || accountIndex >= employeeAccounts.size()) {
            throw new IllegalArgumentException("A valid employee account is required.");
        }
        employeeAccounts.remove(accountIndex);
        resetEmployeeLockout();
    }

    private boolean isUsernameInUse(String username, int employeeIndexToIgnore) {
        if (accounts.get(UserRole.ADMIN).username.equals(username)) {
            return true;
        }
        for (int index = 0; index < employeeAccounts.size(); index++) {
            if (index != employeeIndexToIgnore
                    && employeeAccounts.get(index).username.equals(username)) {
                return true;
            }
        }
        return false;
    }

    private void resetEmployeeLockout() {
        failedEmployeeAttempts = 0;
        employeeLockoutUntil = 0;
        employeeLockoutTimer.stop();
        refreshEmployeeLockout();
    }

    /** Displays the login screen again after a user signs out. */
    public void showLogin() {
        selectedRole = null;
        roleGroup.clearSelection();
        styleRoleChoice(adminChoice, false);
        styleRoleChoice(employeeChoice, false);
        usernameField.setText("");
        passwordField.setText("");
        setVisible(true);
        usernameField.requestFocusInWindow();
    }

}
