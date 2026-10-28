package com.banktabstorage;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import net.runelite.api.Item;

final class ReplacementBankItems
{
    private ReplacementBankItems()
    {
    }

    static Set<Integer> collect(Item[] items, IntUnaryOperator canonicalize, IntPredicate isPlaceholder)
    {
        Set<Integer> result = new LinkedHashSet<>();
        for (Item item : items)
        {
            if (item != null && item.getId() >= 0 && item.getQuantity() > 0 && !isPlaceholder.test(item.getId()))
            {
                result.add(canonicalize.applyAsInt(item.getId()));
            }
        }
        return result;
    }
}
