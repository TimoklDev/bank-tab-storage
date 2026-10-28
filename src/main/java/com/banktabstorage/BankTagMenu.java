package com.banktabstorage;

import java.util.Collection;
import java.util.function.Consumer;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.util.Text;

final class BankTagMenu
{
    private BankTagMenu()
    {
    }

    static String tabName(String option, int widgetId, String target, Collection<String> visibleNames)
    {
        if (!"View tag tab".equals(option) || widgetId >>> 16 != InterfaceID.BANKMAIN || target == null)
        {
            return null;
        }
        String plain = Text.removeTags(target).trim();
        if (plain.isEmpty())
        {
            return null;
        }
        String name = StoredBankTag.normalizeName(plain);
        return !"tagtabs".equals(name) && visibleNames.contains(name) ? name : null;
    }

    static void addEntries(Menu menu, MenuEntry original, Consumer<Boolean> action)
    {
        menu.createMenuEntry(1)
            .setOption("Hide tab")
            .setTarget(original.getTarget())
            .setType(MenuAction.RUNELITE_LOW_PRIORITY)
            .setParam0(original.getParam0())
            .setParam1(original.getParam1())
            .onClick(entry -> action.accept(true));
        menu.createMenuEntry(1)
            .setOption("Import to Bank Tab Manager")
            .setTarget(original.getTarget())
            .setType(MenuAction.RUNELITE_LOW_PRIORITY)
            .setParam0(original.getParam0())
            .setParam1(original.getParam1())
            .onClick(entry -> action.accept(false));
    }
}
