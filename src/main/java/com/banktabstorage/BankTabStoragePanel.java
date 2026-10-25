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
import javax.swing.JTextArea;
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
    private static final Color SURFACE = new Color(53, 53, 53);
    private static final Color SELECTED = new Color(72, 72, 72);
    private static final Color BUTTON = new Color(68, 68, 68);
    private static final Color GOLD = new Color(214, 180, 92);
    private static final Color GREEN = new Color(122, 184, 111);
    private static final Color MUTED = new Color(190, 190, 190);
    private static final Color DANGER = new Color(225, 121, 111);
    private static final Font BODY_FONT = new Font(Font.DIALOG, Font.PLAIN, 12);
    private static final Font NAME_FONT = new Font(Font.DIALOG, Font.BOLD, 13);
    private static final Font DETAIL_FONT = new Font(Font.DIALOG, Font.PLAIN, 11);
    private final StorageActions actions;
    private final IntFunction<BufferedImage> imageProvider;
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("All saved tabs");
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
    };
    private final Map<Integer, BufferedImage> itemImages = new HashMap<>();
    private final Map<String, StoredBankTag> tabsByName = new HashMap<>();
    private final JLabel countLabel = new JLabel();
    private final JTextArea noticeLabel = new JTextArea("Drag tabs into folders to organize them");
    private final JButton loadButton;
    private final JButton toggleButton;
    private final JButton removeButton;
    private StorageLayout storageLayout = StorageLayout.empty();
    private boolean updating;

    BankTabStoragePanel(StorageActions actions, IntFunction<BufferedImage> imageProvider)
    {
        super(false);
        this.actions = actions;
        this.imageProvider = imageProvider;
        setLayout(new BorderLayout(0, 9));
        setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        setBackground(BACKGROUND);
        setFont(BODY_FONT);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel title = new JLabel("Bank Tab Storage");
        title.setForeground(Color.WHITE);
        title.setFont(NAME_FONT.deriveFont(15f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(7));
        countLabel.setForeground(MUTED);
        countLabel.setFont(DETAIL_FONT);
        countLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(countLabel);
        header.add(Box.createVerticalStrut(9));

        JButton saveButton = button("Save current tabs", GOLD);
        saveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        saveButton.addActionListener(event -> actions.saveCurrentTabs());
        header.add(saveButton);
        header.add(Box.createVerticalStrut(5));
        JPanel transfers = new JPanel(new GridLayout(1, 2, 5, 0));
        transfers.setOpaque(false);
        transfers.setAlignmentX(Component.LEFT_ALIGNMENT);
        transfers.setMaximumSize(new Dimension(Integer.MAX_VALUE, 27));
        JButton importButton = button("Import", Color.WHITE);
        importButton.addActionListener(event -> importFromClipboard());
        transfers.add(importButton);
        JButton exportButton = button("Export all", Color.WHITE);
        exportButton.addActionListener(event -> exportToClipboard());
        transfers.add(exportButton);
        header.add(transfers);
        header.add(Box.createVerticalStrut(5));
        JButton folderButton = button("+ New folder", Color.WHITE);
        folderButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        folderButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        folderButton.addActionListener(event -> createFolder());
        header.add(folderButton);
        add(header, BorderLayout.NORTH);

        configureTree();
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(BACKGROUND);
        add(scrollPane, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 7));
        footer.setOpaque(false);
        JPanel selectedActions = new JPanel(new GridLayout(1, 3, 4, 0));
        selectedActions.setOpaque(false);
        loadButton = button("Load", Color.WHITE);
        loadButton.addActionListener(event -> primarySelected());
        selectedActions.add(loadButton);
        toggleButton = button("Show", GREEN);
        toggleButton.addActionListener(event -> secondarySelected());
        selectedActions.add(toggleButton);
        removeButton = button("Remove", DANGER);
        removeButton.addActionListener(event -> removeSelected());
        selectedActions.add(removeButton);
        footer.add(selectedActions, BorderLayout.NORTH);
        noticeLabel.setForeground(MUTED);
        noticeLabel.setFont(DETAIL_FONT);
        noticeLabel.setOpaque(false);
        noticeLabel.setEditable(false);
        noticeLabel.setFocusable(false);
        noticeLabel.setLineWrap(true);
        noticeLabel.setWrapStyleWord(true);
        noticeLabel.setRows(2);
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
        tree.setRootVisible(true);
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
        button.setMargin(new Insets(4, 3, 4, 3));
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
        String name = JOptionPane.showInputDialog(this, "Folder name", "New folder", JOptionPane.PLAIN_MESSAGE);
        if (name != null)
        {
            actions.createFolder(name, parentId);
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
        removeButton.setEnabled(entry != null);
        boolean folder = entry != null && entry.isFolder();
        loadButton.setText(folder ? "Icon" : "Load");
        StoredBankTag tab = entry == null || folder ? null : tabsByName.get(entry.getName());
        toggleButton.setText(folder ? "Rename" : tab != null && tab.isEnabled() ? "Hide" : "Show");
        toggleButton.setForeground(folder ? Color.WHITE : tab != null && tab.isEnabled() ? GOLD : GREEN);
    }

    private void updateCount()
    {
        long visible = tabsByName.values().stream().filter(StoredBankTag::isEnabled).count();
        countLabel.setText(tabsByName.size() + " saved tabs  |  " + visible + " visible in bank");
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
            int width = Math.max(90, tree.getWidth() - 20 - node.getLevel() * 15);
            setPreferredSize(new Dimension(width, entry == null ? 30 : 60));
            setToolTipText(entry == null ? "Drop here to move a tab or folder to the top level"
                : entry.getName() + (entry.isFolder() ? " | Drop tabs here. Right-click for folder actions." : ""));
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
                painter.setFont(DETAIL_FONT);
                painter.setColor(selected ? GOLD : MUTED);
                painter.drawString("All saved tabs", 3, 19);
                painter.dispose();
                return;
            }
            painter.setColor(selected ? SELECTED : SURFACE);
            painter.fillRect(0, 0, getWidth(), getHeight() - 4);
            StoredBankTag tab = entry.isFolder() ? null : tabsByName.get(entry.getName());
            if (entry.isFolder() || tab != null && tab.isEnabled())
            {
                painter.setColor(entry.isFolder() ? GOLD : GREEN);
                painter.fillRect(0, 0, 2, getHeight() - 4);
            }
            BufferedImage image = itemImage(entry.isFolder() ? entry.getIconItemId() : tab.getIconItemId());
            if (image != null)
            {
                painter.drawImage(image, 7, 12, null);
            }
            else if (entry.isFolder())
            {
                painter.setColor(GOLD);
                painter.fillRoundRect(10, 19, 26, 20, 3, 3);
                painter.fillRoundRect(10, 15, 12, 7, 3, 3);
                painter.setColor(new Color(170, 140, 67));
                painter.drawLine(11, 23, 34, 23);
            }
            painter.setFont(NAME_FONT);
            painter.setColor(Color.WHITE);
            painter.drawString(clipText(entry.getName(), painter.getFontMetrics(), getWidth() - 56), 49, 24);
            painter.setFont(DETAIL_FONT);
            painter.setColor(MUTED);
            String details = entry.isFolder() ? folderDetails(entry.getId())
                : tab.getItemCount() + (tab.getItemCount() == 1 ? " item" : " items")
                    + "  |  " + (tab.isEnabled() ? "Showing" : "Hidden");
            painter.drawString(clipText(details, painter.getFontMetrics(), getWidth() - 56), 49, 43);
            JTree.DropLocation drop = tree.getDropLocation();
            if (drop != null && drop.getChildIndex() == -1 && drop.getPath() != null)
            {
                Object target = ((DefaultMutableTreeNode) drop.getPath().getLastPathComponent()).getUserObject();
                if (target instanceof StoredStorageEntry && entry.getId().equals(((StoredStorageEntry) target).getId()))
                {
                    painter.setColor(GOLD);
                    painter.drawRect(0, 0, getWidth() - 1, getHeight() - 5);
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
            if (location.getPath() == null)
            {
                return false;
            }
            DefaultMutableTreeNode target = (DefaultMutableTreeNode) location.getPath().getLastPathComponent();
            if (!target.getAllowsChildren())
            {
                return false;
            }
            try
            {
                EntryTransfer transfer = (EntryTransfer) support.getTransferable().getTransferData(EntryTransfer.FLAVOR);
                StoredStorageEntry entry = target.getUserObject() instanceof StoredStorageEntry
                    ? (StoredStorageEntry) target.getUserObject() : null;
                return transfer.source == tree && storageLayout.canMove(transfer.id, entry == null ? null : entry.getId());
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
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) location.getPath().getLastPathComponent();
                StoredStorageEntry parent = node.getUserObject() instanceof StoredStorageEntry
                    ? (StoredStorageEntry) node.getUserObject() : null;
                actions.moveEntry(transfer.id, parent == null ? null : parent.getId(), location.getChildIndex());
                return true;
            }
            catch (Exception exception)
            {
                showError("Unable to move the selected tab or folder");
                return false;
            }
        }
    }
}
