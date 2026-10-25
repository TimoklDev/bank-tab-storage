package com.banktabstorage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

final class BankTabStoragePanel extends PluginPanel
{
    private static final Color BACKGROUND = new Color(40, 40, 40);
    private static final Color SURFACE = new Color(48, 48, 48);
    private static final Color SELECTED = new Color(69, 56, 47);
    private static final Color BUTTON = new Color(57, 57, 57);
    private static final Color ACCENT = new Color(242, 138, 58);
    private static final Color GREEN = new Color(122, 184, 111);
    private static final Color MUTED = new Color(190, 190, 190);
    private static final Font BODY_FONT = new Font(Font.DIALOG, Font.PLAIN, 12);
    private static final Font NAME_FONT = new Font(Font.DIALOG, Font.BOLD, 13);
    private static final Font DETAIL_FONT = new Font(Font.DIALOG, Font.PLAIN, 11);
    private final StorageActions actions;
    private final IntFunction<BufferedImage> imageProvider;
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    private final DefaultTreeModel treeModel = new DefaultTreeModel(root, true);
    private final JTree tree = new JTree(treeModel)
    {
        private int rowWidth;

        @Override
        public boolean getScrollableTracksViewportWidth()
        {
            return true;
        }

        @Override
        public void doLayout()
        {
            super.doLayout();
            if (rowWidth != getWidth())
            {
                rowWidth = getWidth();
                refreshRowSizes(root);
            }
        }

        @Override
        protected void paintComponent(Graphics graphics)
        {
            super.paintComponent(graphics);
            Graphics2D painter = (Graphics2D) graphics.create();
            painter.setFont(DETAIL_FONT);
            painter.setColor(MUTED);
            painter.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (root.getChildCount() == 0)
            {
                painter.drawString("No saved tabs yet", 8, 22);
                painter.drawString("Save your current Bank Tags tabs.", 8, 40);
            }
            JTree.DropLocation drop = getDropLocation();
            if (drop != null && isOutsideRows(drop.getDropPoint()))
            {
                Rectangle last = getRowCount() == 0 ? null : getRowBounds(getRowCount() - 1);
                int y = last == null ? 6 : last.y + last.height + 5;
                painter.setColor(ACCENT);
                painter.fillRect(2, y, Math.max(0, getWidth() - 4), 2);
                painter.drawString("Move outside folders", 8, y + 17);
            }
            painter.dispose();
        }
    };
    private final Map<Integer, BufferedImage> itemImages = new HashMap<>();
    private final Map<String, StoredBankTag> tabsByName = new HashMap<>();
    private final JLabel countLabel = new JLabel();
    private final JLabel noticeLabel = new JLabel("Drag to organize. Right-click for more.");
    private final JButton loadButton;
    private final JButton toggleButton;
    private StorageLayout storageLayout = StorageLayout.empty();
    private boolean updating;

