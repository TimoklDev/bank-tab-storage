package com.banktabstorage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class BankTabStorageData
{
    static final String KIND = "bank-tab-storage";
    private static final String LEGACY_KIND = "bank-tags-storage";
    static final int VERSION = 2;
    private static final int MAX_TABS = 500;
    private String kind;
    private int version;
    private List<StoredBankTag> tabs;
    private List<StoredStorageEntry> entries;

    private BankTabStorageData()
    {
    }

    BankTabStorageData(List<StoredBankTag> tabs)
    {
        this(tabs, StorageLayout.empty());
    }

    BankTabStorageData(List<StoredBankTag> tabs, StorageLayout layout)
    {
        this.kind = KIND;
        this.version = VERSION;
        this.tabs = copyTabs(tabs);
        this.entries = layout.reconcile(this.tabs).getEntries();
    }

    List<StoredBankTag> validatedTabs()
    {
        if ((!KIND.equals(kind) && !LEGACY_KIND.equals(kind)) || (version != 1 && version != VERSION))
        {
            throw new IllegalArgumentException("The clipboard does not contain a supported Bank Tab Storage export");
        }
        return copyTabs(tabs);
    }

    StorageLayout validatedLayout()
    {
        List<StoredBankTag> validTabs = validatedTabs();
        StorageLayout layout = version == 1 || entries == null ? StorageLayout.empty() : new StorageLayout(entries);
        return layout.reconcile(validTabs);
    }

    private static List<StoredBankTag> copyTabs(List<StoredBankTag> source)
    {
        if (source == null || source.size() > MAX_TABS)
        {
            throw new IllegalArgumentException("The saved tab collection is invalid");
        }
        List<StoredBankTag> result = new ArrayList<>(source.size());
        Set<String> names = new HashSet<>();
        for (StoredBankTag sourceTab : source)
        {
            if (sourceTab == null)
            {
                throw new IllegalArgumentException("The saved tab collection contains an invalid entry");
            }
            StoredBankTag tab = sourceTab.validatedCopy();
            if (!names.add(tab.getName()))
            {
                throw new IllegalArgumentException("The saved tab collection contains duplicate names");
            }
            result.add(tab);
        }
        return result;
    }
}
