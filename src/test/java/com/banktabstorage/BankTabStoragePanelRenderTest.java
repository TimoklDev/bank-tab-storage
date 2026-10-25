package com.banktabstorage;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.JTree;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import net.runelite.client.ui.laf.RuneLiteLAF;
import org.junit.Assert;
import org.junit.Test;

public class BankTabStoragePanelRenderTest
{
    public static void main(String[] args) throws Exception
    {
        SwingUtilities.invokeAndWait(() ->
        {
            RuneLiteLAF.setup();
            List<StoredBankTag> tabs = List.of(
                new StoredBankTag("general gear tab", 4151, List.of(4151), null, false, true),
                new StoredBankTag("zulrah", 12924, List.of(12924), null, false, false),
                new StoredBankTag("muspah", 27690, List.of(27690), null, false, true),
                new StoredBankTag("fishing", 301, List.of(301), null, false, false));
            StoredStorageEntry folder = StoredStorageEntry.folder("Bossing", null);
            StoredStorageEntry skilling = StoredStorageEntry.folder("Skilling", null);
            StorageLayout[] layout = {StorageLayout.empty().reconcile(tabs).addFolder(folder).addFolder(skilling)
                .move("tab:zulrah", folder.getId(), -1).move("tab:muspah", folder.getId(), -1)
                .move(folder.getId(), null, 1)};
            BankTabStoragePanel[] panel = new BankTabStoragePanel[1];
            NoOpActions actions = new NoOpActions()
            {
                private void publish()
                {
                    panel[0].setStorage(tabs, layout[0], "Preview only. No RuneLite profile data is changed.");
                }

                @Override
                public void moveEntry(String id, String parentId, int index)
                {
                    layout[0] = layout[0].move(id, parentId, index);
                    if (parentId != null)
                    {
                        layout[0] = layout[0].updateFolder(parentId, entry -> entry.withExpanded(true));
                    }
                    SwingUtilities.invokeLater(this::publish);
                }

                @Override
                public void setFolderExpanded(String id, boolean expanded)
                {
                    layout[0] = layout[0].updateFolder(id, entry -> entry.withExpanded(expanded));
                }

                @Override
                public void createFolder(String name, String parentId)
                {
                    layout[0] = layout[0].addFolder(StoredStorageEntry.folder(name, parentId));
                    publish();
                }

                @Override
                public void renameFolder(String id, String name)
                {
                    layout[0] = layout[0].updateFolder(id, entry -> entry.withName(name));
                    publish();
                }

                @Override
                public void chooseFolderIcon(String id)
                {
                    layout[0] = layout[0].updateFolder(id, entry -> entry.withIcon(4151));
                    publish();
                }
            };
            panel[0] = new BankTabStoragePanel(actions, new BankTabStoragePanelRenderTest()::itemImage);
            panel[0].setStorage(tabs, layout[0], "Preview only. No RuneLite profile data is changed.");
            JFrame frame = new JFrame("Bank Tab Storage folder preview");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(panel[0]);
            frame.setSize(258, 800);
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
        });
    }

    @Test
    public void rendersPopulatedVault() throws Exception
    {
        final BufferedImage[] rendered = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() ->
        {
            BankTabStoragePanel panel = new BankTabStoragePanel(new NoOpActions(), this::itemImage);
            panel.setTabs(List.of(
                new StoredBankTag("slayer", 4151, List.of(4151, 11840, 12006),
                    List.of(4151, -1, 11840), false, true),
                new StoredBankTag("herblore", 227, List.of(227, 229, 249),
                    null, false, false)), "Saved 2 visible tabs");
            panel.setSize(242, 700);
            layoutTree(panel);
            BufferedImage image = new BufferedImage(242, 700, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            panel.paint(graphics);
            graphics.dispose();
            rendered[0] = image;
        });
        Assert.assertNotNull(rendered[0]);
        Assert.assertEquals(242, rendered[0].getWidth());
        Assert.assertEquals(700, rendered[0].getHeight());
    }

