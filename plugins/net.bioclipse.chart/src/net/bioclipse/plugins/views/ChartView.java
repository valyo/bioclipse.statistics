/* ***************************************************************************
 * Copyright (c) 2008 Bioclipse Project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *****************************************************************************/

package net.bioclipse.plugins.views;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

import net.bioclipse.chart.ChartUtils;
import net.bioclipse.chart.ScatterPlotRenderer;
import net.bioclipse.chart.events.CellChangeListener;
import net.bioclipse.chart.events.CellChangedEvent;
import net.bioclipse.chart.events.CellData;
import net.bioclipse.chart.events.CellSelection;
import net.bioclipse.model.ChartAction;
import net.bioclipse.model.ChartActionFactory;
import net.bioclipse.model.ChartConstants;
import net.bioclipse.model.ChartDescriptor;
import net.bioclipse.model.ChartEventType;
import net.bioclipse.model.ChartModelEvent;
import net.bioclipse.model.ChartModelListener;
import net.bioclipse.model.ChartSelection;
import net.bioclipse.model.JFreeChartActionFactory;
import net.bioclipse.model.JFreeChartTab;
import net.bioclipse.model.ScatterPlotMouseHandler;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.plot.XYPlot;



/**
 * ChartView displays different charts generated with ChartUtils
 * @see ChartUtils
 */

public class ChartView extends ViewPart implements ISelectionListener, ISelectionProvider, ChartModelListener, CellChangeListener {
	private ChartAction saveImageActionSVG,saveImageActionPNG,saveImageActionJPG;
	private Composite parent;
	private List<ISelectionChangedListener> selectionListeners;
	private ChartSelection selection;
	private static final Logger logger = Logger.getLogger(ChartView.class);
	private Frame frame;
	private static final boolean IS_MACOS = System.getProperty("os.name").contains("Mac");
	private CTabFolder tabFolder;
	private ScatterPlotMouseHandler pmh;
	private ChartActionFactory factory;
	
