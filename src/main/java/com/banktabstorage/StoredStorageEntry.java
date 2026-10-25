package com.banktabstorage;

import java.util.UUID;

final class StoredStorageEntry
{
    private String id;
    private String name;
    private String parentId;
    private boolean folder;
    private int iconItemId;
    private boolean expanded;

    private StoredStorageEntry()
    {
    }

    private StoredStorageEntry(String id, String name, String parentId, boolean folder,
        int iconItemId, boolean expanded)
    {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.folder = folder;
        this.iconItemId = iconItemId;
        this.expanded = expanded;
    }

    static StoredStorageEntry tab(String name)
    {
        String normalized = StoredBankTag.normalizeName(name);
        return new StoredStorageEntry("tab:" + normalized, normalized, null, false, -1, false);
    }

    static StoredStorageEntry folder(String name, String parentId)
    {
        return new StoredStorageEntry("folder:" + UUID.randomUUID(), name, parentId, true, -1, true)
            .validatedCopy();
    }

    StoredStorageEntry validatedCopy()
    {
        if (id == null || id.length() > 160 || name == null || name.trim().isEmpty()
            || name.length() > 80 || name.codePoints().anyMatch(Character::isISOControl)
            || iconItemId < -1 || (parentId != null && parentId.length() > 160))
        {
            throw new IllegalArgumentException("The saved folder organization contains an invalid entry");
        }
        if (folder)
        {
            if (!id.startsWith("folder:") || id.length() <= 7)
            {
                throw new IllegalArgumentException("A saved folder has an invalid identifier");
            }
        }
        else if (!id.equals("tab:" + StoredBankTag.normalizeName(name)))
        {
            throw new IllegalArgumentException("A saved tab has an invalid organization identifier");
        }
        return new StoredStorageEntry(id, folder ? name.trim() : StoredBankTag.normalizeName(name),
            parentId, folder, iconItemId, expanded);
    }

    StoredStorageEntry withParent(String parent)
    {
        return new StoredStorageEntry(id, name, parent, folder, iconItemId, expanded).validatedCopy();
    }

    StoredStorageEntry withName(String value)
    {
        return new StoredStorageEntry(id, value, parentId, folder, iconItemId, expanded).validatedCopy();
    }

    StoredStorageEntry withIcon(int value)
    {
        return new StoredStorageEntry(id, name, parentId, folder, value, expanded).validatedCopy();
    }

    StoredStorageEntry withExpanded(boolean value)
    {
        return new StoredStorageEntry(id, name, parentId, folder, iconItemId, value).validatedCopy();
    }

    String getId()
    {
        return id;
    }

    String getName()
    {
        return name;
    }

    String getParentId()
    {
        return parentId;
    }

    boolean isFolder()
    {
        return folder;
    }

    int getIconItemId()
    {
        return iconItemId;
    }

    boolean isExpanded()
    {
        return expanded;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
