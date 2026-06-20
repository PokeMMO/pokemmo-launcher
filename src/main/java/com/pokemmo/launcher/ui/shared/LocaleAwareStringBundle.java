package com.pokemmo.launcher.ui.shared;

/**
 * Stores a locale key and its format parameters for deferred re-resolution.
 *
 * @author Kyu
 */
public class LocaleAwareStringBundle
{
    private final String key;
    private final Object[] params;

    public LocaleAwareStringBundle(String key, Object... params)
    {
        this.key = key;
        this.params = params;
    }

    public String getKey()
    {
        return key;
    }

    public Object[] getParams()
    {
        return params;
    }
}