	/**
	 * The constructor.
	 */
	public ChartView() {
		super();
		selectionListeners = new ArrayList<ISelectionChangedListener>();
		pmh = new ScatterPlotMouseHandler();
		factory = new JFreeChartActionFactory();
		ChartUtils.registerCellChangeListener(this);
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		logger.debug("Creating ChartView Part");
		
		ChartUtils.addListener(this);
		
		this.parent = parent;
		tabFolder = new CTabFolder(parent, SWT.TOP);
		tabFolder.setSimple(false);
		
		tabFolder.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent arg0) {
				JFreeChartTab item = (JFreeChartTab) arg0.item;
				JFreeChart selectedChart = item.getChart();
				ChartUtils.setActiveChart(selectedChart);
			}
			
		});
		
		tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter(){

			@Override
			public void close(CTabFolderEvent event) {
				super.close(event);
				JFreeChartTab tab = (JFreeChartTab) event.item;
				//Remove tab from model
				ChartUtils.remove(tab.getChart());
			}
			
		});
		
		getSite().setSelectionProvider(this);
		getSite().getPage().addSelectionListener(this);
		makeActions();
		hookContextMenu();
		contributeToActionBars();
		
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				ChartView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(parent);
		parent.setMenu(menu);
		getSite().registerContextMenu(menuMgr, this);
	}
	
	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		addActions(manager);
	}

	private void fillContextMenu(IMenuManager manager) {
		addActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		addActions(manager);
	}
	
	private void addActions( IContributionManager manager )
	{
		manager.add(saveImageActionSVG);
		manager.add(saveImageActionPNG);
		manager.add(saveImageActionJPG);
		manager.add(new Separator());
	}

	private void makeActions() {
		
		saveImageActionSVG = factory.createExportSvgAction();
		saveImageActionPNG = factory.createExportPngAction();
		saveImageActionJPG = factory.createExtportJpegAction();
		
		saveImageActionJPG.setEnabled(false);
		saveImageActionPNG.setEnabled(false);
		saveImageActionSVG.setEnabled(false);
		
		ChartUtils.addListener(saveImageActionSVG);
		ChartUtils.addListener(saveImageActionJPG);
		ChartUtils.addListener(saveImageActionPNG);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		parent.setFocus();
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) 
	{
		if( selection instanceof CellSelection){
			CellSelection cs = (CellSelection) selection;
			Object source = cs.getSource();

			Set<JFreeChart> keySet = ChartUtils.keySet();
			Iterator<JFreeChart> i = keySet.iterator();
			List<JFreeChart> matchingCharts = new ArrayList<JFreeChart>();

			//First get the set of charts that originates from CelSelection source and are scatter plots
			while( i.hasNext()){
				JFreeChart chart = i.next();
				ChartDescriptor chartDescriptor = ChartUtils.getChartDescriptor(chart);
				if( chartDescriptor.getSource() == source && chartDescriptor.getPlotType() == ChartConstants.SCATTER_PLOT){
					matchingCharts.add(chart);
				}
			}

			//Check if any points should be marked on the matching charts
			Iterator<JFreeChart> j = matchingCharts.iterator();
			while( j .hasNext() ){
				JFreeChart chart = j.next();
				ChartDescriptor chartDescriptor = ChartUtils.getChartDescriptor(chart);

				ScatterPlotRenderer renderer = (ScatterPlotRenderer) chart.getXYPlot().getRenderer();
				Iterator<CellData> cellIterator = cs.iterator();
				renderer.clearMarkedPoints();

				while( cellIterator.hasNext() )
				{
					CellData cd = cellIterator.next();
					if( cd.getColName().equals(chartDescriptor.getXLabel()) || cd.getColName().equals(chartDescriptor.getYLabel()) )
					{	
						renderer.addMarkedPoint(0, cd.getRowIndex());
						
					}
				}
				chart.plotChanged(new PlotChangeEvent(chart.getPlot()));
			}
		}
	}


	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		if(!selectionListeners.contains(listener))
		{
			selectionListeners.add(listener);
		}
	}

	public ISelection getSelection() {
		return selection;
	}

	public void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		if(selectionListeners.contains(listener))
			selectionListeners.remove(listener);
	}

	public void setSelection(ISelection selection) {
		
		this.getSite().getWorkbenchWindow().getWorkbench().getDisplay().asyncExec(new Runnable() {

			public void run() {
				ChartView.this.getSite().getPage().activate(ChartView.this);
			}
			
		});
		
		this.selection = (ChartSelection)selection;
		java.util.Iterator<ISelectionChangedListener> iter = selectionListeners.iterator();
		while( iter.hasNext() )
		{
			final ISelectionChangedListener listener = iter.next();
			final SelectionChangedEvent e = new SelectionChangedEvent(this, this.selection);
			//Does SWT stuff so this has to be called on SWT's thread
			this.getSite().getShell().getDisplay().asyncExec(new Runnable() {

				public void run() {
					listener.selectionChanged(e);
				}
				
			});
			
		}
	}

	/**
	 * Displays a chart in ChartView and sets up its mouse listener
	 * @param chart
	 */
	public void display( JFreeChart chart )
	{
		final ChartDescriptor cd = ChartUtils.getChartDescriptor(chart);
		
		
		JFreeChartTab chartTab = new JFreeChartTab(tabFolder, SWT.CLOSE);
		chartTab.setText(chart.getTitle().getText());
		chartTab.setChart(chart);
	
		Composite composite = new Composite(tabFolder, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		chartTab.setControl(composite);
		
		frame = SWT_AWT.new_Frame(composite);
	
		final ChartPanel chartPanel = new ChartPanel(chart);
		
		//Since methods are called on a java.awt.Frame it has to be called on the swing/awt thread 
		SwingUtilities.invokeLater(new Runnable()
		{
	
			public void run() {
				frame.removeAll();
				frame.add(chartPanel);
				frame.setVisible(true);
	
				if( cd.getPlotType() == ChartConstants.SCATTER_PLOT)
				{
					//Listens for mouseclicks on points
					XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
					plot.setRenderer(new ScatterPlotRenderer(false,true));
					
					if( ChartView.IS_MACOS )
					{
						frame.addMouseListener(pmh);
						frame.addMouseMotionListener(pmh);
					}
					else
					{
						chartPanel.addMouseListener(pmh);
						frame.addMouseMotionListener(pmh);
					}	
				}
			}
		});
		tabFolder.setSelection(chartTab);
		tabFolder.forceFocus();
		tabFolder.layout();
		ChartUtils.setActiveChart(chart);
		
		//Make sure actions are enabled when the chart has been created
		saveImageActionJPG.setEnabled(true);
		saveImageActionPNG.setEnabled(true);
		saveImageActionSVG.setEnabled(true);
	}
	
	/**
	 * Handles state changes in the model
	 */
	public void handleChartModelEvent(ChartModelEvent e) {
		if(e.getEventType() == ChartEventType.ACTIVE_CHART_CHANGED )
		{
			//Disable actions if no active chart exists
			if( ChartUtils.getActiveChart() == null )
			{
				saveImageActionJPG.setEnabled(false);
				saveImageActionPNG.setEnabled(false);
				saveImageActionSVG.setEnabled(false);
			}
		}
	}

	public void handleCellChangeEvent(CellChangedEvent e) {
		// TODO Auto-generated method stub
		
	}
}