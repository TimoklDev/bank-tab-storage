package com.banktabstorage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

final class StorageLayout
{
    private static final int MAX_ENTRIES = 1000;
    private final List<StoredStorageEntry> entries;

    StorageLayout(List<StoredStorageEntry> source)
    {
        if (source == null || source.size() > MAX_ENTRIES)
        {
            throw new IllegalArgumentException("The saved folder organization is too large");
        }
        Map<String, StoredStorageEntry> byId = new LinkedHashMap<>();
        for (StoredStorageEntry entry : source)
        {
            if (entry == null)
            {
                throw new IllegalArgumentException("The saved folder organization contains an invalid entry");
            }
            StoredStorageEntry copy = entry.validatedCopy();
            if (byId.putIfAbsent(copy.getId(), copy) != null)
            {
                throw new IllegalArgumentException("The saved folder organization contains duplicate entries");
            }
        }
        for (StoredStorageEntry entry : byId.values())
        {
            Set<String> visited = new HashSet<>();
            visited.add(entry.getId());
            String parentId = entry.getParentId();
            while (parentId != null)
            {
                StoredStorageEntry parent = byId.get(parentId);
                if (parent == null || !parent.isFolder() || !visited.add(parentId) || visited.size() > 32)
                {
                    throw new IllegalArgumentException("The saved folder organization contains an invalid parent");
                }
                parentId = parent.getParentId();
            }
        }
        entries = List.copyOf(byId.values());
    }

    static StorageLayout empty()
    {
        return new StorageLayout(List.of());
    }

    List<StoredStorageEntry> getEntries()
    {
        return entries;
    }

    StoredStorageEntry find(String id)
    {
        for (StoredStorageEntry entry : entries)
        {
            if (entry.getId().equals(id))
            {
                return entry;
            }
        }
        throw new IllegalArgumentException("The selected tab or folder no longer exists");
    }

    List<StoredStorageEntry> children(String parentId)
    {
        List<StoredStorageEntry> result = new ArrayList<>();
        for (StoredStorageEntry entry : entries)
        {
            if (Objects.equals(parentId, entry.getParentId()))
            {
                result.add(entry);
            }
        }
        return result;
    }

    StorageLayout reconcile(List<StoredBankTag> tabs)
    {
        Set<String> remaining = new HashSet<>();
        for (StoredBankTag tab : tabs)
        {
            remaining.add(tab.getName());
        }
        List<StoredStorageEntry> result = new ArrayList<>();
        for (StoredStorageEntry entry : entries)
        {
            if (entry.isFolder() || remaining.remove(entry.getName()))
            {
                result.add(entry);
            }
        }
        for (StoredBankTag tab : tabs)
        {
            if (remaining.remove(tab.getName()))
            {
                result.add(StoredStorageEntry.tab(tab.getName()));
            }
        }
        return new StorageLayout(result);
    }

    StorageLayout addFolder(StoredStorageEntry folder)
    {
        List<StoredStorageEntry> result = new ArrayList<>(entries);
        result.add(folder);
        return new StorageLayout(result);
    }

    StorageLayout updateFolder(String id, UnaryOperator<StoredStorageEntry> update)
    {
        if (!find(id).isFolder())
        {
            throw new IllegalArgumentException("Select a folder first");
        }
        List<StoredStorageEntry> result = new ArrayList<>();
        for (StoredStorageEntry entry : entries)
        {
            result.add(entry.getId().equals(id) ? update.apply(entry) : entry);
        }
        return new StorageLayout(result);
    }

    boolean canMove(String id, String parentId)
    {
        try
        {
            find(id);
            String ancestor = parentId;
            while (ancestor != null)
            {
                if (id.equals(ancestor))
                {
                    return false;
                }
                StoredStorageEntry parent = find(ancestor);
                if (!parent.isFolder())
                {
                    return false;
                }
                ancestor = parent.getParentId();
            }
            return true;
        }
        catch (IllegalArgumentException exception)
        {
            return false;
        }
    }

    StorageLayout move(String id, String parentId, int index)
    {
        if (!canMove(id, parentId))
        {
            throw new IllegalArgumentException("A folder cannot be moved into itself or one of its children");
        }
        StoredStorageEntry moving = find(id);
        List<StoredStorageEntry> siblings = children(parentId);
        int target = index < 0 ? siblings.size() : Math.min(index, siblings.size());
        int previous = siblings.indexOf(moving);
        if (previous >= 0 && previous < target)
        {
            target--;
        }
        siblings.remove(moving);
        List<StoredStorageEntry> result = new ArrayList<>(entries);
        result.remove(moving);
        int insertion = target < siblings.size() ? result.indexOf(siblings.get(target))
            : siblings.isEmpty() ? result.size() : result.indexOf(siblings.get(siblings.size() - 1)) + 1;
        result.add(insertion, moving.withParent(parentId));
        return new StorageLayout(result);
    }

    StorageLayout removeFolder(String id)
    {
        StoredStorageEntry folder = find(id);
        if (!folder.isFolder())
        {
            throw new IllegalArgumentException("Select a folder first");
        }
        List<StoredStorageEntry> contents = children(id);
        List<StoredStorageEntry> result = new ArrayList<>();
        for (StoredStorageEntry entry : entries)
        {
            if (entry.getId().equals(id))
            {
                for (StoredStorageEntry child : contents)
                {
                    result.add(child.withParent(folder.getParentId()));
                }
            }
            else if (!id.equals(entry.getParentId()))
            {
                result.add(entry);
            }
        }
        return new StorageLayout(result);
    }

    StorageLayout merge(StorageLayout incoming, List<StoredBankTag> tabs, boolean replace)
    {
        if (replace)
        {
            return incoming.reconcile(tabs);
        }
        Map<String, StoredStorageEntry> result = new LinkedHashMap<>();
        for (StoredStorageEntry entry : entries)
        {
            result.put(entry.getId(), entry);
        }
        for (StoredStorageEntry entry : incoming.entries)
        {
            result.putIfAbsent(entry.getId(), entry);
        }
        return new StorageLayout(new ArrayList<>(result.values())).reconcile(tabs);
    }
}
