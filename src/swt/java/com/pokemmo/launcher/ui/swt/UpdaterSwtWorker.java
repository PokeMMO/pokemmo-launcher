package com.pokemmo.launcher.ui.swt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.pokemmo.launcher.Launcher;
import com.pokemmo.launcher.config.Config;
import com.pokemmo.launcher.ui.LauncherUI;

/**
 * SWT replacement for {@link UpdaterSwingWorker}. Uses {@link CompletableFuture#runAsync}
 * instead of {@code SwingWorker}.
 * <p>
 * All UI interactions go through the {@link LauncherUI} interface, which is
 * thread-safe by contract.
 *
 * @author Kyu
 */
public class UpdaterSwtWorker
{
    private final Launcher parent;
    private final LauncherUI launcherUI;
    private final boolean repair;
    private final boolean clean;
    private boolean success = true;

    public UpdaterSwtWorker(Launcher parent, LauncherUI launcherUI, boolean repair, boolean clean)
    {
        this.parent = parent;
        this.launcherUI = launcherUI;
        this.repair = repair;
        this.clean = clean;
    }

    public void execute()
    {
        CompletableFuture.runAsync(() ->
        {
            if (parent.isUpdating())
                return;

            List<Throwable> failed = new ArrayList<>();

            try
            {
                if (clean)
                {
//                    Files.walk(Path.of(parent.getPokemmoDir().getAbsolutePath()))
//                            .sorted(Comparator.reverseOrder())
//                            .takeWhile(p -> success)
//                            .forEach(p ->
//                            {
//                                try
//                                {
//                                    Files.delete(p);
//                                }
//                                catch (IOException e)
//                                {
//                                    e.printStackTrace();
//                                    failed.add(e);
//                                    success = false;
//                                }
//                            });

					System.out.println("WOULD CLEAN");

                    // syncExec replaces SwingUtilities.invokeAndWait;
                    // SWTException (unchecked) replaces InterruptedException / InvocationTargetException
                    org.eclipse.swt.widgets.Display.getDefault().syncExec(() ->
                    {
                        parent.createPokemmoDir();
                        parent.createSymlinkedDirectories();
                    });
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                failed.add(e);
            }
            finally
            {
                if (success)
                {
                    parent.doUpdate(repair);
                    launcherUI.setCanStart();
                }
                else
                {
                    launcherUI.showErrorWithStacktrace(
                            Config.getString("error.dir_not_accessible",
                                    parent.getPokemmoDir().getAbsolutePath(), "REPAIR_FAILED"),
                            "",
                            failed.toArray(new Throwable[0]),
                            () -> System.exit(Launcher.EXIT_CODE_IO_FAILURE));
                }
            }
        }).exceptionally(throwable ->
        {
            launcherUI.showErrorWithStacktrace("Error", "Update Error", throwable, null);
            return null;
        });
    }
}
