import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class CoffeeShopUI extends JFrame implements ActionListener {

    private JPanel contentPane;
    private JTable menuTable;
    private JTable cartTable;
    private JButton addToCartButton;
    private JButton placeOrderButton;
    private JButton resetButton;
    private JTextField quantityTextField;
    private DefaultTableModel menuTableModel;
    private DefaultTableModel cartTableModel;
    private Connection connection;

    public CoffeeShopUI() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/coffee_shop", "root", "");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 800, 600);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        JLabel menuLabel = new JLabel("Menu");
        menuLabel.setFont(new Font("Tahoma", Font.PLAIN, 20));
        contentPane.add(menuLabel, BorderLayout.NORTH);

        JScrollPane menuScrollPane = new JScrollPane();
        contentPane.add(menuScrollPane, BorderLayout.WEST);

        menuTable = new JTable();
        menuScrollPane.setViewportView(menuTable);

        JLabel cartLabel = new JLabel("Cart");
        cartLabel.setFont(new Font("Tahoma", Font.PLAIN, 20));
        contentPane.add(cartLabel, BorderLayout.EAST);

        JScrollPane cartScrollPane = new JScrollPane();
        contentPane.add(cartScrollPane, BorderLayout.CENTER);

        cartTable = new JTable();
        cartScrollPane.setViewportView(cartTable);

        JPanel buttonPanel = new JPanel();
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        JLabel quantityLabel = new JLabel("Quantity:");
        buttonPanel.add(quantityLabel);

        quantityTextField = new JTextField();
        quantityTextField.setColumns(10);
        buttonPanel.add(quantityTextField);

        addToCartButton = new JButton("Add to Cart");
        addToCartButton.addActionListener(this);
        buttonPanel.add(addToCartButton);

        resetButton = new JButton("Reset Cart");
        resetButton.addActionListener(this);
        buttonPanel.add(resetButton);

        placeOrderButton = new JButton("Place Order");
        placeOrderButton.addActionListener(this);
        buttonPanel.add(placeOrderButton);

        menuTableModel = new DefaultTableModel(new Object[][]{},
                new String[]{"ID", "Name", "Description", "Price", "Image"});
        menuTable.setModel(menuTableModel);

        cartTableModel = new DefaultTableModel(new Object[][]{},
                new String[]{"ID", "Name", "Quantity", "Price"});
        cartTable.setModel(cartTableModel);

        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM coffee_menu");
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                String description = resultSet.getString("description");
                double price = resultSet.getDouble("price");
                String image = resultSet.getString("image");
                menuTableModel.addRow(new Object[]{id, name, description, price, image});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == addToCartButton) {
            int selectedRow         = menuTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a menu item.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int id = (int) menuTableModel.getValueAt(selectedRow, 0);
            String name = (String) menuTableModel.getValueAt(selectedRow, 1);
            double price = (double) menuTableModel.getValueAt(selectedRow, 3);
            int quantity = 1;
            try {
                quantity = Integer.parseInt(quantityTextField.getText().trim());
                if (quantity < 1) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid quantity.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int index = findItemIndexInCart(id);
            if (index == -1) {
                cartTableModel.addRow(new Object[]{id, name, quantity, price * quantity});
            } else {
                int oldQuantity = (int) cartTableModel.getValueAt(index, 2);
                double oldTotalPrice = (double) cartTableModel.getValueAt(index, 3);
                cartTableModel.setValueAt(oldQuantity + quantity, index, 2);
                cartTableModel.setValueAt(oldTotalPrice + price * quantity, index, 3);
            }
            quantityTextField.setText("");
        } else if (e.getSource() == resetButton) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to reset the cart?", "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                cartTableModel.setRowCount(0);
            }
        } else if (e.getSource() == placeOrderButton) {
            if (cartTableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "Please add items to the cart before placing an order.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            double totalPrice = 0;
            for (int i = 0; i < cartTableModel.getRowCount(); i++) {
                totalPrice += (double) cartTableModel.getValueAt(i, 3);
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to place the order for a total of " + totalPrice + "?", "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    PreparedStatement preparedStatement = connection.prepareStatement(
                            "INSERT INTO orders (total_price) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
                    preparedStatement.setDouble(1, totalPrice);
                    preparedStatement.executeUpdate();
                    ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        int orderId = generatedKeys.getInt(1);
                        for (int i = 0; i < cartTableModel.getRowCount(); i++) {
                            int itemId = (int) cartTableModel.getValueAt(i, 0);
                            int quantity = (int) cartTableModel.getValueAt(i, 2);
                            PreparedStatement itemStatement = connection.prepareStatement(
                                    "INSERT INTO order_items (order_id, item_id, quantity) VALUES (?, ?, ?)");
                            itemStatement.setInt(1, orderId);
                            itemStatement.setInt(2, itemId);
                            itemStatement.setInt(3, quantity);
                            itemStatement.executeUpdate();
                        }
                        JOptionPane.showMessageDialog(this, "Order placed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        cartTableModel.setRowCount(0);
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to place the order.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    
    private int findItemIndexInCart(int itemId) {
        for (int i = 0; i < cartTableModel.getRowCount(); i++) {
            if ((int) cartTableModel.getValueAt(i, 0) == itemId) {
                return i;
                }
                }
                return -1;
                }
                
                public static void main(String[] args) {
                EventQueue.invokeLater(() -> {
                try {
                CoffeeShopApplication window = new CoffeeShopApplication();
                ((Window) window.frame).setVisible(true);
                } catch (Exception e) {
                e.printStackTrace();
                }
                });
                }
                }
                
                
                
                
                
    