    @Test
    public void reusesTheObservedImageWhileRendering() throws Exception
    {
        AtomicInteger requests = new AtomicInteger();
        SwingUtilities.invokeAndWait(() ->
        {
            BankTabStoragePanel panel = new BankTabStoragePanel(new NoOpActions(), itemId ->
            {
                requests.incrementAndGet();
                return itemImage(itemId);
            });
            panel.setTabs(List.of(new StoredBankTag("slayer", 4151, List.of(4151),
                null, false, true)), "Saved 1 visible tab");
            panel.setSize(242, 700);
            layoutTree(panel);
            BufferedImage rendered = new BufferedImage(242, 700, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = rendered.createGraphics();
            panel.paint(graphics);
            graphics.dispose();
        });
        Assert.assertEquals(1, requests.get());
    }

    @Test
    public void rendersFoldersAtSidebarWidthAndPersistsCollapseActions() throws Exception
    {
        AtomicInteger collapseActions = new AtomicInteger();
        final BufferedImage[] rendered = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() ->
        {
            RuneLiteLAF.setup();
            UIManager.put("Label.font", new Font("Runescape", Font.PLAIN, 16));
            List<StoredBankTag> tabs = List.of(
                new StoredBankTag("general gear tab", 4151, List.of(4151, 11840, 12006),
                    List.of(4151, -1, 11840), false, true),
                new StoredBankTag("zulrah", 12924, List.of(12924), null, false, false),
                new StoredBankTag("muspah", 27690, List.of(27690), null, false, true),
                new StoredBankTag("fishing", 301, List.of(301), null, false, false));
            StoredStorageEntry folder = StoredStorageEntry.folder("Bossing", null).withIcon(4151);
            StoredStorageEntry nested = StoredStorageEntry.folder("Skilling", null);
            StorageLayout layout = StorageLayout.empty().reconcile(tabs).addFolder(folder).addFolder(nested)
                .move("tab:zulrah", folder.getId(), -1)
                .move("tab:muspah", folder.getId(), -1)
                .move("tab:fishing", nested.getId(), -1)
                .move(folder.getId(), null, 1);
            BankTabStoragePanel panel = new BankTabStoragePanel(new NoOpActions()
            {
                @Override
                public void setFolderExpanded(String id, boolean expanded)
                {
                    if (folder.getId().equals(id) && !expanded)
                    {
                        collapseActions.incrementAndGet();
                    }
                }
            }, this::itemImage);
            panel.setStorage(tabs, layout, "Drag onto folders, or between rows to reorder.");
            panel.setSize(242, 760);
            layoutTree(panel);
            JTree tree = findTree(panel);
            tree.setSize(tree.getParent().getSize());
            tree.treeDidChange();
            Assert.assertEquals(Font.DIALOG, tree.getFont().getName());
            Assert.assertFalse(tree.isRootVisible());
            Assert.assertEquals(6, tree.getRowCount());
            for (int row = 0; row < tree.getRowCount(); row++)
            {
                Object value = ((DefaultMutableTreeNode) tree.getPathForRow(row).getLastPathComponent()).getUserObject();
                if (value instanceof StoredStorageEntry && ((StoredStorageEntry) value).getId().equals(folder.getId()))
                {
                    TreePath path = tree.getPathForRow(row);
                    tree.collapsePath(path);
                    Assert.assertEquals(4, tree.getRowCount());
                    tree.expandPath(path);
                    break;
                }
            }
            rendered[0] = new BufferedImage(242, 760, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = rendered[0].createGraphics();
            panel.paint(graphics);
            graphics.dispose();
        });
        Assert.assertEquals(1, collapseActions.get());
        File output = new File("target/bank-tab-storage-folders-preview.png");
        output.getParentFile().mkdirs();
        ImageIO.write(rendered[0], "png", output);
    }

    @Test
    public void droppingIntoEmptySpaceMovesAnItemToTheTopLevelWithoutARootRow() throws Exception
    {
        SwingUtilities.invokeAndWait(() ->
        {
            AtomicInteger moves = new AtomicInteger();
            StoredStorageEntry folder = StoredStorageEntry.folder("Bossing", null);
            List<StoredBankTag> tabs = List.of(
                new StoredBankTag("zulrah", 4151, List.of(4151), null, false, false));
            StorageLayout layout = StorageLayout.empty().reconcile(tabs).addFolder(folder)
                .move("tab:zulrah", folder.getId(), -1);
            BankTabStoragePanel panel = new BankTabStoragePanel(new NoOpActions()
            {
                @Override
                public void moveEntry(String id, String parentId, int index)
                {
                    Assert.assertEquals("tab:zulrah", id);
                    Assert.assertNull(parentId);
                    Assert.assertEquals(-1, index);
                    moves.incrementAndGet();
                }
            }, this::itemImage);
            panel.setStorage(tabs, layout, "Drag to organize");
            panel.setSize(242, 700);
            layoutTree(panel);
            JTree tree = findTree(panel);
            Assert.assertFalse(tree.isRootVisible());
            Rectangle lastRow = tree.getRowBounds(tree.getRowCount() - 1);
            Point emptySpace = new Point(100, lastRow.y + lastRow.height + 20);
            Assert.assertTrue(panel.moveEntryToDrop("tab:zulrah", tree.getPathForRow(0), -1, emptySpace));
            Assert.assertTrue(panel.moveEntryToDrop("tab:zulrah", null, -1, emptySpace));
            Rectangle folderRow = tree.getRowBounds(0);
            Assert.assertFalse(panel.moveEntryToDrop(folder.getId(), tree.getPathForRow(0), -1,
                new Point(folderRow.x + 40, folderRow.y + 20)));
            Assert.assertEquals(2, moves.get());
        });
    }

    private static JTree findTree(java.awt.Container parent)
    {
        for (Component component : parent.getComponents())
        {
            if (component instanceof JTree)
            {
                return (JTree) component;
            }
            if (component instanceof java.awt.Container)
            {
                JTree result = findTree((java.awt.Container) component);
                if (result != null)
                {
                    return result;
                }
            }
        }
        return null;
    }

    private BufferedImage itemImage(int itemId)
    {
        BufferedImage image = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(itemId == 4151 ? new Color(35, 32, 30) : new Color(54, 109, 69));
        graphics.fillOval(4, 2, 28, 28);
        graphics.setColor(new Color(220, 188, 96));
        graphics.drawOval(4, 2, 28, 28);
        graphics.dispose();
        return image;
    }

    private static void layoutTree(java.awt.Container container)
    {
        container.doLayout();
        for (java.awt.Component component : container.getComponents())
        {
            if (component instanceof java.awt.Container)
            {
                layoutTree((java.awt.Container) component);
            }
        }
    }

    private static class NoOpActions implements StorageActions
    {
        @Override
        public void saveCurrentTabs()
        {
        }

        @Override
        public void loadTab(String name)
        {
        }

        @Override
        public void setTabEnabled(String name, boolean enabled)
        {
        }

        @Override
        public void removeTab(String name)
        {
        }

        @Override
        public void importText(String text, boolean replace)
        {
        }

        @Override
        public String exportText()
        {
            return "{}";
        }

        @Override
        public void createFolder(String name, String parentId)
        {
        }

        @Override
        public void renameFolder(String id, String name)
        {
        }

        @Override
        public void chooseFolderIcon(String id)
        {
        }

        @Override
        public void removeFolder(String id)
        {
        }

        @Override
        public void setFolderExpanded(String id, boolean expanded)
        {
        }

        @Override
        public void moveEntry(String id, String parentId, int index)
        {
        }
    }
}
