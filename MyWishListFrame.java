package org.example.person3;

import org.example.ui.Ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Person 3 — main screen: "My Wish List".
 *
 * Pass in the logged-in user's id (from Person 1's LOGIN response) and an
 * already-connected WishlistClient. This frame owns nothing else about the
 * network — it just calls the client and refreshes the table.
 *
 * Quick manual test: run the main() at the bottom (edit the userId first).
 */
public class MyWishListFrame extends JFrame {

    private final WishlistClient client;
    private final int userId;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private List<WishlistItem> currentItems;

    public MyWishListFrame(WishlistClient client, int userId) {
        super("My Wish List");
        this.client = client;
        this.userId = userId;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // FIX (Person 6): the socket opened for this screen (in MainDashboard.openMyWishList)
        // was never closed, so every open/close leaked one connection. Close it when this
        // window closes.
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                client.close();
            }
        });
        setSize(680, 460);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Ui.BG);
        setLayout(new BorderLayout(0, 16));

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Ui.BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        root.add(Ui.label("My Wish List", Font.BOLD, 22, Ui.TEXT), BorderLayout.NORTH);

        // --- Table -----------------------------------------------------
        tableModel = new DefaultTableModel(
                new Object[]{"Item", "Target Price", "Raised", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false; // editing happens through the dialog, not in-cell
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(Ui.font(Font.PLAIN, 14));
        table.setSelectionBackground(Ui.SELECTION);
        table.setSelectionForeground(Ui.TEXT);
        table.setGridColor(Ui.BORDER);
        table.getTableHeader().setFont(Ui.font(Font.BOLD, 13));
        table.getTableHeader().setBackground(Ui.CARD);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        root.add(Ui.scroll(table), BorderLayout.CENTER);

        // --- Buttons -----------------------------------------------------
        Ui.RoundedButton addButton = new Ui.RoundedButton("Add New Item", Ui.PRIMARY);
        Ui.RoundedButton editButton = new Ui.RoundedButton("Edit Selected", Ui.NEUTRAL);
        Ui.RoundedButton deleteButton = new Ui.RoundedButton("Delete Selected", Ui.DANGER);
        Ui.RoundedButton refreshButton = new Ui.RoundedButton("Refresh", Ui.SUCCESS);

        JPanel buttonPanel = Ui.buttonBar(addButton, editButton, deleteButton, refreshButton);
        root.add(buttonPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> onAdd());
        editButton.addActionListener(e -> onEdit());
        deleteButton.addActionListener(e -> onDelete());
        refreshButton.addActionListener(e -> refresh());

        add(root, BorderLayout.CENTER);

        refresh();
    }

    /** Reloads the table from the server. */
    private void refresh() {
        try {
            currentItems = client.getMyWishlist(userId);
            tableModel.setRowCount(0);
            for (WishlistItem item : currentItems) {
                tableModel.addRow(new Object[]{
                        item.getName(),
                        String.format("%.2f", item.getPrice()),
                        String.format("%.2f", item.getAmountRaised()),
                        item.isCompleted() ? "Completed" : "In progress"
                });
            }
        } catch (IOException | WishlistException ex) {
            showError("Could not load your wish list: " + ex.getMessage());
        }
    }

    private void onAdd() {
        AddEditItemDialog dialog = AddEditItemDialog.forCreate(this, client, userId);
        dialog.setVisible(true);
        if (dialog.wasSaved()) {
            refresh();
        }
    }

    private void onEdit() {
        WishlistItem selected = getSelectedItem();
        if (selected == null) {
            showError("Select an item to edit first.");
            return;
        }
        AddEditItemDialog dialog = AddEditItemDialog.forEdit(this, client, selected);
        dialog.setVisible(true);
        if (dialog.wasSaved()) {
            refresh();
        }
    }

    private void onDelete() {
        WishlistItem selected = getSelectedItem();
        if (selected == null) {
            showError("Select an item to delete first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Remove \"" + selected.getName() + "\" from your wish list?",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            client.deleteWishlistItem(selected.getId());
            refresh();
        } catch (IOException | WishlistException ex) {
            showError("Could not delete item: " + ex.getMessage());
        }
    }

    private WishlistItem getSelectedItem() {
        int row = table.getSelectedRow();
        if (row < 0 || currentItems == null || row >= currentItems.size()) return null;
        return currentItems.get(row);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ---------------------------------------------------------------
    // Standalone manual test — wire this up to your real login flow
    // instead of hardcoding userId once Person 1's screen is ready.
    // ---------------------------------------------------------------
    public static void main(String[] args) {
        Ui.installLookAndFeel();
        SwingUtilities.invokeLater(() -> {
            try {
                WishlistClient client = new WishlistClient("localhost", 5000);
                int testUserId = 1; // <-- replace with the real logged-in user's id
                new MyWishListFrame(client, testUserId).setVisible(true);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                        "Could not connect to server: " + ex.getMessage());
            }
        });
    }
}
