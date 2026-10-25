# Bank Tab Storage

Bank Tab Storage adds a RuneLite side panel for keeping reusable snapshots of Bank Tags tabs. It requires RuneLite's Bank Tags plugin.

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

## Behavior

`Save tabs` updates snapshots for all visible Bank Tags tabs. Previously saved tabs that are hidden remain in storage. Import and export are available from the menu beside the panel title.

`Load` replaces the matching live tag data with the saved snapshot and shows the tab. `Hide` removes only the tab from the bank sidebar. Right-click a saved tab and choose `Remove saved tab` to delete its snapshot and hide the tab while keeping its live tagged items, icon, and layout.

Storage follows the active RuneLite profile because it is saved through RuneLite's configuration system.

## Folders and ordering

Choose `New folder`, enter a name, then select the folder and choose `Icon` to search for an item in the game chatbox. You must be logged in to use the item search. Select a folder and choose `Rename` to change its name. Double-click a folder or click its expansion arrow to open or collapse it. To create a subfolder, right-click the parent folder and choose `New folder`.

Drop a saved tab onto a folder to put it inside. Drop between rows to reorder tabs and folders. Drop into the empty space below the list to move something back to the top level, or right-click it and choose `Move to top level`. Folders can contain subfolders. Right-click an entry for additional actions, including moving it up, down, or out of its current folder. These changes organize the storage sidebar; they do not reorder the live bank sidebar.

Removing a folder keeps its tabs and subfolders, moving them into the parent folder or the top level. Saving current tabs again updates their snapshots while keeping their folder placement and order.

Exports include folders, icons, order, and collapsed state. Existing version 1 exports and RuneLite single-tab exports are still supported. Merging keeps the placement of tabs already in storage; replacing uses the imported organization.

On first use, existing saved tabs are migrated automatically. The old saved configuration is retained for compatibility with earlier plugin versions.

## Data handling

Saved tabs remain in the active RuneLite profile. Clipboard data is read or written only when the user chooses an import or export action. The plugin does not make network requests.
