package com.banktabstorage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.util.AsyncBufferedImage;

final class ItemReplacementPanel extends JPanel
{
    private enum Step
    {
        SOURCE, TABS, TARGET, REVIEW
    }

    private static final Font FONT = new Font(Font.DIALOG, Font.PLAIN, 12);
    private static final Color BACKGROUND = new Color(40, 40, 40);
    private static final Color ACCENT = new Color(242, 138, 58);
    private final StorageActions actions;
    private final IntFunction<BufferedImage> images;
    private final Map<Integer, ImageIcon> icons = new HashMap<>();
    private final JLabel heading = new JLabel();
    private final JLabel results = new JLabel();
    private final JTextArea description = new JTextArea();
    private final JPanel body = new JPanel(new BorderLayout(0, 7));
    private final JTextField search = new JTextField();
    private final DefaultListModel<ReplacementItem> itemModel = new DefaultListModel<>();
    private final JList<ReplacementItem> itemList = new JList<>(itemModel);
    private final JButton next = button("Continue");
    private final JButton back = button("Back");
    private final List<JCheckBox> tabChoices = new ArrayList<>();
    private List<ReplacementItem> sourceItems = List.of();
    private List<ReplacementItem> bankItems = List.of();
    private List<ReplacementItem> shownItems = List.of();
    private List<String> matchingTabs = List.of();
    private List<String> selectedTabs = List.of();
    private ReplacementItem source;
    private ReplacementItem target;
    private long requestId;
    private Step step;

