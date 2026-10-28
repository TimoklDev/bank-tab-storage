# Bank Tab Manager

Bank Tab Manager adds a RuneLite side panel for keeping reusable snapshots of Bank Tags tabs. It requires RuneLite's Bank Tags plugin.

## Features

- Save every currently visible Bank Tags tab
- Restore the tagged items, item icon, layout, and hidden-tag state
- Show or hide saved tabs without deleting their Bank Tags data
- Remove snapshots without deleting tagged items
- Import and export the full storage collection through the clipboard
- Import RuneLite's single-tab `banktags,1` clipboard format
- Show each saved tab with its configured item icon and original name
- Organize saved tabs into collapsible folders and subfolders
- Choose folder icons with the same chatbox item search used by Bank Tags
- Drag tabs and folders to move them or change their order in storage
- Read the sidebar with smooth UI fonts, independent of the game's font
- Replace an item across selected saved and live tag tabs, including hidden tabs
- Hide or import a tag tab directly from its in-game right-click menu

## Behavior

`Save tabs` updates snapshots for all visible Bank Tags tabs. Previously saved tabs that are hidden remain in storage. Import and export are available from the menu beside the panel title.

`Load` replaces the matching live tag data with the saved snapshot and shows the tab. `Hide` removes only the tab from the bank sidebar. Right-click a saved tab and choose `Remove saved tab` to delete its snapshot and hide the tab while keeping its live tagged items, icon, and layout.

Storage follows the active RuneLite profile because it is saved through RuneLite's configuration system.

The plugin was previously named Bank Tab Storage. The existing plugin entry, saved configuration, folders, and earlier clipboard exports remain compatible.

Item counts include unique item IDs from both tags and saved layouts. Repeated layout positions and empty slots do not inflate the count.

## Replacing items

Open your bank and choose `Replace item in tabs` at the top of the panel. Search for an item from your saved or live tag tabs. Select the matching tabs, or use `All matching tabs`, then search for a replacement from items currently in your bank. Review the old item, new item, and affected tabs before applying.

Both live tag data and matching saved snapshots are updated. Repeated item positions in layouts are retained, variation tags remain variation tags, and unchecked tabs are kept as they are. Folder organization and tab visibility are preserved. Empty bank placeholders cannot be selected as upgrades. The replacement must still be in the bank when you apply the change.

## In-game tab menu

Right-click a Bank Tags tab in the bank and choose `Hide tab` to save its current contents in the manager and hide its bank sidebar icon. Choose `Import to Bank Tab Manager` to save or update just that tab without hiding it. The original left-click tab action remains available.

## Folders and ordering

Choose `New folder`, enter a name, then select the folder and choose `Icon` to search for an item in the game chatbox. You must be logged in to use the item search. Select a folder and choose `Rename` to change its name. Double-click a folder or click its expansion arrow to open or collapse it. To create a subfolder, right-click the parent folder and choose `New folder`.

Drop a saved tab onto a folder to put it inside. Drop between rows to reorder tabs and folders. Drop into the empty space below the list to move something back to the top level, or right-click it and choose `Move to top level`. Folders can contain subfolders. Right-click an entry for additional actions, including moving it up, down, or out of its current folder. These changes organize the storage sidebar; they do not reorder the live bank sidebar.

Removing a folder keeps its tabs and subfolders, moving them into the parent folder or the top level. Saving current tabs again updates their snapshots while keeping their folder placement and order.

Exports include folders, icons, order, and collapsed state. Existing version 1 exports and RuneLite single-tab exports are still supported. Merging keeps the placement of tabs already in storage; replacing uses the imported organization.

On first use, existing saved tabs are migrated automatically. The old saved configuration is retained for compatibility with earlier plugin versions.

## Data handling

Saved tabs remain in the active RuneLite profile. Clipboard data is read or written only when the user chooses an import or export action. The plugin does not make network requests.
