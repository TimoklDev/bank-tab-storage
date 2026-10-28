package com.banktabstorage;

final class ReplacementItem
{
    private final int id;
    private final String name;

    ReplacementItem(int id, String name)
    {
        this.id = id;
        this.name = name;
    }

    int getId()
    {
        return id;
    }

    String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
