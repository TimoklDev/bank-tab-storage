package com.banktabstorage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class BankTabStorageData
{
    static final String KIND = "bank-tab-storage";
    private static final String LEGACY_KIND = "bank-tags-storage";
    static final int VERSION = 1;
    private static final int MAX_TABS = 500;
    private String kind;
    private int version;
    private List<StoredBankTag> tabs;

    private BankTabStorageData()
    {
    }

    BankTabStorageData(List<StoredBankTag> tabs)
    {
        this.kind = KIND;
        this.version = VERSION;
        this.tabs = copyTabs(tabs);
    }

    List<StoredBankTag> validatedTabs()
    {
        if ((!KIND.equals(kind) && !LEGACY_KIND.equals(kind)) || version != VERSION)
        {
            throw new IllegalArgumentException("The clipboard does not contain a supported Bank Tab Storage export");
        }
        return copyTabs(tabs);
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
