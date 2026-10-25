package com.banktabstorage;

import com.google.gson.Gson;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class StorageLayoutTest
{
    private final BankTabStorageCodec codec = new BankTabStorageCodec(new Gson());
    private final List<StoredBankTag> tabs = List.of(tab("slayer"), tab("zulrah"), tab("herblore"));

    @Test
    public void migratesVersionOneWithAllTabsInTheirOriginalOrder()
    {
        String json = "{\"kind\":\"bank-tab-storage\",\"version\":1,\"tabs\":["
            + "{\"name\":\"zulrah\",\"iconItemId\":1,\"itemIds\":[1]},"
            + "{\"name\":\"slayer\",\"iconItemId\":2,\"itemIds\":[2]}]}";
        BankTabStorageData data = codec.decodeData(json);

        Assert.assertEquals(List.of("zulrah", "slayer"), names(data.validatedLayout(), null));
        Assert.assertEquals(2, data.validatedTabs().size());
    }

    @Test
    public void roundTripsFoldersIconsNestingOrderAndCollapsedState()
    {
        StoredStorageEntry folder = StoredStorageEntry.folder("Bosses", null).withIcon(4151).withExpanded(false);
        StoredStorageEntry child = StoredStorageEntry.folder("Serpents", folder.getId());
        StorageLayout layout = StorageLayout.empty().reconcile(tabs).addFolder(folder).addFolder(child)
            .move("tab:zulrah", child.getId(), -1)
            .move("tab:slayer", folder.getId(), -1)
            .move(folder.getId(), null, 0);

        BankTabStorageData decoded = codec.decodeData(codec.encode(tabs, layout));
        StorageLayout result = decoded.validatedLayout();

        Assert.assertEquals(List.of("Bosses", "herblore"), names(result, null));
        Assert.assertEquals(List.of("Serpents", "slayer"), names(result, folder.getId()));
        Assert.assertEquals(List.of("zulrah"), names(result, child.getId()));
        Assert.assertEquals(4151, result.find(folder.getId()).getIconItemId());
        Assert.assertFalse(result.find(folder.getId()).isExpanded());
        Assert.assertEquals(tabs.stream().map(StoredBankTag::validatedCopy).collect(Collectors.toList()),
            decoded.validatedTabs());
    }

    @Test
    public void reordersBothDirectionsUsingDropInsertionIndexes()
    {
        StorageLayout layout = StorageLayout.empty().reconcile(tabs);

        layout = layout.move("tab:slayer", null, 3);
        Assert.assertEquals(List.of("zulrah", "herblore", "slayer"), names(layout, null));
        layout = layout.move("tab:slayer", null, 0);
        Assert.assertEquals(List.of("slayer", "zulrah", "herblore"), names(layout, null));
        layout = layout.move("tab:slayer", null, 1);
        Assert.assertEquals(List.of("slayer", "zulrah", "herblore"), names(layout, null));
    }

    @Test
    public void movingAFolderKeepsItsContents()
    {
        StoredStorageEntry folder = StoredStorageEntry.folder("Bosses", null);
        StoredStorageEntry parent = StoredStorageEntry.folder("Combat", null);
        StorageLayout layout = StorageLayout.empty().reconcile(tabs).addFolder(folder).addFolder(parent)
            .move("tab:zulrah", folder.getId(), -1)
            .move(folder.getId(), parent.getId(), 0);

        Assert.assertEquals(List.of("Bosses"), names(layout, parent.getId()));
        Assert.assertEquals(List.of("zulrah"), names(layout, folder.getId()));
        layout = layout.move(folder.getId(), null, 0);
        Assert.assertEquals(List.of("zulrah"), names(layout, folder.getId()));
    }

    @Test
    public void rejectsMovingAFolderInsideItselfOrItsDescendants()
    {
        StoredStorageEntry parent = StoredStorageEntry.folder("Combat", null);
        StoredStorageEntry child = StoredStorageEntry.folder("Bosses", parent.getId());
        StorageLayout layout = new StorageLayout(List.of(parent, child));

        Assert.assertFalse(layout.canMove(parent.getId(), parent.getId()));
        Assert.assertFalse(layout.canMove(parent.getId(), child.getId()));
        Assert.assertThrows(IllegalArgumentException.class, () -> layout.move(parent.getId(), child.getId(), 0));
    }

    @Test
    public void removingAFolderKeepsItsChildrenAtItsPosition()
    {
        StoredStorageEntry folder = StoredStorageEntry.folder("Bosses", null);
        StoredStorageEntry nested = StoredStorageEntry.folder("More bosses", folder.getId());
        StorageLayout layout = StorageLayout.empty().reconcile(tabs).addFolder(folder).addFolder(nested)
            .move("tab:slayer", folder.getId(), 0)
            .move("tab:zulrah", nested.getId(), 0)
            .move(folder.getId(), null, 0)
            .removeFolder(folder.getId());

        Assert.assertEquals(List.of("slayer", "More bosses", "herblore"), names(layout, null));
        Assert.assertEquals(List.of("zulrah"), names(layout, nested.getId()));
    }

    @Test
    public void refreshingSnapshotsPreservesFoldersAndAddsNewTabsToTheRoot()
    {
        StoredStorageEntry folder = StoredStorageEntry.folder("Bosses", null);
        StorageLayout layout = StorageLayout.empty().reconcile(tabs).addFolder(folder)
            .move("tab:zulrah", folder.getId(), -1);

        layout = layout.reconcile(List.of(tab("slayer"), tab("zulrah"), tab("fishing")));

        Assert.assertEquals(List.of("zulrah"), names(layout, folder.getId()));
        Assert.assertEquals(List.of("slayer", "Bosses", "fishing"), names(layout, null));
    }

    @Test
    public void mergePreservesExistingPlacementAndImportsNewFolderContents()
    {
        StoredStorageEntry existingFolder = StoredStorageEntry.folder("Existing", null);
        StorageLayout existing = StorageLayout.empty().reconcile(List.of(tab("slayer")))
            .addFolder(existingFolder).move("tab:slayer", existingFolder.getId(), -1);
        StoredStorageEntry importedFolder = StoredStorageEntry.folder("Imported", null);
        StorageLayout incoming = StorageLayout.empty().reconcile(tabs).addFolder(importedFolder)
            .move("tab:slayer", importedFolder.getId(), -1)
            .move("tab:zulrah", importedFolder.getId(), -1);

        StorageLayout merged = existing.merge(incoming, tabs, false);

        Assert.assertEquals(List.of("slayer"), names(merged, existingFolder.getId()));
        Assert.assertEquals(List.of("zulrah"), names(merged, importedFolder.getId()));
        Assert.assertEquals(5, merged.getEntries().size());
        StorageLayout replaced = existing.merge(incoming, tabs, true);
        Assert.assertEquals(List.of("slayer", "zulrah"), names(replaced, importedFolder.getId()));
        Assert.assertEquals(4, replaced.getEntries().size());
    }

    @Test
    public void rejectsBrokenFolderReferencesInAnImport()
    {
        StoredStorageEntry folder = StoredStorageEntry.folder("Bosses", "folder:missing");
        Assert.assertThrows(IllegalArgumentException.class, () -> new StorageLayout(List.of(folder)));
        String json = "{\"kind\":\"bank-tab-storage\",\"version\":2,\"tabs\":[],\"entries\":["
            + "{\"id\":\"folder:cycle\",\"name\":\"Bosses\",\"folder\":true,"
            + "\"parentId\":\"folder:cycle\",\"iconItemId\":-1}]}";
        Assert.assertThrows(IllegalArgumentException.class, () -> codec.decodeData(json));
    }

    private static List<String> names(StorageLayout layout, String parentId)
    {
        return layout.children(parentId).stream().map(StoredStorageEntry::getName).collect(Collectors.toList());
    }

    private static StoredBankTag tab(String name)
    {
        return new StoredBankTag(name, 4151, List.of(4151), null, false, false).validatedCopy();
    }
}
