package com.pokemmo.launcher.ui.shared;

import java.util.function.BiConsumer;

/**
 * Static bridge that routes UI calls (e.g., error dialogs) from non-UI code
 * (like {@code Util.open()}) to whichever UI implementation is active.
 * <p>
 * Both {@code MainFrame} and {@code MainShell} set the callback at startup.
 *
 * @author Kyu
 */
public class UiBridge
{
    private static BiConsumer<String, String> errorCallback = (msg, title) ->
            System.err.println("UiBridge: no callback registered — " + title + ": " + msg);

    private UiBridge()
    {
    }

    public static void setErrorCallback(BiConsumer<String, String> callback)
    {
        errorCallback = callback;
    }

    public static void showError(String message, String windowTitle)
    {
        errorCallback.accept(message, windowTitle);
    }
}
