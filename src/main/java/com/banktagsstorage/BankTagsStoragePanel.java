package com.banktagsstorage;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

final class BankTagsStoragePanel extends PluginPanel
{
    private static final Color BACKGROUND = new Color(40, 40, 40);
    private static final Color SURFACE = new Color(53, 53, 53);
    private static final Color SELECTED = new Color(72, 72, 72);
    private static final Color BUTTON = new Color(68, 68, 68);
    private static final Color GOLD = new Color(214, 180, 92);
    private static final Color GREEN = new Color(122, 184, 111);
    private static final Color MUTED = new Color(174, 174, 174);
    private static final Color DANGER = new Color(225, 121, 111);
    private static final String LIST_CARD = "list";
    private static final String EMPTY_CARD = "empty";
    private final StorageActions actions;
    private final IntFunction<BufferedImage> imageProvider;
    private final DefaultListModel<StoredBankTag> tabModel = new DefaultListModel<>();
    private final JList<StoredBankTag> tabList = new JList<>(tabModel);
    private final Set<Integer> watchedImages = new HashSet<>();
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel content = new JPanel(contentLayout);
    private final JLabel countLabel = new JLabel();
    private final JLabel noticeLabel = new JLabel("Save the tabs you want to keep ready");
    private final JButton loadButton;
    private final JButton toggleButton;
    private final JButton removeButton;

    BankTagsStoragePanel(StorageActions actions, IntFunction<BufferedImage> imageProvider)
    {
        super(false);
        this.actions = actions;
        this.imageProvider = imageProvider;
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(BACKGROUND);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel title = new JLabel("Bank Tab Storage");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);

