package org.example.person3;

import org.example.ui.Ui;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * One dialog, two modes:
 *  - CREATE: pick a catalog item (GET_AVAILABLE_ITEMS) + set a target price,
 *            then CREATE_WISHLIST_ITEM.
 *  - EDIT:   change the target price of an item already on the wish list,
 *            then UPDATE_WISHLIST_ITEM.
 *
 * (The server protocol only supports changing price on update — the catalog
 * item itself can't be swapped after creation. If you want to change the
 * item, delete it and add a new one.)
 */
public class AddEditItemDialog extends JDialog {

    private enum Mode { CREATE, EDIT }

    private final WishlistClient client;
    private final Mode mode;
    private final int userId;          // used in CREATE
    private final WishlistItem editingItem; // used in EDIT

    private JComboBox<CatalogItem> catalogCombo; // CREATE only
    private JTextField priceField;
    private boolean saved = false;

    private AddEditItemDialog(Frame owner, WishlistClient client, Mode mode,
                               int userId, WishlistItem editingItem) {
        super(owner, mode == Mode.CREATE ? "Add Wish List Item" : "Edit Wish List Item", true);
        this.client = client;
        this.mode = mode;
        this.userId = userId;
        this.editingItem = editingItem;

        getContentPane().setBackground(Ui.BG);
        buildUi();
        pack();
        setLocationRelativeTo(owner);
    }

    public static AddEditItemDialog forCreate(Frame owner, WishlistClient client, int userId) {
        return new AddEditItemDialog(owner, client, Mode.CREATE, userId, null);
    }

    public static AddEditItemDialog forEdit(Frame owner, WishlistClient client, WishlistItem item) {
        return new AddEditItemDialog(owner, client, Mode.EDIT, -1, item);
    }

    public boolean wasSaved() {
        return saved;
    }

    private void buildUi() {
        JPanel content = Ui.card(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        if (mode == Mode.CREATE) {
            gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.WEST;
            content.add(Ui.label("Item:", Font.PLAIN, 13, Ui.TEXT), gbc);

            catalogCombo = new JComboBox<>();
            catalogCombo.setFont(Ui.font(Font.PLAIN, 14));
            loadCatalogItems();
            catalogCombo.addActionListener(e -> onCatalogSelectionChanged());
            gbc.gridx = 1;
            content.add(catalogCombo, gbc);
            row++;
        } else {
            gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.WEST;
            content.add(Ui.label("Item:", Font.PLAIN, 13, Ui.TEXT), gbc);
            gbc.gridx = 1;
            content.add(Ui.label(editingItem.getName(), Font.BOLD, 14, Ui.TEXT), gbc); // name isn't editable, just shown
            row++;
        }

        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.WEST;
        content.add(Ui.label("Target price ($):", Font.PLAIN, 13, Ui.TEXT), gbc);
        priceField = new JTextField(10);
        Ui.styleField(priceField);
        if (mode == Mode.EDIT) {
            priceField.setText(String.valueOf(editingItem.getPrice()));
        }
        gbc.gridx = 1;
        content.add(priceField, gbc);
        row++;

        Ui.RoundedButton saveButton = new Ui.RoundedButton("Save", Ui.PRIMARY);
        Ui.RoundedButton cancelButton = new Ui.RoundedButton("Cancel", Ui.NEUTRAL);
        JPanel buttons = Ui.buttonBar(cancelButton, saveButton);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 8, 8, 8);
        content.add(buttons, gbc);

        saveButton.addActionListener(e -> onSave());
        cancelButton.addActionListener(e -> dispose());

        setContentPane(content);
    }

    private void loadCatalogItems() {
        try {
            List<CatalogItem> items = client.getAvailableItems();
            for (CatalogItem item : items) {
                catalogCombo.addItem(item);
            }
            if (!items.isEmpty()) {
                onCatalogSelectionChanged(); // pre-fill price with the first item's default
            }
        } catch (IOException | WishlistException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load catalog items: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Pre-fills the price field with the selected catalog item's default price (user can still change it). */
    private void onCatalogSelectionChanged() {
        CatalogItem selected = (CatalogItem) catalogCombo.getSelectedItem();
        if (selected != null) {
            priceField.setText(String.valueOf(selected.getDefaultPrice()));
        }
    }

    private void onSave() {
        double price;
        try {
            price = Double.parseDouble(priceField.getText().trim());
            if (price <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Enter a valid price greater than 0.",
                    "Invalid price", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (mode == Mode.CREATE) {
                CatalogItem selected = (CatalogItem) catalogCombo.getSelectedItem();
                if (selected == null) {
                    JOptionPane.showMessageDialog(this, "Choose an item.", "Missing item",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                client.createWishlistItem(userId, selected.getId(), price);
            } else {
                client.updateWishlistItemPrice(editingItem.getId(), price);
            }
            saved = true;
            dispose();
        } catch (IOException | WishlistException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not save item: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
