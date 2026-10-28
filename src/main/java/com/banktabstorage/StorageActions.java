package com.banktabstorage;

import java.util.List;

interface StorageActions
{
    void saveCurrentTabs();

    void loadTab(String name);

    void setTabEnabled(String name, boolean enabled);

    void removeTab(String name);

    void importText(String text, boolean replace);

    String exportText();

    void createFolder(String name, String parentId);

    void renameFolder(String id, String name);

    void chooseFolderIcon(String id);

    void removeFolder(String id);

    void setFolderExpanded(String id, boolean expanded);

    void moveEntry(String id, String parentId, int index);

    void startItemReplacement();

    void selectReplacementSource(long requestId, int itemId);

    void selectReplacementTabs(long requestId, List<String> names);

    void replaceItems(long requestId, int itemId);

    void cancelItemReplacement(long requestId);
}
