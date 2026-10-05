package com.banktabstorage;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.tabs.TabInterface;
import org.junit.Assert;
import org.junit.Test;

public class BankTabStoragePluginDependencyTest
{
    @Test
    public void injectsOnlyThePublicBankTagsService()
    {
        Set<Class<?>> injectedTypes = Arrays.stream(BankTabStoragePlugin.class.getDeclaredFields())
            .filter(field -> field.isAnnotationPresent(Inject.class))
            .map(Field::getType)
            .collect(Collectors.toSet());

        Assert.assertTrue(injectedTypes.contains(BankTagsService.class));
        Assert.assertFalse(injectedTypes.contains(BankTagsPlugin.class));
        Assert.assertFalse(injectedTypes.contains(TabInterface.class));
    }
}
