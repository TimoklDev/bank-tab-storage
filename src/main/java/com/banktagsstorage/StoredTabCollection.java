package com.banktagsstorage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class StoredTabCollection
{
    private StoredTabCollection()
    {
    }

    static List<StoredBankTag> merge(List<StoredBankTag> existing, List<StoredBankTag> incoming, boolean replace)
    {
        Map<String, StoredBankTag> merged = new LinkedHashMap<>();
        if (!replace)
        {
            for (StoredBankTag tab : existing)
            {
                StoredBankTag copy = tab.validatedCopy();
                merged.put(copy.getName(), copy);
            }
        }
        for (StoredBankTag tab : incoming)
        {
            StoredBankTag copy = tab.validatedCopy();
            merged.put(copy.getName(), copy);
        }
        return new ArrayList<>(merged.values());
    }

    static List<StoredBankTag> syncEnabled(List<StoredBankTag> tabs, List<String> visibleNames)
    {
        Set<String> visible = new LinkedHashSet<>();
        for (String name : visibleNames)
        {
            visible.add(StoredBankTag.normalizeName(name));
        }
        List<StoredBankTag> result = new ArrayList<>(tabs.size());
        for (StoredBankTag tab : tabs)
        {
            result.add(tab.withEnabled(visible.contains(tab.getName())));
        }
        return result;
    }

    static List<StoredBankTag> setEnabled(List<StoredBankTag> tabs, String name, boolean enabled)
    {
        String normalized = StoredBankTag.normalizeName(name);
        List<StoredBankTag> result = new ArrayList<>(tabs.size());
        for (StoredBankTag tab : tabs)
        {
            result.add(tab.getName().equals(normalized) ? tab.withEnabled(enabled) : tab.copy());
        }
        return result;
    }

    static List<StoredBankTag> remove(List<StoredBankTag> tabs, String name)
    {
        String normalized = StoredBankTag.normalizeName(name);
        List<StoredBankTag> result = new ArrayList<>();
        for (StoredBankTag tab : tabs)
        {
            if (!tab.getName().equals(normalized))
            {
                result.add(tab.copy());
            }
        }
        return result;
    }

    static StoredBankTag find(List<StoredBankTag> tabs, String name)
    {
        String normalized = StoredBankTag.normalizeName(name);
        for (StoredBankTag tab : tabs)
        {
            if (tab.getName().equals(normalized))
            {
                return tab.copy();
            }
        }
        return null;
    }
}

