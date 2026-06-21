package com.pokemmo.launcher.ui.awt;

import com.pokemmo.launcher.Launcher;
import com.pokemmo.launcher.config.Config;
import com.pokemmo.launcher.ui.awt.MainFrame;

import javax.swing.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kyu <kyu@pokemmo.com>
 */
public class UpdaterSwingWorker extends SwingWorker<Void, Void>
{
	private final Launcher parent;
	private final MainFrame mainFrame;
	private final boolean repair, clean;
	private boolean success = true;

	public UpdaterSwingWorker(Launcher parent, MainFrame mainFrame, boolean repair, boolean clean)
	{
		this.parent = parent;
		this.mainFrame = mainFrame;
		this.repair = repair;
		this.clean = clean;
	}

	@Override
	protected Void doInBackground()
	{
		if(parent.isUpdating())
			return null;

		List<Throwable> failed = new ArrayList<>();

		try
		{
			if(clean)
			{
//				Files.walk(Path.of(parent.getPokemmoDir().getAbsolutePath()))
//						.sorted(Comparator.reverseOrder())
//						.takeWhile(p -> success)
//						.forEach(p -> {
//							try
//							{
//								Files.delete(p);
//							}
//							catch(IOException e)
//							{
//								e.printStackTrace();
//								failed.add(e);
//								success = false;
//							}
//						});

				System.out.println("WOULD CLEAN");

				SwingUtilities.invokeAndWait(() -> {
					parent.createPokemmoDir();
					parent.createSymlinkedDirectories();
				});
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			failed.add(e);
		}
		finally
		{
			if(success)
			{
				parent.doUpdate(repair);
				SwingUtilities.invokeLater(mainFrame::setCanStart);
			}
			else
				SwingUtilities.invokeLater(() -> mainFrame.showErrorWithStacktrace(Config.getString("error.dir_not_accessible", parent.getPokemmoDir().getAbsolutePath(), "REPAIR_FAILED"), "", failed.toArray(new Throwable[0]), () -> System.exit(Launcher.EXIT_CODE_IO_FAILURE)));
		}

		return null;
	}
}
