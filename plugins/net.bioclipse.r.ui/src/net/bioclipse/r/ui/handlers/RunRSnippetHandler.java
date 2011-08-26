package net.bioclipse.r.ui.handlers;

import net.bioclipse.r.business.Activator;
import net.bioclipse.r.business.IRBusinessManager;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler to execute a Text Selection in R
 *
 * @author ola
 *
 */
public class RunRSnippetHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (!(selection instanceof ITextSelection)) return null;

		ITextSelection textsel = (ITextSelection) selection;
		System.out.println("You selected text: \n" + textsel.getText());

		IRBusinessManager r = Activator.getDefault().getJavaRBusinessManager();
		r.evalSnippet(textsel.getText());

		//We are done
		return null;
	}

}