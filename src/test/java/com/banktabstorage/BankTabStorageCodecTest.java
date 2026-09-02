package com.banktabstorage;

import com.google.gson.Gson;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class BankTabStorageCodecTest
{
    private final BankTabStorageCodec codec = new BankTabStorageCodec(new Gson());

    @Test
    public void roundTripsCompleteSnapshots()
    {
        StoredBankTag source = new StoredBankTag("Slayer", 4151, List.of(4151, -11840),
            List.of(4151, -1, 11840), true, true);
        List<StoredBankTag> decoded = codec.decode(codec.encode(List.of(source)));
        Assert.assertEquals(List.of(source.validatedCopy()), decoded);
    }

    @Test
    public void importsRuneLiteSingleTabFormat()
    {
        List<StoredBankTag> decoded = codec.decode("banktags,1,slayer,4151,4151,layout,2,11840");
        Assert.assertEquals(1, decoded.size());
        StoredBankTag tab = decoded.get(0);
        Assert.assertEquals("slayer", tab.getName());
        Assert.assertEquals(4151, tab.getIconItemId());
        Assert.assertEquals(List.of(4151, 11840), tab.getItemIds());
        Assert.assertEquals(List.of(-1, -1, 11840), tab.getLayout());
        Assert.assertFalse(tab.isEnabled());
    }

    @Test
    public void importsLegacyStorageFormat()
    {
        Assert.assertTrue(codec.decode("{\"kind\":\"bank-tags-storage\",\"version\":1,\"tabs\":[]}").isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownJson()
    {
        codec.decode("{\"kind\":\"something-else\",\"version\":1,\"tabs\":[]}");
    }
}
