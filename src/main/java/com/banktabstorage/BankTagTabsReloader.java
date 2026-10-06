package com.banktabstorage;

import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.banktags.BankTagsPlugin;

final class BankTagTabsReloader implements BankTabReloader
{
    private static final String USE_TABS_CONFIG = "useTabs";
    private final ConfigManager configManager;
    private final Client client;

    BankTagTabsReloader(ConfigManager configManager, Client client)
    {
        this.configManager = configManager;
        this.client = client;
    }

    @Override
    public void reload()
    {
        Widget bank = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        if (bank == null || bank.getOnLoadListener() == null)
        {
            return;
        }

        String previous = configManager.getConfiguration(BankTagsPlugin.CONFIG_GROUP, USE_TABS_CONFIG);
        if (previous != null && !previous.isEmpty() && !Boolean.parseBoolean(previous))
        {
            return;
        }

        configManager.setConfiguration(BankTagsPlugin.CONFIG_GROUP, USE_TABS_CONFIG, false);
        try
        {
            runBankInit();
        }
        finally
        {
            if (previous == null)
            {
                configManager.unsetConfiguration(BankTagsPlugin.CONFIG_GROUP, USE_TABS_CONFIG);
            }
            else
            {
                configManager.setConfiguration(BankTagsPlugin.CONFIG_GROUP, USE_TABS_CONFIG, previous);
            }
        }
        runBankInit();
    }

    private void runBankInit()
    {
        Widget bank = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        if (bank != null && bank.getOnLoadListener() != null)
        {
            client.createScriptEventBuilder(bank.getOnLoadListener())
                .setSource(bank)
                .build()
                .run();
        }
    }
}
