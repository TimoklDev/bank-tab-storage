package com.banktagsstorage;

interface StorageActions
{
    void saveCurrentTabs();

    void loadTab(String name);

    void setTabEnabled(String name, boolean enabled);

    void removeTab(String name);

    void importText(String text, boolean replace);

    String exportText();
}

