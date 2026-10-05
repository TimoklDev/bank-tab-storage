package com.banktabstorage;

import net.runelite.client.plugins.banktags.BankTag;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.tabs.Layout;
import org.junit.Assert;
import org.junit.Test;

public class BankTagsAccessTest
{
    @Test
    public void closesAHiddenActiveTab()
    {
        FakeBankTagsService service = new FakeBankTagsService("slayer");
        BankTagsAccess access = new BankTagsAccess(null, null, null, service);

        access.refreshActiveTab("Slayer", true);

        Assert.assertTrue(service.closed);
        Assert.assertNull(service.openedTag);
    }

    @Test
    public void refreshesAnActiveTabThroughThePublicService()
    {
        FakeBankTagsService service = new FakeBankTagsService("slayer");
        BankTagsAccess access = new BankTagsAccess(null, null, null, service);

        access.refreshActiveTab("slayer", false);

        Assert.assertFalse(service.closed);
        Assert.assertEquals("slayer", service.openedTag);
        Assert.assertEquals(BankTagsService.OPTION_ALLOW_MODIFICATIONS, service.openedOptions);
    }

    @Test
    public void leavesAnUnrelatedActiveTabAlone()
    {
        FakeBankTagsService service = new FakeBankTagsService("slayer");
        BankTagsAccess access = new BankTagsAccess(null, null, null, service);

        access.refreshActiveTab("herblore", false);

        Assert.assertFalse(service.closed);
        Assert.assertNull(service.openedTag);
    }

    private static final class FakeBankTagsService implements BankTagsService
    {
        private final String activeTag;
        private String openedTag;
        private int openedOptions;
        private boolean closed;

        private FakeBankTagsService(String activeTag)
        {
            this.activeTag = activeTag;
        }

        @Override
        public void openBankTag(String tag, int options)
        {
            openedTag = tag;
            openedOptions = options;
        }

        @Override
        public void closeBankTag()
        {
            closed = true;
        }

        @Override
        public String getActiveTag()
        {
            return activeTag;
        }

        @Override
        public BankTag getActiveBankTag()
        {
            return null;
        }

        @Override
        public Layout getActiveLayout()
        {
            return null;
        }
    }
}
