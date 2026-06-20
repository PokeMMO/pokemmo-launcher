package com.pokemmo.launcher.ui.shared;

/**
 * Interface for UI elements that need to update their text when the locale changes.
 *
 * @author Kyu
 */
public interface LocaleAwareInterface
{
    void setTextKey(String key, Object... params);

    void setToolTipKey(String tooltip, Object... params);

    void updateLocale();
}
