package com.banktagsstorage;

import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.plugins.banktags.tabs.LayoutManager;
import net.runelite.client.plugins.banktags.tabs.TabInterface;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
    name = "Bank Tab Storage",
    description = "Save, restore, show, hide, import, and export complete Bank Tags tabs. Requires the Bank Tags plugin!",
    tags = {"bank", "tags", "storage", "layout", "loadout", "preset"}
)
@PluginDependency(BankTagsPlugin.class)
public final class BankTagsStoragePlugin extends Plugin implements StorageActions
{
    private static final String CONFIG_GROUP = "banktagsstorage";
    private static final String CONFIG_KEY = "savedTabs";
    private volatile List<StoredBankTag> storedTabs = List.of();
    private volatile boolean changingBankTabs;

    @Inject
    private Gson gson;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ConfigManager configManager;

    @Inject
    private ItemManager itemManager;

    @Inject
    private TagManager tagManager;

    @Inject
    private LayoutManager layoutManager;

    @Inject
    private TabInterface tabInterface;

    @Inject
    private BankTagsPlugin bankTagsPlugin;

    private BankTagsAccess bankTags;
    private BankTagsStorageCodec codec;
    private BankTagsStoragePanel panel;
    private NavigationButton navigationButton;

    @Override
    protected void startUp()
    {
        codec = new BankTagsStorageCodec(gson);
        bankTags = new BankTagsAccess(configManager, tagManager, layoutManager, client, tabInterface, bankTagsPlugin);
        loadStorage();
        storedTabs = immutable(StoredTabCollection.syncEnabled(storedTabs, bankTags.visibleNames()));
        SwingUtilities.invokeLater(() ->
        {
            panel = new BankTagsStoragePanel(this, itemManager::getImage);
            panel.setTabs(storedTabs, "Save the tabs you want to keep ready");
            navigationButton = NavigationButton.builder()
                .tooltip("Bank Tab Storage")
                .icon(createNavigationIcon())
                .priority(5)
                .panel(panel)
                .build();
            clientToolbar.addNavigation(navigationButton);
        });
    }

    @Override
    protected void shutDown()
    {
        SwingUtilities.invokeLater(() ->
        {
            if (navigationButton != null)
            {
                clientToolbar.removeNavigation(navigationButton);
            }
            navigationButton = null;
            panel = null;
        });
        bankTags = null;
        codec = null;
        storedTabs = List.of();
        changingBankTabs = false;
    }

