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

## Behavior

`Save current tabs` updates snapshots for all visible Bank Tags tabs. Previously saved tabs that are hidden remain in storage.

`Load` replaces the matching live tag data with the saved snapshot and shows the tab. `Hide` removes only the tab from the bank sidebar. `Remove` deletes the saved snapshot and hides the tab, but leaves its live tagged items, icon, and layout untouched.

Storage follows the active RuneLite profile because it is saved through RuneLite's configuration system.

## Data handling

Saved tabs remain in the active RuneLite profile. Clipboard data is read or written only when the user chooses an import or export action. The plugin does not make network requests.
