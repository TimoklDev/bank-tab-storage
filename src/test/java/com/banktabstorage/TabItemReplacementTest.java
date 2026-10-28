package com.banktabstorage;

import com.google.gson.Gson;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class TabItemReplacementTest
{
    private final TabItemReplacement replacement = new TabItemReplacement(
        id -> id == 1100 ? 1000 : id,
        id -> id >= 1000 && id <= 1002 ? 1000 : id >= 2000 && id <= 2002 ? 2000 : id);

    @Test
    public void countsItemsStoredOnlyInLayouts()
    {
        StoredBankTag tab = tab("doom", List.of(), List.of(1000, -1, 1000, 2000, -1), false);

        Assert.assertEquals(2, tab.getItemCount());
        Assert.assertEquals(Set.of(1000, 2000), tab.getAllItemIds());
    }

    @Test
    public void combinesTagsAndLayoutItemsWithoutCountingEmptyOrRepeatedSlots()
    {
        StoredBankTag tab = tab("doom", List.of(-1000, 1000, 3000), List.of(1000, -1, 2000, 2000), false);

        Assert.assertEquals(3, tab.getItemCount());
    }

    @Test
    public void replacesEveryMatchingLayoutSlotAndKeepsOtherState()
    {
        StoredBankTag before = tab("doom", List.of(1000, 3000), List.of(-1, 1000, 3000, 1000), true);

        StoredBankTag after = replacement.replace(before, 1000, 2000);

        Assert.assertEquals(List.of(2000, 3000), after.getItemIds());
        Assert.assertEquals(List.of(-1, 2000, 3000, 2000), after.getLayout());
        Assert.assertEquals(before.getIconItemId(), after.getIconItemId());
        Assert.assertEquals(before.isHidden(), after.isHidden());
        Assert.assertEquals(before.isEnabled(), after.isEnabled());
        Assert.assertEquals(List.of(-1, 1000, 3000, 1000), before.getLayout());
    }

    @Test
    public void replacesLayoutOnlyItemsWithoutAddingUnrelatedTags()
    {
        StoredBankTag before = tab("doom", List.of(), List.of(1000, -1, 1000), false);

        Assert.assertTrue(replacement.contains(before, 1000));
        StoredBankTag after = replacement.replace(before, 1000, 2000);

        Assert.assertTrue(after.getItemIds().isEmpty());
        Assert.assertEquals(List.of(2000, -1, 2000), after.getLayout());
    }

    @Test
    public void preservesVariationTagsAndReplacesTheirLayoutVariants()
    {
        StoredBankTag before = tab("slayer", List.of(-1000, 3000), List.of(1001, -1, 1002, 3000), false);

        Assert.assertTrue(replacement.contains(before, 1002));
        StoredBankTag after = replacement.replace(before, 1001, 2001);

        Assert.assertEquals(List.of(-2000, 3000), after.getItemIds());
        Assert.assertEquals(List.of(2001, -1, 2001, 3000), after.getLayout());
    }

    @Test
    public void exactTagsDoNotReplaceOtherChargeVariants()
    {
        StoredBankTag before = tab("slayer", List.of(1001, 1002), List.of(1001, 1002), false);

        StoredBankTag after = replacement.replace(before, 1001, 2001);

        Assert.assertEquals(List.of(1002, 2001), after.getItemIds());
        Assert.assertEquals(List.of(2001, 1002), after.getLayout());
    }

    @Test
    public void canonicalizesNotedSourceIds()
    {
        StoredBankTag before = tab("slayer", List.of(1000), null, false);

        Assert.assertTrue(replacement.contains(before, 1100));
        Assert.assertEquals(List.of(2000), replacement.replace(before, 1100, 2000).getItemIds());
    }

    @Test
    public void deduplicatesItemTagsButPreservesIntentionalLayoutDuplicates()
    {
        StoredBankTag before = tab("slayer", List.of(1000, 2000), List.of(1000, 2000), false);

        StoredBankTag after = replacement.replace(before, 1000, 2000);

        Assert.assertEquals(List.of(2000), after.getItemIds());
        Assert.assertEquals(List.of(2000, 2000), after.getLayout());
    }

    @Test
    public void onlyChangesSelectedTabsIncludingHiddenOnes()
    {
        StoredBankTag hidden = tab("hidden", List.of(1000), List.of(1000), false);
        StoredBankTag unchecked = tab("unchecked", List.of(1000), List.of(1000), true);

        List<StoredBankTag> result = replacement.replaceSelected(List.of(hidden, unchecked),
            Set.of("hidden"), 1000, 2000);

        Assert.assertEquals(List.of(2000), result.get(0).getItemIds());
        Assert.assertFalse(result.get(0).isEnabled());
        Assert.assertEquals(unchecked, result.get(1));
    }

    @Test
    public void replacementAndFolderOrganizationSurviveExport()
    {
        List<StoredBankTag> tabs = List.of(tab("hidden", List.of(), List.of(1000), false));
        StoredStorageEntry folder = StoredStorageEntry.folder("Bossing", null);
        StorageLayout layout = StorageLayout.empty().reconcile(tabs).addFolder(folder)
            .move("tab:hidden", folder.getId(), -1);
        List<StoredBankTag> result = replacement.replaceSelected(tabs, Set.of("hidden"), 1000, 2000);
        BankTabStorageCodec codec = new BankTabStorageCodec(new Gson());

        BankTabStorageData decoded = codec.decodeData(codec.encode(result, layout));

        Assert.assertEquals(List.of(2000), decoded.validatedTabs().get(0).getLayout());
        Assert.assertEquals(folder.getId(), decoded.validatedLayout().find("tab:hidden").getParentId());
    }

    private static StoredBankTag tab(String name, List<Integer> items, List<Integer> layout, boolean enabled)
    {
        return new StoredBankTag(name, 4151, items, layout, true, enabled).validatedCopy();
    }
}
