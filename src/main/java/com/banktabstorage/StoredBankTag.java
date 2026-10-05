package com.banktabstorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.runelite.client.util.Text;

final class StoredBankTag
{
    private static final int MAX_NAME_LENGTH = 80;
    private static final int MAX_ITEMS = 10000;
    private static final int MAX_LAYOUT_SIZE = 10000;
    private String name;
    private int iconItemId;
    private List<Integer> itemIds;
    private List<Integer> layout;
    private boolean hidden;
    private boolean enabled;

    private StoredBankTag()
    {
    }

    StoredBankTag(String name, int iconItemId, List<Integer> itemIds, List<Integer> layout,
        boolean hidden, boolean enabled)
    {
        this.name = name;
        this.iconItemId = iconItemId;
        this.itemIds = itemIds;
        this.layout = layout;
        this.hidden = hidden;
        this.enabled = enabled;
    }

    StoredBankTag validatedCopy()
    {
        String normalizedName = normalizeName(name);
        if (iconItemId < 0)
        {
            throw new IllegalArgumentException("A saved tab has an invalid icon item ID");
        }
        List<Integer> normalizedItems = normalizeItems(itemIds);
        List<Integer> normalizedLayout = normalizeLayout(layout);
        return new StoredBankTag(normalizedName, iconItemId, normalizedItems, normalizedLayout, hidden, enabled);
    }

    StoredBankTag withEnabled(boolean newEnabled)
    {
        return new StoredBankTag(name, iconItemId, itemIds, layout, hidden, newEnabled).validatedCopy();
    }

    StoredBankTag copy()
    {
        return new StoredBankTag(name, iconItemId, itemIds, layout, hidden, enabled).validatedCopy();
    }

    String getName()
    {
        return name;
    }

    int getIconItemId()
    {
        return iconItemId;
    }

    List<Integer> getItemIds()
    {
        return new ArrayList<>(itemIds);
    }

    List<Integer> getLayout()
    {
        return layout == null ? null : new ArrayList<>(layout);
    }

    boolean hasLayout()
    {
        return layout != null;
    }

    boolean isHidden()
    {
        return hidden;
    }

    boolean isEnabled()
    {
        return enabled;
    }

    int getItemCount()
    {
        return itemIds.size();
    }

    static String normalizeName(String value)
    {
        if (value == null)
        {
            throw new IllegalArgumentException("A saved tab is missing its name");
        }
        StringBuilder filtered = new StringBuilder();
        value.codePoints().filter(StoredBankTag::isAllowedNameCharacter).forEach(filtered::appendCodePoint);
        String normalized = Text.standardize(filtered.toString());
        if (normalized.isEmpty() || normalized.length() > MAX_NAME_LENGTH)
        {
            throw new IllegalArgumentException("A saved tab has an invalid name");
        }
        return normalized;
    }

    private static boolean isAllowedNameCharacter(int value)
    {
        return "</>:".indexOf(value) == -1;
    }

    private static List<Integer> normalizeItems(List<Integer> values)
    {
        if (values == null || values.size() > MAX_ITEMS)
        {
            throw new IllegalArgumentException("A saved tab has an invalid item list");
        }
        Set<Integer> unique = new LinkedHashSet<>();
        for (Integer value : values)
        {
            if (value == null || value == Integer.MIN_VALUE)
            {
                throw new IllegalArgumentException("A saved tab has an invalid item ID");
            }
            unique.add(value);
        }
        List<Integer> normalized = new ArrayList<>(unique);
        Collections.sort(normalized);
        return normalized;
    }

    private static List<Integer> normalizeLayout(List<Integer> values)
    {
        if (values == null)
        {
            return null;
        }
        if (values.size() > MAX_LAYOUT_SIZE)
        {
            throw new IllegalArgumentException("A saved tab has an invalid layout");
        }
        List<Integer> normalized = new ArrayList<>(values.size());
        for (Integer value : values)
        {
            if (value == null || value < -1)
            {
                throw new IllegalArgumentException("A saved tab has an invalid layout item ID");
            }
            normalized.add(value);
        }
        return normalized;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof StoredBankTag))
        {
            return false;
        }
        StoredBankTag tab = (StoredBankTag) other;
        return iconItemId == tab.iconItemId
            && hidden == tab.hidden
            && enabled == tab.enabled
            && Objects.equals(name, tab.name)
            && Objects.equals(itemIds, tab.itemIds)
            && Objects.equals(layout, tab.layout);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, iconItemId, itemIds, layout, hidden, enabled);
    }
}
