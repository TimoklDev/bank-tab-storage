package com.banktabstorage;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BankTabStorageRuneLiteTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(BankTabStoragePlugin.class);
        RuneLite.main(args);
    }
}