    @Subscribe
    public void onProfileChanged(ProfileChanged event)
    {
        clientThread.invokeLater(() ->
        {
            loadStorage();
            storedTabs = immutable(StoredTabCollection.syncEnabled(storedTabs, bankTags.visibleNames()));
            publish("Loaded storage for the active profile");
        });
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!changingBankTabs
            && BankTagsPlugin.CONFIG_GROUP.equals(event.getGroup())
            && BankTagsPlugin.TAG_TABS_CONFIG.equals(event.getKey()))
        {
            clientThread.invokeLater(() ->
            {
                storedTabs = immutable(StoredTabCollection.syncEnabled(storedTabs, bankTags.visibleNames()));
                persist();
                publish("Bank tab visibility updated");
            });
        }
    }

    @Override
    public void saveCurrentTabs()
    {
        runOnClient(() ->
        {
            List<StoredBankTag> current = bankTags.captureVisibleTabs();
            storedTabs = immutable(StoredTabCollection.merge(storedTabs, current, false));
            storedTabs = immutable(StoredTabCollection.syncEnabled(storedTabs, bankTags.visibleNames()));
            persist();
            publish(current.isEmpty() ? "No visible Bank Tags tabs were found"
                : "Saved " + current.size() + (current.size() == 1 ? " visible tab" : " visible tabs"));
        });
    }

    @Override
    public void loadTab(String name)
    {
        runOnClient(() ->
        {
            StoredBankTag tab = requireTab(name);
            changingBankTabs = true;
            try
            {
                bankTags.applySnapshot(tab);
                bankTags.setVisible(tab.getName(), true);
                bankTags.refreshBank(null);
            }
            finally
            {
                changingBankTabs = false;
            }
            storedTabs = immutable(StoredTabCollection.setEnabled(storedTabs, tab.getName(), true));
            persist();
            publish("Loaded '" + tab.getName() + "' into Bank Tags");
        });
    }

    @Override
    public void setTabEnabled(String name, boolean enabled)
    {
        runOnClient(() ->
        {
            StoredBankTag tab = requireTab(name);
            changingBankTabs = true;
            try
            {
                bankTags.setVisible(tab.getName(), enabled);
                bankTags.refreshBank(enabled ? null : tab.getName());
            }
            finally
            {
                changingBankTabs = false;
            }
            storedTabs = immutable(StoredTabCollection.setEnabled(storedTabs, tab.getName(), enabled));
            persist();
            publish((enabled ? "Showing '" : "Hid '") + tab.getName() + "'");
        });
    }

    @Override
    public void removeTab(String name)
    {
        runOnClient(() ->
        {
            StoredBankTag tab = requireTab(name);
            changingBankTabs = true;
            try
            {
                bankTags.setVisible(tab.getName(), false);
                bankTags.refreshBank(tab.getName());
            }
            finally
            {
                changingBankTabs = false;
            }
            storedTabs = immutable(StoredTabCollection.remove(storedTabs, tab.getName()));
            persist();
            publish("Removed '" + tab.getName() + "' from storage");
        });
    }

    @Override
    public void importText(String text, boolean replace)
    {
        List<StoredBankTag> imported;
        try
        {
            imported = codec.decode(text);
        }
        catch (IllegalArgumentException exception)
        {
            showError(exception.getMessage());
            return;
        }
        runOnClient(() ->
        {
            storedTabs = immutable(StoredTabCollection.merge(storedTabs, imported, replace));
            storedTabs = immutable(StoredTabCollection.syncEnabled(storedTabs, bankTags.visibleNames()));
            persist();
            publish("Imported " + imported.size() + (imported.size() == 1 ? " saved tab" : " saved tabs"));
        });
    }

    @Override
    public String exportText()
    {
        return codec.encode(storedTabs);
    }

    private void runOnClient(Runnable action)
    {
        clientThread.invokeLater(() ->
        {
            try
            {
                action.run();
            }
            catch (RuntimeException exception)
            {
                showError(exception.getMessage() == null ? "The Bank Tags operation failed" : exception.getMessage());
            }
        });
    }

    private StoredBankTag requireTab(String name)
    {
        StoredBankTag tab = StoredTabCollection.find(storedTabs, name);
        if (tab == null)
        {
            throw new IllegalArgumentException("The selected saved tab no longer exists");
        }
        return tab;
    }

    private void loadStorage()
    {
        String json = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY);
        if (json == null || json.trim().isEmpty())
        {
            storedTabs = List.of();
            return;
        }
        try
        {
            storedTabs = immutable(codec.decode(json));
        }
        catch (IllegalArgumentException exception)
        {
            storedTabs = List.of();
        }
    }

    private void persist()
    {
        configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY, codec.encode(storedTabs));
    }

    private void publish(String notice)
    {
        List<StoredBankTag> snapshot = immutable(storedTabs);
        SwingUtilities.invokeLater(() ->
        {
            if (panel != null)
            {
                panel.setTabs(snapshot, notice);
            }
        });
    }

    private void showError(String message)
    {
        SwingUtilities.invokeLater(() ->
        {
            if (panel != null)
            {
                panel.showError(message);
            }
        });
    }

    private static List<StoredBankTag> immutable(List<StoredBankTag> source)
    {
        List<StoredBankTag> result = new ArrayList<>(source.size());
        for (StoredBankTag tab : source)
        {
            result.add(tab.copy());
        }
        return List.copyOf(result);
    }

    private static BufferedImage createNavigationIcon()
    {
        return ImageUtil.loadImageResource(BankTagsStoragePlugin.class, "sidebar-icon.png");
    }
}
