package com.banktagsstorage;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BankTagsStorageRuneLiteTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(BankTagsStoragePlugin.class);
        RuneLite.main(args);
    }
}