        JLabel subtitle = new JLabel("Your bank tab vault");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(11f));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(subtitle);
        header.add(Box.createVerticalStrut(9));

        JPanel status = new JPanel(new BorderLayout());
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        status.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        status.setBackground(SURFACE);
        status.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, GOLD),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        countLabel.setForeground(Color.WHITE);
        countLabel.setFont(countLabel.getFont().deriveFont(Font.BOLD, 11f));
        status.add(countLabel, BorderLayout.CENTER);
        header.add(status);
        header.add(Box.createVerticalStrut(8));

        JButton saveButton = button("Save current tabs", GOLD);
        saveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        saveButton.addActionListener(event -> actions.saveCurrentTabs());
        header.add(saveButton);
        header.add(Box.createVerticalStrut(5));

        JPanel transferActions = new JPanel(new GridLayout(1, 2, 5, 0));
        transferActions.setOpaque(false);
        transferActions.setAlignmentX(Component.LEFT_ALIGNMENT);
        transferActions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JButton importButton = button("Import", Color.WHITE);
        importButton.addActionListener(event -> importFromClipboard());
        transferActions.add(importButton);
        JButton exportButton = button("Export all", Color.WHITE);
        exportButton.addActionListener(event -> exportToClipboard());
        transferActions.add(exportButton);
        header.add(transferActions);
        add(header, BorderLayout.NORTH);

        configureList();
        JScrollPane scrollPane = new JScrollPane(tabList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(BACKGROUND);
        content.setOpaque(false);
        content.add(scrollPane, LIST_CARD);
        content.add(emptyState(), EMPTY_CARD);
        add(content, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 7));
        footer.setOpaque(false);
        JPanel selectedActions = new JPanel(new GridLayout(1, 3, 5, 0));
        selectedActions.setOpaque(false);
        loadButton = button("Load", Color.WHITE);
        loadButton.addActionListener(event -> loadSelected());
        selectedActions.add(loadButton);
        toggleButton = button("Show", GREEN);
        toggleButton.addActionListener(event -> toggleSelected());
        selectedActions.add(toggleButton);
        removeButton = button("Remove", DANGER);
        removeButton.addActionListener(event -> removeSelected());
        selectedActions.add(removeButton);
        footer.add(selectedActions, BorderLayout.NORTH);
        noticeLabel.setForeground(MUTED);
        noticeLabel.setFont(noticeLabel.getFont().deriveFont(10f));
        footer.add(noticeLabel, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        updateSelection();
        updateCount();
    }

    void setTabs(List<StoredBankTag> tabs, String notice)
    {
        String selectedName = selectedName();
        tabModel.clear();
        for (StoredBankTag tab : tabs)
        {
            tabModel.addElement(tab.copy());
            watchImage(tab.getIconItemId());
        }
        if (selectedName != null)
        {
            selectByName(selectedName);
        }
        if (tabList.getSelectedIndex() < 0 && !tabModel.isEmpty())
        {
            tabList.setSelectedIndex(0);
        }
        if (notice != null && !notice.isEmpty())
        {
            noticeLabel.setText(notice);
        }
        updateCount();
        updateSelection();
        contentLayout.show(content, tabModel.isEmpty() ? EMPTY_CARD : LIST_CARD);
    }

    void showError(String message)
    {
        JOptionPane.showMessageDialog(this, message, "Bank Tab Storage", JOptionPane.ERROR_MESSAGE);
    }

    private void configureList()
    {
        tabList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabList.setCellRenderer(new TabRenderer());
        tabList.setBackground(BACKGROUND);
        tabList.setForeground(Color.WHITE);
        tabList.setFixedCellHeight(70);
        tabList.addListSelectionListener(event ->
        {
            if (!event.getValueIsAdjusting())
            {
                updateSelection();
            }
        });
        tabList.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event)
            {
                if (event.getClickCount() == 2)
                {
                    loadSelected();
                }
            }
        });
    }

    private JPanel emptyState()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND);
        panel.add(Box.createVerticalGlue());
        JLabel title = new JLabel("The vault is empty");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(5));
        JLabel detail = new JLabel("Save your visible Bank Tags tabs");
        detail.setForeground(MUTED);
        detail.setFont(detail.getFont().deriveFont(11f));
        detail.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(detail);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JButton button(String text, Color foreground)
    {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(BUTTON);
        button.setForeground(foreground);
        button.setMargin(new Insets(2, 4, 2, 4));
        return button;
    }

    private void loadSelected()
    {
        StoredBankTag tab = tabList.getSelectedValue();
        if (tab == null)
        {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
            "Load '" + tab.getName() + "'? This replaces its current tagged items, icon, and layout.",
            "Load saved tab", JOptionPane.OK_CANCEL_OPTION);
        if (choice == JOptionPane.OK_OPTION)
        {
            actions.loadTab(tab.getName());
        }
    }

    private void toggleSelected()
    {
        StoredBankTag tab = tabList.getSelectedValue();
        if (tab != null)
        {
            actions.setTabEnabled(tab.getName(), !tab.isEnabled());
        }
    }

    private void removeSelected()
    {
        StoredBankTag tab = tabList.getSelectedValue();
        if (tab == null)
        {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
            "Remove '" + tab.getName() + "' from storage? The live Bank Tags data will be kept.",
            "Remove saved tab", JOptionPane.OK_CANCEL_OPTION);
        if (choice == JOptionPane.OK_OPTION)
        {
            actions.removeTab(tab.getName());
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
                "How should the clipboard tabs be imported?", "Import saved tabs",
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
        if (tabModel.isEmpty())
        {
            showError("There are no saved tabs to export");
            return;
        }
        try
        {
            StringSelection selection = new StringSelection(actions.exportText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            noticeLabel.setText("Copied all saved tabs to the clipboard");
        }
        catch (Exception exception)
        {
            showError(exception.getMessage() == null ? "Unable to write to the clipboard" : exception.getMessage());
        }
    }

    private void updateSelection()
    {
        StoredBankTag selected = tabList.getSelectedValue();
        boolean available = selected != null;
        loadButton.setEnabled(available);
        toggleButton.setEnabled(available);
        removeButton.setEnabled(available);
        if (selected == null)
        {
            toggleButton.setText("Show");
            toggleButton.setForeground(GREEN);
        }
        else
        {
            toggleButton.setText(selected.isEnabled() ? "Hide" : "Show");
            toggleButton.setForeground(selected.isEnabled() ? GOLD : GREEN);
        }
    }

    private void updateCount()
    {
        int visible = 0;
        for (int index = 0; index < tabModel.size(); index++)
        {
            if (tabModel.get(index).isEnabled())
            {
                visible++;
            }
        }
        countLabel.setText(tabModel.size() + " stored  |  " + visible + " visible in bank");
    }

    private String selectedName()
    {
        StoredBankTag selected = tabList.getSelectedValue();
        return selected == null ? null : selected.getName();
    }

    private void selectByName(String name)
    {
        for (int index = 0; index < tabModel.size(); index++)
        {
            if (tabModel.get(index).getName().equals(name))
            {
                tabList.setSelectedIndex(index);
                return;
            }
        }
    }

    private void watchImage(int itemId)
    {
        if (!watchedImages.add(itemId))
        {
            return;
        }
        BufferedImage image = imageProvider.apply(itemId);
        if (image instanceof AsyncBufferedImage)
        {
            ((AsyncBufferedImage) image).onLoaded(() -> SwingUtilities.invokeLater(tabList::repaint));
        }
    }

    private final class TabRenderer extends JPanel implements ListCellRenderer<StoredBankTag>
    {
        private StoredBankTag tab;
        private boolean selected;

        private TabRenderer()
        {
            setOpaque(true);
            setPreferredSize(new Dimension(200, 70));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends StoredBankTag> list, StoredBankTag value,
            int index, boolean selected, boolean focused)
        {
            this.tab = value;
            this.selected = selected;
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics)
        {
            Graphics2D painter = (Graphics2D) graphics.create();
            painter.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            painter.setColor(BACKGROUND);
            painter.fillRect(0, 0, getWidth(), getHeight());
            if (tab == null)
            {
                painter.dispose();
                return;
            }
            painter.setColor(selected ? SELECTED : SURFACE);
            painter.fillRect(0, 0, getWidth(), getHeight() - 6);
            int left = 10;
            if (tab.isEnabled())
            {
                painter.setColor(GOLD);
                painter.fillRect(0, 0, 3, getHeight() - 6);
                left = 11;
            }
            BufferedImage image = imageProvider.apply(tab.getIconItemId());
            if (image != null)
            {
                painter.drawImage(image, left, 16, null);
            }
            int textLeft = 56;
            String stateText = tab.isEnabled() ? "SHOWING" : "HIDDEN";
            painter.setFont(getFont().deriveFont(Font.BOLD, 9f));
            FontMetrics stateMetrics = painter.getFontMetrics();
            int stateWidth = stateMetrics.stringWidth(stateText);
            painter.setColor(tab.isEnabled() ? GREEN : MUTED);
            painter.drawString(stateText, getWidth() - stateWidth - 8, 24);

            painter.setFont(getFont().deriveFont(Font.BOLD, 12f));
            painter.setColor(Color.WHITE);
            painter.drawString(clipText(tab.getName(), painter.getFontMetrics(),
                getWidth() - textLeft - stateWidth - 18), textLeft, 24);

            int count = tab.getItemCount();
            String details = count + (count == 1 ? " item" : " items")
                + "  |  " + (tab.hasLayout() ? "Layout saved" : "No layout");
            painter.setFont(getFont().deriveFont(10f));
            painter.setColor(MUTED);
            painter.drawString(clipText(details, painter.getFontMetrics(), getWidth() - textLeft - 8),
                textLeft, 44);
            painter.dispose();
        }

        private String clipText(String value, FontMetrics metrics, int width)
        {
            if (width <= 0 || metrics.stringWidth(value) <= width)
            {
                return width <= 0 ? "" : value;
            }
            String suffix = "...";
            int limit = Math.max(0, width - metrics.stringWidth(suffix));
            int end = value.length();
            while (end > 0 && metrics.stringWidth(value.substring(0, end)) > limit)
            {
                end--;
            }
            return value.substring(0, end) + suffix;
        }
    }
}
