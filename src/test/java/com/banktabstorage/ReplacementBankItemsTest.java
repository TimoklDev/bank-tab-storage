package com.banktabstorage;

import java.util.Set;
import net.runelite.api.Item;
import org.junit.Assert;
import org.junit.Test;

public class ReplacementBankItemsTest
{
    @Test
    public void onlyIncludesRealPositiveQuantityBankItems()
    {
        Item[] bank = {new Item(1000, 1), new Item(2000, 0), new Item(3000, 1),
            new Item(-1, 0), null, new Item(1100, 4)};

        Set<Integer> result = ReplacementBankItems.collect(bank, id -> id == 1100 ? 1000 : id,
            id -> id == 3000);

        Assert.assertEquals(Set.of(1000), result);
    }
}