    BankTabStoragePanel(StorageActions actions, IntFunction<BufferedImage> imageProvider)
    {
        super(false);
        this.actions = actions;
        this.imageProvider = imageProvider;
        setLayout(new BorderLayout(0, 7));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setBackground(BACKGROUND);
        setFont(BODY_FONT);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JPanel heading = new JPanel(new BorderLayout(4, 0));
        heading.setOpaque(false);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel title = new JLabel("Bank Tab Storage");
        title.setForeground(Color.WHITE);
        title.setFont(NAME_FONT.deriveFont(14f));
        heading.add(title, BorderLayout.CENTER);
        JButton menuButton = button("\u22EE", MUTED);
        menuButton.setFont(NAME_FONT.deriveFont(17f));
        menuButton.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        menuButton.setContentAreaFilled(false);
        menuButton.setToolTipText("Import and export");
        menuButton.getAccessibleContext().setAccessibleName("Import and export menu");
        menuButton.addActionListener(event ->
        {
            JPopupMenu menu = new JPopupMenu();
            addMenuItem(menu, "Import tabs and folders", this::importFromClipboard);
            addMenuItem(menu, "Export all", this::exportToClipboard);
            menu.show(menuButton, 0, menuButton.getHeight());
        });
        heading.add(menuButton, BorderLayout.EAST);
        header.add(heading);
        header.add(Box.createVerticalStrut(3));
        countLabel.setForeground(MUTED);
        countLabel.setFont(DETAIL_FONT);
        countLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(countLabel);
        header.add(Box.createVerticalStrut(7));

        JPanel toolbar = new JPanel(new GridLayout(1, 2, 5, 0));
        toolbar.setOpaque(false);
        toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 27));
        JButton saveButton = button("Save tabs", ACCENT);
        saveButton.setToolTipText("Save or update every currently visible Bank Tags tab");
        saveButton.addActionListener(event -> actions.saveCurrentTabs());
        toolbar.add(saveButton);
        JButton folderButton = button("New folder", Color.WHITE);
        folderButton.addActionListener(event -> createFolder(null));
        toolbar.add(folderButton);
        header.add(toolbar);
        add(header, BorderLayout.NORTH);

        configureTree();
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(BACKGROUND);
        add(scrollPane, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 5));
        footer.setOpaque(false);
        JPanel selectedActions = new JPanel(new GridLayout(1, 2, 5, 0));
        selectedActions.setOpaque(false);
        loadButton = button("Load", Color.WHITE);
        loadButton.addActionListener(event -> primarySelected());
        selectedActions.add(loadButton);
        toggleButton = button("Show", GREEN);
        toggleButton.addActionListener(event -> secondarySelected());
        selectedActions.add(toggleButton);
        footer.add(selectedActions, BorderLayout.NORTH);
        noticeLabel.setForeground(MUTED);
        noticeLabel.setFont(DETAIL_FONT);
        noticeLabel.setPreferredSize(new Dimension(180, 16));
        footer.add(noticeLabel, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        updateSelection();
        updateCount();
    }

    @Override
    public void paint(Graphics graphics)
    {
        Graphics2D painter = (Graphics2D) graphics.create();
        painter.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        painter.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        super.paint(painter);
        painter.dispose();
    }

    void setTabs(List<StoredBankTag> tabs, String notice)
    {
        setStorage(tabs, storageLayout.reconcile(tabs), notice);
    }

    void setStorage(List<StoredBankTag> tabs, StorageLayout layout, String notice)
    {
        StoredStorageEntry selected = selectedEntry();
        String selectedId = selected == null ? null : selected.getId();
        Set<String> previousIds = new HashSet<>();
        for (StoredStorageEntry entry : storageLayout.getEntries())
        {
            previousIds.add(entry.getId());
        }
        storageLayout = layout.reconcile(tabs);
        tabsByName.clear();
        Set<Integer> currentImageIds = new HashSet<>();
        for (StoredBankTag tab : tabs)
        {
            tabsByName.put(tab.getName(), tab.copy());
            currentImageIds.add(tab.getIconItemId());
        }
        for (StoredStorageEntry entry : storageLayout.getEntries())
        {
            if (entry.isFolder() && entry.getIconItemId() >= 0)
            {
                currentImageIds.add(entry.getIconItemId());
            }
            if (entry.isFolder() && !previousIds.contains(entry.getId()))
            {
                selectedId = entry.getId();
            }
        }
        itemImages.keySet().retainAll(currentImageIds);
        currentImageIds.forEach(this::itemImage);
        updating = true;
        try
        {
            root.removeAllChildren();
            Map<String, DefaultMutableTreeNode> nodes = new HashMap<>();
            for (StoredStorageEntry entry : storageLayout.getEntries())
            {
                nodes.put(entry.getId(), new DefaultMutableTreeNode(entry, entry.isFolder()));
            }
            for (StoredStorageEntry entry : storageLayout.getEntries())
            {
                DefaultMutableTreeNode parent = entry.getParentId() == null ? root : nodes.get(entry.getParentId());
                parent.add(nodes.get(entry.getId()));
            }
            treeModel.reload();
            tree.expandPath(new TreePath(root.getPath()));
            restoreExpansion(root);
            if (selectedId != null && nodes.containsKey(selectedId))
            {
                tree.setSelectionPath(new TreePath(nodes.get(selectedId).getPath()));
            }
            else if (root.getChildCount() > 0)
            {
                tree.setSelectionPath(new TreePath(((DefaultMutableTreeNode) root.getChildAt(0)).getPath()));
            }
        }
        finally
        {
            updating = false;
        }
        if (notice != null && !notice.isEmpty())
        {
            noticeLabel.setText(notice);
            noticeLabel.setToolTipText(notice);
        }
        updateCount();
        updateSelection();
    }

    void showError(String message)
    {
        JOptionPane.showMessageDialog(this, message, "Bank Tab Storage", JOptionPane.ERROR_MESSAGE);
    }

    private void configureTree()
    {
        tree.setFont(BODY_FONT);
        tree.setBackground(BACKGROUND);
        tree.setForeground(Color.WHITE);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(0);
        tree.setToggleClickCount(2);
        tree.putClientProperty("JTree.lineStyle", "None");
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new EntryRenderer());
        tree.setDragEnabled(!GraphicsEnvironment.isHeadless());
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setTransferHandler(new EntryTransferHandler());
        ToolTipManager.sharedInstance().registerComponent(tree);
        if (tree.getUI() instanceof BasicTreeUI)
        {
            BasicTreeUI ui = (BasicTreeUI) tree.getUI();
            ui.setLeftChildIndent(5);
            ui.setRightChildIndent(10);
        }
        tree.addTreeSelectionListener(event ->
        {
            if (!updating)
            {
                updateSelection();
            }
        });
        tree.addTreeExpansionListener(new TreeExpansionListener()
        {
            @Override
            public void treeExpanded(TreeExpansionEvent event)
            {
                expanded(event, true);
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event)
            {
                expanded(event, false);
            }
        });
        tree.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event))
                {
                    StoredStorageEntry entry = selectedEntry();
                    if (entry != null && !entry.isFolder())
                    {
                        primarySelected();
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent event)
            {
                popup(event);
            }

            @Override
            public void mouseReleased(MouseEvent event)
            {
                popup(event);
            }
        });
    }

    private void refreshRowSizes(DefaultMutableTreeNode node)
    {
        treeModel.nodeChanged(node);
        for (int index = 0; index < node.getChildCount(); index++)
        {
            refreshRowSizes((DefaultMutableTreeNode) node.getChildAt(index));
        }
    }

    private void expanded(TreeExpansionEvent event, boolean expanded)
    {
        Object value = ((DefaultMutableTreeNode) event.getPath().getLastPathComponent()).getUserObject();
        if (!updating && value instanceof StoredStorageEntry)
        {
            String id = ((StoredStorageEntry) value).getId();
            storageLayout = storageLayout.updateFolder(id, entry -> entry.withExpanded(expanded));
            actions.setFolderExpanded(id, expanded);
        }
    }

    private void restoreExpansion(DefaultMutableTreeNode parent)
    {
        for (int index = 0; index < parent.getChildCount(); index++)
        {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(index);
            StoredStorageEntry entry = (StoredStorageEntry) child.getUserObject();
            if (entry.isFolder())
            {
                restoreExpansion(child);
                TreePath path = new TreePath(child.getPath());
                if (entry.isExpanded())
                {
                    tree.expandPath(path);
                }
                else
                {
                    tree.collapsePath(path);
                }
            }
        }
    }

    private JButton button(String text, Color foreground)
    {
        JButton button = new JButton(text);
        button.setFont(BODY_FONT);
        button.setFocusPainted(false);
        button.setBackground(BUTTON);
        button.setForeground(foreground);
        button.setMargin(new Insets(3, 4, 3, 4));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(73, 73, 73)),
            BorderFactory.createEmptyBorder(4, 5, 4, 5)));
        return button;
    }

    private StoredStorageEntry selectedEntry()
    {
        Object selected = tree.getLastSelectedPathComponent();
        if (!(selected instanceof DefaultMutableTreeNode))
        {
            return null;
        }
        Object value = ((DefaultMutableTreeNode) selected).getUserObject();
        return value instanceof StoredStorageEntry ? (StoredStorageEntry) value : null;
    }

    private void createFolder()
    {
        StoredStorageEntry selected = selectedEntry();
        String parentId = selected == null ? null : selected.isFolder() ? selected.getId() : selected.getParentId();
        createFolder(parentId);
    }

    private void createFolder(String parentId)
    {
        String name = JOptionPane.showInputDialog(this, "Folder name", "New folder", JOptionPane.PLAIN_MESSAGE);
        if (name != null)
        {
            actions.createFolder(name, parentId);
        }
    }

    private boolean isOutsideRows(Point point)
    {
        Rectangle last = tree.getRowCount() == 0 ? null : tree.getRowBounds(tree.getRowCount() - 1);
        return last == null || point.y >= last.y + last.height;
    }

    private StorageDropTarget dropTarget(TreePath path, int childIndex, Point point)
    {
        if (path == null || isOutsideRows(point))
        {
            return new StorageDropTarget(null, -1);
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (!node.getAllowsChildren())
        {
            return null;
        }
        StoredStorageEntry entry = node.getUserObject() instanceof StoredStorageEntry
            ? (StoredStorageEntry) node.getUserObject() : null;
        return new StorageDropTarget(entry == null ? null : entry.getId(), childIndex);
    }

    boolean moveEntryToDrop(String id, TreePath path, int childIndex, Point point)
    {
        StorageDropTarget target = dropTarget(path, childIndex, point);
        if (target == null || !storageLayout.canMove(id, target.parentId))
        {
            return false;
        }
        actions.moveEntry(id, target.parentId, target.index);
        return true;
    }

    private static final class StorageDropTarget
    {
        private final String parentId;
        private final int index;

        private StorageDropTarget(String parentId, int index)
        {
            this.parentId = parentId;
            this.index = index;
        }
    }

    private void primarySelected()
    {
        StoredStorageEntry entry = selectedEntry();
        if (entry == null)
        {
            return;
        }
        if (entry.isFolder())
        {
            actions.chooseFolderIcon(entry.getId());
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
            "Load '" + entry.getName() + "'? This replaces its current tagged items, icon, and layout.",
            "Load saved tab", JOptionPane.OK_CANCEL_OPTION);
        if (choice == JOptionPane.OK_OPTION)
        {
            actions.loadTab(entry.getName());
        }
    }

    private void secondarySelected()
    {
        StoredStorageEntry entry = selectedEntry();
        if (entry == null)
        {
            return;
        }
        if (entry.isFolder())
        {
            String name = (String) JOptionPane.showInputDialog(this, "Folder name", "Rename folder",
                JOptionPane.PLAIN_MESSAGE, null, null, entry.getName());
            if (name != null)
            {
                actions.renameFolder(entry.getId(), name);
            }
        }
        else
        {
            StoredBankTag tab = tabsByName.get(entry.getName());
            actions.setTabEnabled(tab.getName(), !tab.isEnabled());
        }
    }

    private void removeSelected()
    {
        StoredStorageEntry entry = selectedEntry();
        if (entry == null)
        {
            return;
        }
        String message = entry.isFolder()
            ? "Remove folder '" + entry.getName() + "'? Its tabs and subfolders will be kept."
            : "Remove '" + entry.getName() + "' from storage? The live Bank Tags data will be kept.";
        if (JOptionPane.showConfirmDialog(this, message, "Remove from storage", JOptionPane.OK_CANCEL_OPTION)
            == JOptionPane.OK_OPTION)
        {
            if (entry.isFolder())
            {
                actions.removeFolder(entry.getId());
            }
            else
            {
                actions.removeTab(entry.getName());
            }
        }
    }

    private void popup(MouseEvent event)
    {
        if (!event.isPopupTrigger())
        {
            return;
        }
        tree.setSelectionPath(tree.getPathForLocation(event.getX(), event.getY()));
        JPopupMenu menu = new JPopupMenu();
        StoredStorageEntry entry = selectedEntry();
        addMenuItem(menu, "New folder", this::createFolder);
        if (entry != null)
        {
            menu.addSeparator();
            addMenuItem(menu, entry.isFolder() ? "Change icon" : "Load saved tab", this::primarySelected);
            addMenuItem(menu, entry.isFolder() ? "Rename folder" : toggleButton.getText(), this::secondarySelected);
            menu.addSeparator();
            addMenuItem(menu, "Move up", () -> moveSelected(-1));
            addMenuItem(menu, "Move down", () -> moveSelected(1));
            if (entry.getParentId() != null)
            {
                addMenuItem(menu, "Move out of folder", () -> actions.moveEntry(entry.getId(),
                    storageLayout.find(entry.getParentId()).getParentId(), -1));
                addMenuItem(menu, "Move to top level", () -> actions.moveEntry(entry.getId(), null, -1));
            }
            menu.addSeparator();
            addMenuItem(menu, entry.isFolder() ? "Remove folder" : "Remove saved tab", this::removeSelected);
        }
        menu.show(tree, event.getX(), event.getY());
    }

    private void addMenuItem(JPopupMenu menu, String text, Runnable action)
    {
        JMenuItem item = new JMenuItem(text);
        item.setFont(BODY_FONT);
        item.addActionListener(event -> action.run());
        menu.add(item);
    }

    private void moveSelected(int offset)
    {
        StoredStorageEntry entry = selectedEntry();
        if (entry != null)
        {
            List<StoredStorageEntry> siblings = storageLayout.children(entry.getParentId());
            int position = siblings.indexOf(storageLayout.find(entry.getId()));
            int target = position + offset;
            if (target >= 0 && target < siblings.size())
            {
                actions.moveEntry(entry.getId(), entry.getParentId(), offset > 0 ? position + 2 : position - 1);
            }
        }
    }

    private void importFromClipboard()
    {
        try
        {
            Object value = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (value == null)
            {
                throw new IllegalArgumentException("The clipboard does not contain text");
            }
            Object[] options = {"Merge", "Replace", "Cancel"};
            int choice = JOptionPane.showOptionDialog(this,
                "How should the clipboard tabs and folders be imported?", "Import saved storage",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (choice == 0 || choice == 1)
            {
                actions.importText(value.toString(), choice == 1);
            }
        }
        catch (Exception exception)
        {
            showError(exception.getMessage() == null ? "Unable to read the clipboard" : exception.getMessage());
        }
    }

    private void exportToClipboard()
    {
        try
        {
            StringSelection selection = new StringSelection(actions.exportText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            noticeLabel.setText("Copied saved tabs and folders to the clipboard");
        }
        catch (Exception exception)
        {
            showError(exception.getMessage() == null ? "Unable to write to the clipboard" : exception.getMessage());
        }
    }

    private void updateSelection()
    {
        StoredStorageEntry entry = selectedEntry();
        loadButton.setEnabled(entry != null);
        toggleButton.setEnabled(entry != null);
        boolean folder = entry != null && entry.isFolder();
        loadButton.setText(folder ? "Icon" : "Load");
        StoredBankTag tab = entry == null || folder ? null : tabsByName.get(entry.getName());
        toggleButton.setText(folder ? "Rename" : tab != null && tab.isEnabled() ? "Hide" : "Show");
        toggleButton.setForeground(folder ? Color.WHITE : tab != null && tab.isEnabled() ? ACCENT : GREEN);
    }

    private void updateCount()
    {
        long visible = tabsByName.values().stream().filter(StoredBankTag::isEnabled).count();
        countLabel.setText(tabsByName.size() + " saved, " + visible + " in bank");
    }

    private BufferedImage itemImage(int itemId)
    {
        if (itemId < 0)
        {
            return null;
        }
        BufferedImage image = itemImages.get(itemId);
        if (image == null)
        {
            image = imageProvider.apply(itemId);
            if (image != null)
            {
                itemImages.put(itemId, image);
                if (image instanceof AsyncBufferedImage)
                {
                    ((AsyncBufferedImage) image).onLoaded(() -> SwingUtilities.invokeLater(tree::repaint));
                }
            }
        }
        return image;
    }

    private final class EntryRenderer extends JPanel implements TreeCellRenderer
    {
        private StoredStorageEntry entry;
        private boolean selected;

        @Override
        public Component getTreeCellRendererComponent(JTree source, Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean focused)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            this.entry = node.getUserObject() instanceof StoredStorageEntry
                ? (StoredStorageEntry) node.getUserObject() : null;
            this.selected = selected;
            int width = Math.max(90, tree.getWidth() - 5 - node.getLevel() * 15);
            setPreferredSize(new Dimension(width, entry == null ? 0 : entry.isFolder() ? 44 : 48));
            StoredBankTag tab = entry == null || entry.isFolder() ? null : tabsByName.get(entry.getName());
            setToolTipText(entry == null ? null : entry.getName()
                + (entry.isFolder() ? " | Drop tabs here. Right-click for folder actions."
                    : tab != null && tab.isEnabled() ? " | Visible in bank" : " | Hidden from bank"));
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics)
        {
            Graphics2D painter = (Graphics2D) graphics.create();
            painter.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            painter.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            painter.setColor(BACKGROUND);
            painter.fillRect(0, 0, getWidth(), getHeight());
            if (entry == null)
            {
                painter.dispose();
                return;
            }
            if (selected || entry.isFolder())
            {
                painter.setColor(selected ? SELECTED : SURFACE);
                painter.fillRoundRect(0, 0, getWidth(), getHeight() - 2, 4, 4);
            }
            StoredBankTag tab = entry.isFolder() ? null : tabsByName.get(entry.getName());
            if (selected)
            {
                painter.setColor(ACCENT);
                painter.fillRect(0, 5, 2, getHeight() - 12);
            }
            BufferedImage image = itemImage(entry.isFolder() ? entry.getIconItemId() : tab.getIconItemId());
            if (image != null)
            {
                painter.drawImage(image, 5, (getHeight() - 2 - image.getHeight()) / 2, null);
            }
            else if (entry.isFolder())
            {
                painter.setColor(ACCENT);
                painter.fillRoundRect(9, 16, 25, 17, 3, 3);
                painter.fillRoundRect(9, 12, 11, 7, 3, 3);
                painter.setColor(new Color(184, 98, 39));
                painter.drawLine(10, 19, 32, 19);
            }
            int nameY = getHeight() / 2 - 4;
            int detailY = getHeight() / 2 + 12;
            painter.setFont(entry.isFolder() ? NAME_FONT : NAME_FONT.deriveFont(Font.PLAIN));
            painter.setColor(Color.WHITE);
            painter.drawString(clipText(entry.getName(), painter.getFontMetrics(), getWidth() - 54), 46, nameY);
            painter.setFont(DETAIL_FONT);
            painter.setColor(MUTED);
            if (entry.isFolder())
            {
                painter.drawString(clipText(folderDetails(entry.getId()), painter.getFontMetrics(), getWidth() - 54),
                    46, detailY);
            }
            else
            {
                String state = tab.isEnabled() ? "Visible" : "Hidden";
                int stateWidth = painter.getFontMetrics().stringWidth(state);
                String details = tab.getItemCount() + (tab.getItemCount() == 1 ? " item" : " items");
                painter.drawString(clipText(details, painter.getFontMetrics(), getWidth() - 62 - stateWidth),
                    46, detailY);
                painter.setColor(tab.isEnabled() ? GREEN : MUTED);
                painter.drawString(state, getWidth() - stateWidth - 8, detailY);
                painter.setColor(new Color(51, 51, 51));
                painter.drawLine(46, getHeight() - 1, getWidth() - 8, getHeight() - 1);
            }
            JTree.DropLocation drop = tree.getDropLocation();
            if (drop != null && drop.getChildIndex() == -1 && drop.getPath() != null)
            {
                Object target = ((DefaultMutableTreeNode) drop.getPath().getLastPathComponent()).getUserObject();
                if (target instanceof StoredStorageEntry && entry.getId().equals(((StoredStorageEntry) target).getId()))
                {
                    painter.setColor(ACCENT);
                    painter.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 3, 4, 4);
                }
            }
            painter.dispose();
        }

        private String folderDetails(String id)
        {
            List<StoredStorageEntry> children = storageLayout.children(id);
            long folders = children.stream().filter(StoredStorageEntry::isFolder).count();
            long tabs = children.size() - folders;
            return tabs + (tabs == 1 ? " tab" : " tabs")
                + (folders == 0 ? "" : "  |  " + folders + (folders == 1 ? " folder" : " folders"));
        }

        private String clipText(String value, FontMetrics metrics, int width)
        {
            if (width <= 0)
            {
                return "";
            }
            if (metrics.stringWidth(value) <= width)
            {
                return value;
            }
            int end = value.length();
            while (end > 0 && metrics.stringWidth(value.substring(0, end) + "...") > width)
            {
                end = value.offsetByCodePoints(end, -1);
            }
            return value.substring(0, end) + "...";
        }
    }

    private static final class EntryTransfer implements Transferable
    {
        private static final DataFlavor FLAVOR = new DataFlavor(
            DataFlavor.javaJVMLocalObjectMimeType + ";class=java.lang.Object", "Bank Tab Storage entry");
        private final JTree source;
        private final String id;

        private EntryTransfer(JTree source, String id)
        {
            this.source = source;
            this.id = id;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors()
        {
            return new DataFlavor[]{FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
            return FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
        {
            if (!isDataFlavorSupported(flavor))
            {
                throw new UnsupportedFlavorException(flavor);
            }
            return this;
        }
    }

    private final class EntryTransferHandler extends TransferHandler
    {
        @Override
        protected Transferable createTransferable(JComponent component)
        {
            StoredStorageEntry entry = selectedEntry();
            return entry == null ? null : new EntryTransfer(tree, entry.getId());
        }

        @Override
        public int getSourceActions(JComponent component)
        {
            return MOVE;
        }

        @Override
        public boolean canImport(TransferSupport support)
        {
            if (!support.isDrop() || !support.isDataFlavorSupported(EntryTransfer.FLAVOR))
            {
                return false;
            }
            JTree.DropLocation location = (JTree.DropLocation) support.getDropLocation();
            StorageDropTarget target = dropTarget(location.getPath(), location.getChildIndex(), location.getDropPoint());
            if (target == null)
            {
                return false;
            }
            try
            {
                EntryTransfer transfer = (EntryTransfer) support.getTransferable().getTransferData(EntryTransfer.FLAVOR);
                return transfer.source == tree && storageLayout.canMove(transfer.id, target.parentId);
            }
            catch (Exception exception)
            {
                return false;
            }
        }

        @Override
        public boolean importData(TransferSupport support)
        {
            if (!canImport(support))
            {
                return false;
            }
            try
            {
                EntryTransfer transfer = (EntryTransfer) support.getTransferable().getTransferData(EntryTransfer.FLAVOR);
                JTree.DropLocation location = (JTree.DropLocation) support.getDropLocation();
                return moveEntryToDrop(transfer.id, location.getPath(), location.getChildIndex(), location.getDropPoint());
            }
            catch (Exception exception)
            {
                showError("Unable to move the selected tab or folder");
                return false;
            }
        }
    }
}
