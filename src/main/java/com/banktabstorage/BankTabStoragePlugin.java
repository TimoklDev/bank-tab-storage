package com.banktabstorage;

import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.chatbox.ChatboxItemSearch;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.plugins.banktags.tabs.LayoutManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
    name = "Bank Tab Storage",
    description = "Save, restore, show, hide, import, and export complete Bank Tags tabs. Requires the Bank Tags plugin!",
    tags = {"bank", "tags", "storage", "layout", "loadout", "preset"}
)
@PluginDependency(BankTagsPlugin.class)
public final class BankTabStoragePlugin extends Plugin implements StorageActions
{
    private static final String CONFIG_GROUP = "banktabstorage";
    private static final String LEGACY_CONFIG_GROUP = "banktagsstorage";
    private static final String CONFIG_KEY = "savedStorageV2";
    private static final String PREVIOUS_CONFIG_KEY = "savedTabs";
    private volatile List<StoredBankTag> storedTabs = List.of();
    private volatile StorageLayout storageLayout = StorageLayout.empty();
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
    private ChatboxItemSearch itemSearch;

    @Inject
    private TagManager tagManager;

    @Inject
    private LayoutManager layoutManager;

    @Inject
    private BankTagsService bankTagsService;

    private BankTagsAccess bankTags;
    private BankTabStorageCodec codec;
    private BankTabStoragePanel panel;
    private NavigationButton navigationButton;

    @Override
    protected void startUp()
    {
        codec = new BankTabStorageCodec(gson);
        bankTags = new BankTagsAccess(configManager, tagManager, layoutManager,
            new BankTagTabsReloader(configManager, client), bankTagsService);
        loadStorage();
        storedTabs = immutable(StoredTabCollection.syncEnabled(storedTabs, bankTags.visibleNames()));
        SwingUtilities.invokeLater(() ->
        {
            panel = new BankTabStoragePanel(this, itemManager::getImage);
            panel.setStorage(storedTabs, storageLayout, "Drag to organize. Right-click for more.");
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
        storageLayout = StorageLayout.empty();
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
                bankTags.refreshBank(tab.getName(), false);
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
                bankTags.refreshBank(tab.getName(), !enabled);
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
                bankTags.refreshBank(tab.getName(), true);
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
        BankTabStorageData imported;
        try
        {
            imported = codec.decodeData(text);
        }
        catch (IllegalArgumentException exception)
        {
            showError(exception.getMessage());
            return;
        }
        runOnClient(() ->
        {
            List<StoredBankTag> incomingTabs = imported.validatedTabs();
            List<StoredBankTag> merged = immutable(StoredTabCollection.merge(storedTabs, incomingTabs, replace));
            StorageLayout mergedLayout = storageLayout.merge(imported.validatedLayout(), merged, replace);
            storedTabs = merged;
            storageLayout = mergedLayout;
            storedTabs = immutable(StoredTabCollection.syncEnabled(storedTabs, bankTags.visibleNames()));
            persist();
            publish("Imported " + incomingTabs.size() + (incomingTabs.size() == 1 ? " saved tab" : " saved tabs"));
        });
    }

    @Override
    public String exportText()
    {
        return codec.encode(storedTabs, storageLayout);
    }

    @Override
    public void createFolder(String name, String parentId)
    {
        runOnClient(() ->
        {
            StoredStorageEntry folder = StoredStorageEntry.folder(name, parentId);
            storageLayout = storageLayout.addFolder(folder);
            persist();
            publish("Created '" + folder.getName() + "'. Select Icon to choose its item");
        });
    }

    @Override
    public void renameFolder(String id, String name)
    {
        runOnClient(() ->
        {
            storageLayout = storageLayout.updateFolder(id, folder -> folder.withName(name));
            persist();
            publish("Folder renamed");
        });
    }

    @Override
    public void chooseFolderIcon(String id)
    {
        runOnClient(() ->
        {
            if (client.getGameState() != GameState.LOGGED_IN)
            {
                throw new IllegalArgumentException("Log in to choose a folder icon using the item search");
            }
            storageLayout.find(id);
            long profileId = configManager.getProfile().getId();
            itemSearch.tooltipText("Use as folder icon")
                .onItemSelected(itemId -> runOnClient(() ->
                {
                    if (configManager.getProfile().getId() == profileId)
                    {
                        storageLayout = storageLayout.updateFolder(id, folder -> folder.withIcon(itemId));
                        persist();
                        publish("Folder icon updated");
                    }
                }))
                .value("")
                .build();
            publish("Choose an item in the chatbox for the folder icon");
        });
    }

    @Override
    public void removeFolder(String id)
    {
        runOnClient(() ->
        {
            storageLayout = storageLayout.removeFolder(id);
            persist();
            publish("Folder removed. Its contents have been kept");
        });
    }

    @Override
    public void setFolderExpanded(String id, boolean expanded)
    {
        runOnClient(() ->
        {
            storageLayout = storageLayout.updateFolder(id, folder -> folder.withExpanded(expanded));
            persist();
        });
    }

    @Override
    public void moveEntry(String id, String parentId, int index)
    {
        runOnClient(() ->
        {
            storageLayout = storageLayout.move(id, parentId, index);
            if (parentId != null)
            {
                storageLayout = storageLayout.updateFolder(parentId, folder -> folder.withExpanded(true));
            }
            persist();
            publish("Storage order updated");
        });
    }

    private void runOnClient(Runnable action)
    {
        clientThread.invokeLater(() ->
        {
            if (bankTags == null || codec == null)
            {
                return;
            }
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
        boolean legacy = false;
        if (json == null || json.trim().isEmpty())
        {
            json = configManager.getConfiguration(CONFIG_GROUP, PREVIOUS_CONFIG_KEY);
            if (json == null || json.trim().isEmpty())
            {
                json = configManager.getConfiguration(LEGACY_CONFIG_GROUP, PREVIOUS_CONFIG_KEY);
            }
            legacy = json != null && !json.trim().isEmpty();
        }
        if (json == null || json.trim().isEmpty())
        {
            storedTabs = List.of();
            storageLayout = StorageLayout.empty();
            return;
        }
        try
        {
            BankTabStorageData data = codec.decodeData(json);
            storedTabs = immutable(data.validatedTabs());
            storageLayout = data.validatedLayout();
            if (legacy)
            {
                persist();
            }
        }
        catch (IllegalArgumentException exception)
        {
            storedTabs = List.of();
            storageLayout = StorageLayout.empty();
        }
    }

    private void persist()
    {
        storageLayout = storageLayout.reconcile(storedTabs);
        configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY, codec.encode(storedTabs, storageLayout));
    }

    private void publish(String notice)
    {
        List<StoredBankTag> snapshot = immutable(storedTabs);
        StorageLayout layoutSnapshot = storageLayout;
        SwingUtilities.invokeLater(() ->
        {
            if (panel != null)
            {
                panel.setStorage(snapshot, layoutSnapshot, notice);
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
        return ImageUtil.loadImageResource(BankTabStoragePlugin.class, "sidebar-icon.png");
    }
}
