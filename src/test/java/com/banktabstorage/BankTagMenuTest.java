package com.banktabstorage;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Proxy;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.gameval.InterfaceID;
import org.junit.Assert;
import org.junit.Test;

public class BankTagMenuTest
{
    @Test
    public void recognizesARealColoredBankTagTab()
    {
        Assert.assertEquals("doom", BankTagMenu.tabName("View tag tab", InterfaceID.Bankmain.ITEMS_CONTAINER,
            "<col=ff9040>Doom</col>", List.of("doom")));
    }

    @Test
    public void ignoresNormalBankTabsItemsAndTheSpecialTabList()
    {
        Assert.assertNull(BankTagMenu.tabName("View tab", InterfaceID.Bankmain.ITEMS_CONTAINER,
            "doom", List.of("doom")));
        Assert.assertNull(BankTagMenu.tabName("View tag tab", 0, "doom", List.of("doom")));
        Assert.assertNull(BankTagMenu.tabName("View tag tab", InterfaceID.Bankmain.ITEMS_CONTAINER,
            "unrelated", List.of("doom")));
        Assert.assertNull(BankTagMenu.tabName("View tag tab", InterfaceID.Bankmain.ITEMS_CONTAINER,
            "tagtabs", List.of("tagtabs")));
    }

    @Test
    public void addsRightClickActionsWithoutChangingTheDefaultTabEntry()
    {
        List<MenuEntry> entries = new ArrayList<>();
        MenuEntry original = entry().setOption("View tag tab").setTarget("doom")
            .setType(MenuAction.CC_OP).setParam0(6).setParam1(InterfaceID.Bankmain.ITEMS_CONTAINER);
        entries.add(entry().setOption("Cancel"));
        entries.add(original);
        Menu menu = (Menu) Proxy.newProxyInstance(Menu.class.getClassLoader(), new Class<?>[]{Menu.class},
            (proxy, method, args) ->
            {
                if (method.getName().equals("createMenuEntry"))
                {
                    MenuEntry added = entry();
                    entries.add((int) args[0], added);
                    return added;
                }
                return null;
            });
        List<Boolean> actions = new ArrayList<>();

        BankTagMenu.addEntries(menu, original, actions::add);

        Assert.assertTrue(actions.isEmpty());
        Assert.assertSame(original, entries.get(entries.size() - 1));
        Assert.assertEquals(MenuAction.CC_OP, original.getType());
        Assert.assertEquals("Import to Bank Tab Manager", entries.get(1).getOption());
        Assert.assertEquals("Hide tab", entries.get(2).getOption());
        Assert.assertEquals(MenuAction.RUNELITE_LOW_PRIORITY, entries.get(1).getType());
        Assert.assertEquals(MenuAction.RUNELITE_LOW_PRIORITY, entries.get(2).getType());
        entries.get(1).onClick().accept(entries.get(1));
        entries.get(2).onClick().accept(entries.get(2));
        Assert.assertEquals(List.of(false, true), actions);
    }

    private static MenuEntry entry()
    {
        Map<String, Object> properties = new HashMap<>();
        return (MenuEntry) Proxy.newProxyInstance(MenuEntry.class.getClassLoader(), new Class<?>[]{MenuEntry.class},
            (proxy, method, args) ->
            {
                String name = method.getName();
                if (name.startsWith("set"))
                {
                    properties.put("get" + name.substring(3), args[0]);
                    return proxy;
                }
                if (name.equals("onClick") && args != null && args.length == 1)
                {
                    properties.put("onClick", args[0]);
                    return proxy;
                }
                return properties.get(name);
            });
    }
}
