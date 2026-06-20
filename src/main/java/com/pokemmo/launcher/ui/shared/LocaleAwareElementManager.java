package com.pokemmo.launcher.ui.shared;

import java.util.HashSet;
import java.util.Set;

/**
 * Singleton manager that tracks all locale-aware UI elements across both AWT and SWT UIs.
 * When the locale changes, all registered elements are updated.
 *
 * @author Kyu
 */
public class LocaleAwareElementManager
{
    public static final LocaleAwareElementManager instance = new LocaleAwareElementManager();

    private final Set<LocaleAwareInterface> active_elements = new HashSet<>();

    private LocaleAwareElementManager()
    {
    }

    public void addElement(LocaleAwareInterface element)
    {
        active_elements.add(element);
    }

    public void removeElement(LocaleAwareInterface element)
    {
        active_elements.remove(element);
    }

    public void updateElements()
    {
        active_elements.forEach(LocaleAwareInterface::updateLocale);
    }
}
