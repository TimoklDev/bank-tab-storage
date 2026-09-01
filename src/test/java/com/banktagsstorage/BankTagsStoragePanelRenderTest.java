package com.banktagsstorage;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.Assert;
import org.junit.Test;

public class BankTagsStoragePanelRenderTest
{
    @Test
    public void rendersPopulatedVault() throws Exception
    {
        final BufferedImage[] rendered = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() ->
        {
            BankTagsStoragePanel panel = new BankTagsStoragePanel(new NoOpActions(), this::itemImage);
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

    private static final class NoOpActions implements StorageActions
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
    }
}
