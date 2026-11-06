import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.List;

public class Amain {
    /** Starts the application on Swing's event-dispatch thread. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    // Matches Swing controls to the operating system's appearance.
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception exception) {
                    System.err.println("Could not apply the system look and feel: " + exception.getMessage());
                }
                // Keep the POS instance alive across logouts to retain the app session.
                final LoginFrame[] loginFrame = new LoginFrame[1];
                final MilkTeaPOS[] pos = new MilkTeaPOS[1];
                loginFrame[0] = new LoginFrame((role, username) -> {
                    if (pos[0] == null) {
                        pos[0] = new MilkTeaPOS(role, username, () -> {
                            pos[0].setVisible(false);
                            loginFrame[0].showLogin();
                        }, new LoginFrame.AccountManagementListener() {
                            @Override
                            public String getUsername(LoginFrame.UserRole accountRole) {
                                return loginFrame[0].getAccountUsername(accountRole);
                            }

                            @Override
                            public void updateCredentials(LoginFrame.UserRole accountRole,
                                    String accountUsername, String accountPassword) {
                                loginFrame[0].updateAccountFromAdmin(
                                        accountRole, accountUsername, accountPassword);
                            }

                            @Override
                            public List<String> getEmployeeUsernames() {
                                return loginFrame[0].getEmployeeUsernames();
                            }

                            @Override
                            public void updateEmployeeCredentials(int accountIndex,
                                    String accountUsername, String accountPassword) {
                                loginFrame[0].updateEmployeeCredentials(
                                        accountIndex, accountUsername, accountPassword);
                            }

                            @Override
                            public void addEmployeeAccount(
                                    String accountUsername, String accountPassword) {
                                loginFrame[0].addEmployeeAccount(
                                        accountUsername, accountPassword);
                            }

                            @Override
                            public void deleteEmployeeAccount(int accountIndex) {
                                loginFrame[0].deleteEmployeeAccount(accountIndex);
                            }
                        });
                    } else {
                        pos[0].setUserRole(role, username);
                    }
                    pos[0].setVisible(true);
                });
                loginFrame[0].setVisible(true);
            }
        });
    }
}
