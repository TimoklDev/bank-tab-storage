package com.banktabstorage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.runelite.client.util.Text;

final class BankTabStorageCodec
{
    private static final int MAX_TEXT_LENGTH = 5_000_000;
    private static final int MAX_LAYOUT_INDEX = 9999;
    private final Gson gson;

    BankTabStorageCodec(Gson gson)
    {
        this.gson = gson.newBuilder().setPrettyPrinting().create();
    }

    String encode(List<StoredBankTag> tabs)
    {
        return gson.toJson(new BankTabStorageData(tabs));
    }

    List<StoredBankTag> decode(String text)
    {
        if (text == null || text.trim().isEmpty())
        {
            throw new IllegalArgumentException("The clipboard is empty");
        }
        String normalized = text.trim();
        if (normalized.length() > MAX_TEXT_LENGTH)
        {
            throw new IllegalArgumentException("The clipboard data is too large");
        }
        if (normalized.startsWith("banktags,"))
        {
            return List.of(decodeRuneLiteTab(normalized));
        }
        try
        {
            JsonObject root = gson.fromJson(normalized, JsonObject.class);
            if (root == null)
            {
                throw new IllegalArgumentException("The clipboard does not contain Bank Tab Storage data");
            }
            BankTabStorageData data = gson.fromJson(root, BankTabStorageData.class);
            return data.validatedTabs();
        }
        catch (IllegalArgumentException exception)
        {
            throw exception;
        }
        catch (RuntimeException exception)
        {
            throw new IllegalArgumentException("The clipboard does not contain valid Bank Tab Storage data", exception);
        }
    }

    private StoredBankTag decodeRuneLiteTab(String text)
    {
        List<String> values = Text.fromCSV(text);
        if (values.size() < 4 || !"banktags".equals(values.get(0)) || !"1".equals(values.get(1)))
        {
            throw new IllegalArgumentException("The RuneLite Bank Tags export is invalid");
        }
        String name = StoredBankTag.normalizeName(values.get(2));
        int iconItemId = parseInt(values.get(3), "icon item ID");
        Set<Integer> items = new LinkedHashSet<>();
        List<Integer> layout = null;
        int index = 4;
        while (index < values.size() && !"layout".equals(values.get(index)))
        {
            items.add(parseInt(values.get(index), "item ID"));
            index++;
        }
        if (index < values.size())
        {
            index++;
            if ((values.size() - index) % 2 != 0)
            {
                throw new IllegalArgumentException("The RuneLite Bank Tags layout is invalid");
            }
            layout = new ArrayList<>();
            while (index < values.size())
            {
                int layoutIndex = parseInt(values.get(index++), "layout position");
                int itemId = parseInt(values.get(index++), "layout item ID");
                if (layoutIndex < 0 || layoutIndex > MAX_LAYOUT_INDEX || itemId < 0)
                {
                    throw new IllegalArgumentException("The RuneLite Bank Tags layout is invalid");
                }
                while (layout.size() <= layoutIndex)
                {
                    layout.add(-1);
                }
                layout.set(layoutIndex, itemId);
                items.add(itemId);
            }
        }
        return new StoredBankTag(name, iconItemId, new ArrayList<>(items), layout, false, false).validatedCopy();
    }

    private int parseInt(String value, String label)
    {
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException exception)
        {
            throw new IllegalArgumentException("The RuneLite Bank Tags " + label + " is invalid", exception);
        }
    }
}
