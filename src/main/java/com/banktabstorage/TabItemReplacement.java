package com.banktabstorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

final class TabItemReplacement
{
    private final IntUnaryOperator canonicalize;
    private final IntUnaryOperator variationBase;

    TabItemReplacement(IntUnaryOperator canonicalize, IntUnaryOperator variationBase)
    {
        this.canonicalize = canonicalize;
        this.variationBase = variationBase;
    }

    boolean contains(StoredBankTag tab, int itemId)
    {
        int source = canonicalize.applyAsInt(itemId);
        boolean variations = hasVariationTag(tab, source);
        for (int stored : tab.getAllItemIds())
        {
            if (matches(stored, source, variations))
            {
                return true;
            }
        }
        return false;
    }

    StoredBankTag replace(StoredBankTag tab, int sourceId, int replacementId)
    {
        if (sourceId < 0 || replacementId < 0)
        {
            throw new IllegalArgumentException("Choose valid items to replace");
        }
        int source = canonicalize.applyAsInt(sourceId);
        int replacement = canonicalize.applyAsInt(replacementId);
        boolean variations = hasVariationTag(tab, source);
        List<Integer> items = new ArrayList<>();
        for (int stored : tab.getItemIds())
        {
            items.add(matches(Math.abs(stored), source, variations)
                ? stored < 0 ? -variationBase.applyAsInt(replacement) : replacement : stored);
        }
        List<Integer> layout = tab.getLayout();
        if (layout != null)
        {
            for (int index = 0; index < layout.size(); index++)
            {
                int stored = layout.get(index);
                if (stored >= 0 && matches(stored, source, variations))
                {
                    layout.set(index, replacement);
                }
            }
        }
        return new StoredBankTag(tab.getName(), tab.getIconItemId(), items, layout,
            tab.isHidden(), tab.isEnabled()).validatedCopy();
    }

    List<StoredBankTag> replaceSelected(List<StoredBankTag> tabs, Set<String> selected,
        int sourceId, int replacementId)
    {
        List<StoredBankTag> result = new ArrayList<>();
        for (StoredBankTag tab : tabs)
        {
            result.add(selected.contains(tab.getName()) ? replace(tab, sourceId, replacementId) : tab.copy());
        }
        return result;
    }

    private boolean hasVariationTag(StoredBankTag tab, int source)
    {
        for (int stored : tab.getItemIds())
        {
            if (stored < 0 && variationBase.applyAsInt(canonicalize.applyAsInt(-stored))
                == variationBase.applyAsInt(source))
            {
                return true;
            }
        }
        return false;
    }

    private boolean matches(int stored, int source, boolean variations)
    {
        int canonical = canonicalize.applyAsInt(stored);
        return canonical == source || variations
            && variationBase.applyAsInt(canonical) == variationBase.applyAsInt(source);
    }
}
