package com.banktabstorage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.plugins.banktags.tabs.Layout;
import net.runelite.client.plugins.banktags.tabs.LayoutManager;
import net.runelite.client.util.Text;

final class BankTagsAccess
{
    private final ConfigManager configManager;
    private final TagManager tagManager;
    private final LayoutManager layoutManager;
    private final BankTabReloader tabReloader;
    private final BankTagsService bankTagsService;

    BankTagsAccess(ConfigManager configManager, TagManager tagManager, LayoutManager layoutManager,
        BankTabReloader tabReloader, BankTagsService bankTagsService)
    {
        this.configManager = configManager;
        this.tagManager = tagManager;
        this.layoutManager = layoutManager;
        this.tabReloader = tabReloader;
        this.bankTagsService = bankTagsService;
    }

    List<StoredBankTag> captureVisibleTabs()
    {
        List<StoredBankTag> tabs = new ArrayList<>();
        for (String name : visibleNames())
        {
            tabs.add(capture(name));
        }
        return tabs;
    }

    List<String> visibleNames()
    {
        String value = configManager.getConfiguration(BankTagsPlugin.CONFIG_GROUP, BankTagsPlugin.TAG_TABS_CONFIG);
        Set<String> names = new LinkedHashSet<>();
        if (value != null)
        {
            for (String name : Text.fromCSV(value))
            {
                names.add(StoredBankTag.normalizeName(name));
            }
        }
        return new ArrayList<>(names);
    }

    void applySnapshot(StoredBankTag source)
    {
        StoredBankTag tab = source.validatedCopy();
        String name = tab.getName();
        tagManager.removeTag(name);
        for (int itemId : tab.getItemIds())
        {
            tagManager.addTag(Math.abs(itemId), name, itemId < 0);
        }
        configManager.setConfiguration(BankTagsPlugin.CONFIG_GROUP,
            BankTagsPlugin.TAG_ICON_PREFIX + name, tab.getIconItemId());
        List<Integer> layout = tab.getLayout();
        if (layout == null)
        {
            layoutManager.removeLayout(name);
        }
        else
        {
            int[] values = new int[layout.size()];
            for (int index = 0; index < layout.size(); index++)
            {
                values[index] = layout.get(index);
            }
            layoutManager.saveLayout(new Layout(name, values));
        }
        tagManager.setHidden(name, tab.isHidden());
    }

    void setVisible(String value, boolean visible)
    {
        String name = StoredBankTag.normalizeName(value);
        List<String> names = visibleNames();
        if (visible)
        {
            if (!names.contains(name))
            {
                names.add(name);
            }
        }
        else
        {
            names.removeIf(name::equals);
        }
        configManager.setConfiguration(BankTagsPlugin.CONFIG_GROUP,
            BankTagsPlugin.TAG_TABS_CONFIG, Text.toCSV(names));
    }

    void refreshBank(String changedTab, boolean close)
    {
        String activeTag = bankTagsService.getActiveTag();
        tabReloader.reload();
        if (activeTag == null
            || !StoredBankTag.normalizeName(activeTag).equals(StoredBankTag.normalizeName(changedTab)))
        {
            return;
        }
        if (close)
        {
            bankTagsService.closeBankTag();
            return;
        }
        bankTagsService.openBankTag(activeTag, BankTagsService.OPTION_ALLOW_MODIFICATIONS);
    }

    private StoredBankTag capture(String value)
    {
        String name = StoredBankTag.normalizeName(value);
        int iconItemId = readIconItemId(name);
        List<Integer> items = tagManager.getItemsForTag(name);
        Layout layout = layoutManager.loadLayout(name);
        List<Integer> layoutValues = null;
        if (layout != null)
        {
            layoutValues = new ArrayList<>();
            for (int itemId : layout.getLayout())
            {
                layoutValues.add(itemId);
            }
        }
        return new StoredBankTag(name, iconItemId, items, layoutValues,
            tagManager.isHidden(name), true).validatedCopy();
    }

    private int readIconItemId(String name)
    {
        String value = configManager.getConfiguration(BankTagsPlugin.CONFIG_GROUP,
            BankTagsPlugin.TAG_ICON_PREFIX + name);
        if (value == null)
        {
            return ItemID.SPADE;
        }
        try
        {
            int itemId = Integer.parseInt(value);
            return itemId < 0 ? ItemID.SPADE : itemId;
        }
        catch (NumberFormatException exception)
        {
            return ItemID.SPADE;
        }
    }

}