    ItemReplacementPanel(StorageActions actions, IntFunction<BufferedImage> images, Runnable close)
    {
        super(new BorderLayout(0, 9));
        this.actions = actions;
        this.images = images;
        setBackground(BACKGROUND);
        JPanel top = new JPanel(new BorderLayout(0, 5));
        top.setOpaque(false);
        heading.setFont(FONT.deriveFont(Font.BOLD, 13f));
        heading.setForeground(Color.WHITE);
        top.add(heading, BorderLayout.NORTH);
        description.setFont(FONT);
        description.setForeground(new Color(200, 200, 200));
        description.setOpaque(false);
        description.setEditable(false);
        description.setFocusable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setRows(3);
        top.add(description, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        body.setOpaque(false);
        add(body, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(0, 5));
        footer.setOpaque(false);
        next.setForeground(ACCENT);
        next.addActionListener(event -> advance());
        footer.add(next, BorderLayout.NORTH);
        JPanel navigation = new JPanel(new GridLayout(1, 2, 5, 0));
        navigation.setOpaque(false);
        back.addActionListener(event -> goBack());
        navigation.add(back);
        JButton cancel = button("Cancel");
        cancel.addActionListener(event ->
        {
            actions.cancelItemReplacement(requestId);
            close.run();
        });
        navigation.add(cancel);
        footer.add(navigation, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        search.setFont(FONT);
        results.setFont(FONT.deriveFont(11f));
        results.setForeground(new Color(200, 200, 200));
        search.putClientProperty("JTextField.placeholderText", "Search items...");
        search.getAccessibleContext().setAccessibleName("Search replacement items");
        search.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent event)
            {
                filter();
            }

            @Override
            public void removeUpdate(DocumentEvent event)
            {
                filter();
            }

            @Override
            public void changedUpdate(DocumentEvent event)
            {
                filter();
            }
        });
        itemList.setBackground(BACKGROUND);
        itemList.setForeground(Color.WHITE);
        itemList.setSelectionBackground(new Color(69, 56, 47));
        itemList.setSelectionForeground(Color.WHITE);
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.setFixedCellHeight(40);
        itemList.setCellRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean selected, boolean focused)
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                ReplacementItem item = (ReplacementItem) value;
                label.putClientProperty("html.disable", true);
                label.setFont(FONT);
                label.setText(item.getName());
                label.setToolTipText(item.getName());
                label.setIcon(icon(item.getId()));
                label.setIconTextGap(7);
                label.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
                return label;
            }
        });
        itemList.addListSelectionListener(event ->
        {
            if (step == Step.SOURCE || step == Step.TARGET)
            {
                next.setEnabled(itemList.getSelectedValue() != null);
            }
        });
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

    void showSources(long id, List<ReplacementItem> choices)
    {
        requestId = id;
        icons.clear();
        sourceItems = List.copyOf(choices);
        source = null;
        selectedTabs = List.of();
        showItems(true);
    }

    void showTabs(long id, ReplacementItem item, List<String> names)
    {
        if (requestId != id)
        {
            return;
        }
        source = item;
        matchingTabs = List.copyOf(names);
        step = Step.TABS;
        heading.setText("2. Choose tabs");
        description.setText("Replace " + source.getName() + " in these tabs. Hidden tabs are included.");
        body.removeAll();
        tabChoices.clear();
        JCheckBox all = new JCheckBox("All matching tabs", true);
        all.setFont(FONT.deriveFont(Font.BOLD));
        all.setOpaque(false);
        all.setForeground(Color.WHITE);
        body.add(all, BorderLayout.NORTH);
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(BACKGROUND);
        for (String name : matchingTabs)
        {
            JCheckBox choice = new JCheckBox(name, selectedTabs.isEmpty() || selectedTabs.contains(name));
            choice.putClientProperty("html.disable", true);
            choice.setFont(FONT);
            choice.setToolTipText(name);
            choice.setForeground(Color.WHITE);
            choice.setOpaque(false);
            choice.addActionListener(event ->
            {
                all.setSelected(tabChoices.stream().allMatch(JCheckBox::isSelected));
                updateTabSelection();
            });
            tabChoices.add(choice);
            list.add(choice);
        }
        all.setSelected(tabChoices.stream().allMatch(JCheckBox::isSelected));
        all.addActionListener(event ->
        {
            tabChoices.forEach(choice -> choice.setSelected(all.isSelected()));
            updateTabSelection();
        });
        body.add(scroll(list), BorderLayout.CENTER);
        back.setEnabled(true);
        updateTabSelection();
        refresh();
    }

    void showTargets(long id, List<ReplacementItem> choices, List<String> names)
    {
        if (requestId != id)
        {
            return;
        }
        selectedTabs = List.copyOf(names);
        bankItems = List.copyOf(choices);
        showItems(false);
    }

    void operationFailed()
    {
        next.setEnabled(step == Step.REVIEW || step == Step.TABS || itemList.getSelectedValue() != null);
    }

    private void showItems(boolean fromTabs)
    {
        step = fromTabs ? Step.SOURCE : Step.TARGET;
        heading.setText(fromTabs ? "1. Choose the old item" : "3. Choose the new item");
        description.setText(fromTabs ? "Search items in your saved and live tag tabs, including hidden tabs."
            : "Search items currently in your bank. Empty placeholders are excluded.");
        shownItems = fromTabs ? sourceItems : bankItems;
        body.removeAll();
        body.add(search, BorderLayout.NORTH);
        body.add(scroll(itemList), BorderLayout.CENTER);
        body.add(results, BorderLayout.SOUTH);
        back.setEnabled(!fromTabs);
        next.setText(fromTabs ? "Choose item" : "Review replacement");
        search.setText("");
        filter();
        refresh();
        SwingUtilities.invokeLater(search::requestFocusInWindow);
    }

    private void filter()
    {
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        itemModel.clear();
        for (ReplacementItem item : shownItems)
        {
            if (item.getName().toLowerCase(Locale.ROOT).contains(query))
            {
                itemModel.addElement(item);
            }
        }
        results.setText(itemModel.isEmpty() ? "No matching items" : itemModel.size() + " matching items");
        next.setEnabled(false);
    }

    private void updateTabSelection()
    {
        selectedTabs = tabChoices.stream().filter(JCheckBox::isSelected).map(JCheckBox::getText)
            .collect(Collectors.toList());
        next.setText("Continue (" + selectedTabs.size() + ")");
        next.setEnabled(!selectedTabs.isEmpty());
    }

    private void advance()
    {
        if (step == Step.SOURCE && itemList.getSelectedValue() != null)
        {
            selectedTabs = List.of();
            next.setEnabled(false);
            actions.selectReplacementSource(requestId, itemList.getSelectedValue().getId());
        }
        else if (step == Step.TABS && !selectedTabs.isEmpty())
        {
            next.setEnabled(false);
            actions.selectReplacementTabs(requestId, List.copyOf(selectedTabs));
        }
        else if (step == Step.TARGET && itemList.getSelectedValue() != null)
        {
            target = itemList.getSelectedValue();
            showReview();
        }
        else if (step == Step.REVIEW)
        {
            next.setEnabled(false);
            actions.replaceItems(requestId, target.getId());
        }
    }

    private void showReview()
    {
        step = Step.REVIEW;
        heading.setText("4. Review replacement");
        description.setText("Updates item tags and every matching layout position in the selected tabs.");
        body.removeAll();
        JPanel items = new JPanel(new GridLayout(2, 1, 0, 7));
        items.setOpaque(false);
        items.add(itemLabel("Replace: ", source));
        items.add(itemLabel("With: ", target));
        body.add(items, BorderLayout.NORTH);
        JList<String> tabs = new JList<>(selectedTabs.toArray(new String[0]));
        tabs.setFont(FONT);
        tabs.setForeground(Color.WHITE);
        tabs.setBackground(BACKGROUND);
        body.add(scroll(tabs), BorderLayout.CENTER);
        next.setText("Replace in " + selectedTabs.size() + (selectedTabs.size() == 1 ? " tab" : " tabs"));
        next.setEnabled(true);
        back.setEnabled(true);
        refresh();
    }

    private JPanel itemLabel(String prefix, ReplacementItem item)
    {
        JPanel row = new JPanel(new BorderLayout(7, 0));
        row.setOpaque(false);
        row.add(new JLabel(icon(item.getId())), BorderLayout.WEST);
        JTextArea text = new JTextArea(prefix + item.getName());
        text.setForeground(Color.WHITE);
        text.setFont(FONT);
        text.setOpaque(false);
        text.setEditable(false);
        text.setFocusable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setRows(3);
        row.add(text, BorderLayout.CENTER);
        return row;
    }

    private void goBack()
    {
        if (step == Step.TABS)
        {
            showItems(true);
        }
        else if (step == Step.TARGET)
        {
            showTabs(requestId, source, matchingTabs);
        }
        else if (step == Step.REVIEW)
        {
            showItems(false);
        }
    }

    private ImageIcon icon(int id)
    {
        if (!icons.containsKey(id))
        {
            BufferedImage image = images.apply(id);
            if (image != null)
            {
                icons.put(id, new ImageIcon(image));
                if (image instanceof AsyncBufferedImage)
                {
                    ((AsyncBufferedImage) image).onLoaded(() -> SwingUtilities.invokeLater(this::repaint));
                }
            }
        }
        return icons.get(id);
    }

    private JScrollPane scroll(Component component)
    {
        JScrollPane pane = new JScrollPane(component);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pane.getViewport().setBackground(BACKGROUND);
        return pane;
    }

    private JButton button(String text)
    {
        JButton button = new JButton(text);
        button.setFont(FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(57, 57, 57));
        button.setPreferredSize(new Dimension(150, 28));
        button.setFocusPainted(false);
        return button;
    }

    private void refresh()
    {
        body.revalidate();
        body.repaint();
    }
}
