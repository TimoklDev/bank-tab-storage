package com.banktagsstorage;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class StoredTabCollectionTest
{
    @Test
    public void mergeUpdatesExistingPositionAndKeepsHiddenTabs()
    {
        StoredBankTag first = tab("first", 1, false);
        StoredBankTag hidden = tab("hidden", 2, false);
        StoredBankTag updated = tab("first", 3, true);
        List<StoredBankTag> merged = StoredTabCollection.merge(List.of(first, hidden), List.of(updated), false);
        Assert.assertEquals(2, merged.size());
        Assert.assertEquals("first", merged.get(0).getName());
        Assert.assertEquals(3, merged.get(0).getIconItemId());
        Assert.assertEquals("hidden", merged.get(1).getName());
    }

    @Test
    public void syncEnabledFollowsVisibleBankTabs()
    {
        List<StoredBankTag> synced = StoredTabCollection.syncEnabled(
            List.of(tab("first", 1, false), tab("second", 2, true)), List.of("FIRST"));
        Assert.assertTrue(synced.get(0).isEnabled());
        Assert.assertFalse(synced.get(1).isEnabled());
    }

    private StoredBankTag tab(String name, int icon, boolean enabled)
    {
        return new StoredBankTag(name, icon, List.of(icon), null, false, enabled).validatedCopy();
    }
}

