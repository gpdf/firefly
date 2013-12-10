package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.AreaStatCmd;
import edu.caltech.ipac.firefly.commands.CenterPlotOnQueryCmd;
import edu.caltech.ipac.firefly.commands.ChangeColorCmd;
import edu.caltech.ipac.firefly.commands.CropCmd;
import edu.caltech.ipac.firefly.commands.DataFilterInCmd;
import edu.caltech.ipac.firefly.commands.DataFilterOutCmd;
import edu.caltech.ipac.firefly.commands.DistanceToolCmd;
import edu.caltech.ipac.firefly.commands.ExpandCmd;
import edu.caltech.ipac.firefly.commands.FitsDownloadCmd;
import edu.caltech.ipac.firefly.commands.FitsHeaderCmd;
import edu.caltech.ipac.firefly.commands.FlipImageCmd;
import edu.caltech.ipac.firefly.commands.FlipLeftCmd;
import edu.caltech.ipac.firefly.commands.FlipRightCmd;
import edu.caltech.ipac.firefly.commands.GridCmd;
import edu.caltech.ipac.firefly.commands.ImageSelectCmd;
import edu.caltech.ipac.firefly.commands.IrsaCatalogCmd;
import edu.caltech.ipac.firefly.commands.LayerCmd;
import edu.caltech.ipac.firefly.commands.LoadDS9RegionCmd;
import edu.caltech.ipac.firefly.commands.LockImageCmd;
import edu.caltech.ipac.firefly.commands.MarkerToolCmd;
import edu.caltech.ipac.firefly.commands.NorthArrowCmd;
import edu.caltech.ipac.firefly.commands.QuickStretchCmd;
import edu.caltech.ipac.firefly.commands.RestoreCmd;
import edu.caltech.ipac.firefly.commands.RotateCmd;
import edu.caltech.ipac.firefly.commands.RotateNorthCmd;
import edu.caltech.ipac.firefly.commands.SelectAreaCmd;
import edu.caltech.ipac.firefly.commands.ShowColorOpsCmd;
import edu.caltech.ipac.firefly.commands.ZoomDownCmd;
import edu.caltech.ipac.firefly.commands.ZoomFillCmd;
import edu.caltech.ipac.firefly.commands.ZoomFitCmd;
import edu.caltech.ipac.firefly.commands.ZoomOriginalCmd;
import edu.caltech.ipac.firefly.commands.ZoomUpCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.MenuGenerator;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopoutControlsUI;
import edu.caltech.ipac.firefly.ui.PopoutWidget;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.util.PropFile;
import edu.caltech.ipac.firefly.util.WebAppProperties;
import edu.caltech.ipac.firefly.util.event.HasWebEventManager;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: May 19, 2008
 * Time: 2:35:43 PM
 */


/**
 * @author Trey Roby
 */
public class AllPlots implements HasWebEventManager {

    interface ColorTableFile extends PropFile { @Source("colorTable.prop") TextResource get(); }
    interface VisMenuBarFile extends PropFile { @Source("VisMenuBar.prop") TextResource get(); }
    interface ReadoutSideFile extends PropFile { @Source("ReadoutSideCmd.prop") TextResource get(); }

    public enum PopoutStatus {Enabled, Disabled}
    public enum WcsMatchMode {NorthAndCenter, ByUserPositionAndZoom}

    private static AllPlots _instance = null;
    private final NumberFormat _nf = NumberFormat.getFormat("#.#");
    private final WebEventManager _emMan = new WebEventManager();

    private final List<MiniPlotWidget> _allMpwList = new ArrayList<MiniPlotWidget>(10);
    private final List<PlotWidgetGroup> _groups = new ArrayList<PlotWidgetGroup>(5);
    private final List<PopoutWidget> _additionalWidgets = new ArrayList<PopoutWidget>(4);
    private final Map<String, GeneralCommand> _commandMap = new HashMap<String, GeneralCommand>(13);
    private final Map<PopoutWidget, PopoutStatus> _statusMap = new HashMap<PopoutWidget, PopoutStatus>(3);

    private WebMouseReadout _mouseReadout;
    private MenuItem _zoomLevelPopup = null;
    private Toolbar.CmdButton _toolbarLayerButton = null;
    private boolean _layerButtonAdded = false;

    private MiniPlotWidget _primarySel = null;
    private int toolPopLeftOffset= 0;
    private VisMenuBar menuBar;
    private PopoutWidget.ViewType expandUpdateViewType= PopoutWidget.ViewType.GRID;

    private PopoutWidget.ExpandUseType _defaultExpandUseType = PopoutWidget.ExpandUseType.GROUP;


    private boolean initialized = false;
    private MPWListener _pvListener;
    private boolean toolBarIsPopup= true;
    private boolean mouseReadoutWide= false;

    //-- wcs match parameters
    private boolean _matchWCS = false;
    private WorldPt wcsMatchCenterWP = null;
    private WcsMatchMode wcsMatchMode;
    private MiniPlotWidget mpwWcsPrim= null;




//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    private AllPlots() {
        WebEventManager.getAppEvManager().addListener(Name.SEARCH_RESULT_START, new TearDownListen());
        PopoutWidget.setExpandBehavior(new ExpandBehavior());

        Window.addResizeHandler(new ResizeHandler() {
            public void onResize(ResizeEvent event) { getVisMenuBar().updateLayout(); }
        });
    }


    public static AllPlots getInstance() {
        if (_instance == null) _instance = new AllPlots();
        return _instance;
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================



    public void setToolBarIsPopup(boolean toolBarIsPopup) {
        this.toolBarIsPopup= toolBarIsPopup;
    }
    public void setMouseReadoutWide(boolean wide) {
        this.mouseReadoutWide= wide;
        MiniPlotWidget.setDefaultThumbnailSize(wide ? 70 : 100);
    }




    public void disableWCSMatch() {
        if (_matchWCS) {
            wcsMatchCenterWP = null;
            mpwWcsPrim= null;
            _matchWCS = false;
            for(MiniPlotWidget mpw : getActiveGroupList(true))  mpw.getPlotView().clearWcsSync();
            fireEvent(new WebEvent<Boolean>(this, Name.WCS_SYNC_CHANGE, false));
        }
    }

    public WorldPt getWcsMatchCenter() { return wcsMatchCenterWP; }
    public boolean isWCSMatch() { return _matchWCS; }
    public boolean isWCSMatchIsNorth() { return wcsMatchMode==WcsMatchMode.NorthAndCenter; }

    public void enableWCSSync(WcsMatchMode matchMode) {
        WorldPt wp= null;
        WebPlot p= AllPlots.getInstance().getMiniPlotWidget().getCurrentPlot();
        if (matchMode==WcsMatchMode.NorthAndCenter) {
            if (p.containsAttributeKey(WebPlot.MOVING_TARGET_CTX_ATTR)) {
                wp= null;
            }
            else if (p.containsAttributeKey(WebPlot.FIXED_TARGET)) {
                Object o = p.getAttribute(WebPlot.FIXED_TARGET);
                if (o instanceof ActiveTarget.PosEntry) {
                    ActiveTarget.PosEntry entry = (ActiveTarget.PosEntry) o;
                    wp= entry.getPt();
                }
            }
            else {
                wp= p.getWorldCoords(new ImagePt(p.getImageDataWidth()/2,p.getImageDataHeight()/2));
            }
        }
        else {
            wp= p.getPlotView().findCurrentCenterWorldPoint();
        }
        enableWCSSync(wp, matchMode);

    }


    /**
     *
     * @param wp world point to sync to, required when doSync is true
     */
    public void enableWCSSync(WorldPt wp, WcsMatchMode matchMode) {
        if (_primarySel==null || _primarySel.getCurrentPlot()==null || !isExpanded()) return;
        if (_matchWCS && matchMode==wcsMatchMode) return;

        wcsMatchMode= matchMode;
        _matchWCS = true;
        wcsMatchCenterWP = wp;
        mpwWcsPrim= getMiniPlotWidget();
        WebPlot lockPrimary= mpwWcsPrim.getCurrentPlot();
        lockPrimary.getPlotView().getMiniPlotWidget().getGroup().setLockRelated(true);
        PopoutWidget expControl= getExpandedController();

        if (expControl.getPopoutControlsUI()!=null) {
            Dimension dim;
            boolean isGrid= expControl.isExpandedAsGrid();
            if (isGrid) {
                dim= expControl.getPopoutControlsUI().getGridDimension();
            }
            else {
                int w = mpwWcsPrim.getMovablePanel().getOffsetWidth();
                int h = mpwWcsPrim.getMovablePanel().getOffsetHeight();
                dim= new Dimension(w,h);
            }
            float zLevel = matchMode==WcsMatchMode.ByUserPositionAndZoom ?
                           lockPrimary.getZoomFact() :
                           ZoomUtil.getEstimatedFullZoomFactor(lockPrimary, dim, VisUtil.FullType.WIDTH_HEIGHT ,-1, 1);

            ZoomUtil.wcsSyncToLevel(zLevel, isGrid,wcsMatchMode==WcsMatchMode.NorthAndCenter);
        }
        fireEvent(new WebEvent<Boolean>(this, Name.WCS_SYNC_CHANGE, _matchWCS));
    }






    public void setToolPopLeftOffset(int offset) {
        this.toolPopLeftOffset= offset;
    }


    public PlotWidgetGroup getGroup(MiniPlotWidget mpw) {
        PlotWidgetGroup retval = null;
        if (_groups != null) {
            for (PlotWidgetGroup g : _groups) {
                if (g.contains(mpw)) {
                    retval = g;
                    break;
                }
            }
        }
        return retval;
    }


    public PlotWidgetGroup getGroup(String groupName) {
        PlotWidgetGroup retval = null;
        if (_groups != null && groupName != null) {
            for (PlotWidgetGroup g : _groups) {
                if (g.getName().equals(groupName)) {
                    retval = g;
                    break;
                }
            }
        }
        return retval;
    }


    public PlotWidgetGroup getActiveGroup() { return getGroup(_primarySel); }

    public void tearDownPlots() {

        fireTearDown();
        _primarySel = null;
        getVisMenuBar().teardown();
        List<PlotWidgetGroup> l = new ArrayList(_groups);
        for (PlotWidgetGroup g : l) g.autoTearDownPlots();

        _additionalWidgets.clear();
        _statusMap.clear();


        MiniPlotWidget newSelected = null;
        for (PlotWidgetGroup g : _groups) {
            for (MiniPlotWidget mpw : g) {
                if (mpw != null) {
                    newSelected = mpw;
                    break;
                }
            }
            if (newSelected != null) break;
        }

        if (newSelected != null) {
            setSelectedWidget(newSelected);
        } else {
            firePlotWidgetChange(null);
        }
        removeLayerButton();
    }


    public void setDefaultExpandUseType(PopoutWidget.ExpandUseType useType) {
        _defaultExpandUseType = useType;
    }

    public void setDefaultTiledTitle(String title) {
        PopoutControlsUI.setTitledTitle(title);
    }

    public PopoutWidget.ExpandUseType getDefaultExpandUseType() {
        return _defaultExpandUseType;
    }

    /**
     * remove the MiniPlotWidget. For efficiency does not choose a new selected widget. You should set the selected widget
     * after calling this method
     *
     * @param mpw the MiniPlotWidget to remove
     */
    void removeMiniPlotWidget(MiniPlotWidget mpw) {
        _allMpwList.remove(mpw);
        WebPlotView pv = mpw.getPlotView();
        if (pv != null) {
            pv.removeListener(_pvListener);
            _mouseReadout.removePlotView(pv);
        }
        PlotWidgetGroup group = mpw.getGroup();
        if (_groups.contains(group) && group.size() == 0) {
            _groups.remove(group);
        }
        fireRemoved(mpw);
    }

    public void delete(MiniPlotWidget delMpw) {

        PlotWidgetGroup group=delMpw.getGroup();
        boolean redoExpand= (isExpanded() && getExpandedController()==delMpw);
        MiniPlotWidget targetWidget= getMiniPlotWidget();
        group.removeMiniPlotWidget(delMpw);
        if (getMiniPlotWidget()==delMpw) {
            if (group.size()>0) {
                targetWidget= group.getAll().get(0);
                setSelectedWidget(targetWidget);
            }
            else {
                List<MiniPlotWidget> all= getAll();
                if (all.size()>0) {
                    targetWidget= getAll().get(0);
                    setSelectedWidget(targetWidget);
                }
                else {
                    _primarySel= null;
                    redoExpand= false;
                }
            }
        }
        if (redoExpand) targetWidget.forceExpand(true);
    }


    public Map<String, GeneralCommand> getCommandMap() { return _commandMap;}

    public GeneralCommand getCommand(String name) { return _commandMap.get(name); }



    /**
     * Get the WebPlotView object that show the plots
     * This will return null until the first plot request is completes
     *
     * @return the WebPlotView
     */
    public WebPlotView getPlotView() {
        Vis.assertInitialized();
        return _primarySel != null ? _primarySel.getPlotView() : null;
    }

    public MiniPlotWidget getMiniPlotWidget() { return _primarySel; }

    public boolean getGroupContainsSelection(PlotWidgetGroup group) {
        boolean retval = false;
        for (MiniPlotWidget mpw : group.getAllActive()) {
            if (mpw == _primarySel) {
                retval = true;
                break;
            }
        }
        return retval;
    }

    public void forceExpand() {
        if (getMiniPlotWidget()!=null) forceExpand(getMiniPlotWidget());
    }

    public void forceExpand(MiniPlotWidget mpw) {
        if (isExpanded()) {
            // maybe do something here
        }
        else {
            mpw.forceExpand();
        }
    }

    public void forceCollapse() {
        if (_primarySel!=null && isExpanded()) {
            MiniPlotWidget mpw= (MiniPlotWidget)getExpandedController();
            mpw.forceCollapse();
        }
    }

    public boolean isExpanded() {
        boolean retval= false;
        for(PopoutWidget pw : getAllPopouts()) {
            if (pw.isExpanded()) {
                retval= true;
                break;
            }
        }
        return retval;
    }

    public PopoutWidget getExpandedController() {
        PopoutWidget retval= null;
        for(PopoutWidget pw : getAllPopouts()) {
            if (pw.isPrimaryExpanded()) {
                retval= pw;
                break;
            }
        }
        return retval;
    }


    public void updateExpanded() {
        updateExpanded(expandUpdateViewType);
    }

    public void updateExpanded(PopoutWidget.ViewType viewType) {
        PopoutWidget primary= null;
        for(PopoutWidget pw : getAllPopouts()) {
            if (pw.isExpanded() && pw.isPrimaryExpanded()) {
                primary= pw;
                break;
            }
        }
        if (primary!=null) {
            primary.updateExpanded(viewType);
        }


    }

    public List<PopoutWidget> getAllPopouts() {
        List<PopoutWidget> retval =
                new ArrayList<PopoutWidget>(_allMpwList.size() + _additionalWidgets.size());
        for (MiniPlotWidget mpw : _allMpwList) {
            if (_statusMap.get(mpw) != PopoutStatus.Disabled) {
                retval.add(mpw);
            }
        }
        for (PopoutWidget popout : _additionalWidgets) {
            if (_statusMap.get(popout) != PopoutStatus.Disabled) {
                retval.add(popout);
            }
        }
        return retval;
    }

    public List<PopoutWidget> getAdditionalPopoutList() {
        List<PopoutWidget> retval =
                new ArrayList<PopoutWidget>(_additionalWidgets.size());
        for (PopoutWidget popout : _additionalWidgets) {
            if (_statusMap.get(popout) != PopoutStatus.Disabled) {
                retval.add(popout);
            }
        }
        return retval;
    }

    public List<MiniPlotWidget> getAll(boolean ignoreUninitialized) {
        List<MiniPlotWidget> retval = new ArrayList<MiniPlotWidget>(_allMpwList.size());
        for (MiniPlotWidget mpw : _allMpwList) {
            if (_statusMap.get(mpw) != PopoutStatus.Disabled) {
                if (ignoreUninitialized) {
                    if (mpw.isInit()) retval.add(mpw);
                }
                else {
                    retval.add(mpw);
                }
            }
        }
        return retval;
    }

    public List<MiniPlotWidget> getAll() { return getAll(false); }

    public WebMouseReadout getMouseReadout() { return _mouseReadout; }

    public List<MiniPlotWidget> getActiveList() { return getActiveGroupList(true); }

    public List<MiniPlotWidget> getActiveGroupList(boolean ignoreUninitialized) {
        PlotWidgetGroup group = getActiveGroup();
        List<MiniPlotWidget> retval;
        if (group == null) {
            retval = Collections.emptyList();
        } else {
            if (group.getLockRelated()) {
                retval = ignoreUninitialized ? group.getAllActive() : group.getAll();
            } else {
                AllPlots.getInstance().getActiveGroup();
                retval = Arrays.asList(AllPlots.getInstance().getMiniPlotWidget());
            }
        }
        return retval;
    }

    boolean isFullControl() { return _allMpwList.size()>0 ? _allMpwList.get(0).isFullControl() : false; }

    void updateUISelectedLook() {
        for (MiniPlotWidget mpw : _allMpwList) {
            mpw.updateUISelectedLook();
        }
    }

    public List<MiniPlotWidget> getGroupListWith(MiniPlotWidget mpw) {
        PlotWidgetGroup group = getGroup(mpw);
        List<MiniPlotWidget> retval;
        if (group == null) {
            retval = Collections.emptyList();
        } else if (!getGroupContainsSelection(group)) {
            retval = Collections.emptyList();
        } else {
            if (group.getLockRelated()) {
                retval = group.getAllActive();
            } else {
                retval = Arrays.asList(getMiniPlotWidget());
            }
        }
        return retval;
    }

    public void setStatus(PopoutWidget popout, PopoutStatus status) {
        if (status == PopoutStatus.Disabled) {
            _statusMap.put(popout, status);
            if (popout instanceof MiniPlotWidget && popout == _primarySel) {
                findNewSelected();
            }
        } else if (_statusMap.containsKey(popout)) {
            _statusMap.remove(popout);
        }
    }


    public void registerPopout(PopoutWidget popout) {
        if (!_additionalWidgets.contains(popout)) {
            _additionalWidgets.add(popout);
        }
    }

    public void deregisterPopout(PopoutWidget popout) {
        if (_additionalWidgets.contains(popout)) {
            _additionalWidgets.remove(popout);
        }
    }


    public void setSelectedWidget(final MiniPlotWidget mpw) {
        if (mpw != null && mpw.isInit()) {
            Vis.init(new Vis.InitComplete() {
                public void done() {
                    setSelectedWidget(mpw, false);
                }
            });
        }
    }

    public void setSelectedWidget(MiniPlotWidget mpw, boolean toggleShowMenuBar) {
        setSelectedWidget(mpw, false, toggleShowMenuBar);
    }

    public void setSelectedWidget(MiniPlotWidget mpw, boolean force, boolean toggleShowMenuBar) {
        VisMenuBar bar= getVisMenuBar();
        if (!force && mpw == _primarySel && bar.isVisible() && !mpw.isExpanded()) {
            if (bar.isVisible() && toggleShowMenuBar) toggleShowMenuBarPopup(mpw);
            return;
        }
        MiniPlotWidget old= _primarySel;
        _primarySel = mpw;
        _primarySel.saveCorners();
        updateUISelectedLook();


        bar.updateToolbarAlignment();
        if (toggleShowMenuBar) toggleShowMenuBarPopup(mpw);
        if (old!=_primarySel) firePlotWidgetChange(mpw);
        updateTitleFeedback();
        bar.updateVisibleWidgets();
        bar.updatePlotTitleToMenuBar();
    }


//====================================================================
//------------------- from HasWebEventManager interface
//====================================================================

    public WebEventManager getEventManager() { return _emMan; }
    public void addListener(WebEventListener l) { _emMan.addListener(l); }
    public void addListener(Name eventName, WebEventListener l) { _emMan.addListener(eventName, l); }
    public void removeListener(WebEventListener l) { _emMan.removeListener(l); }
    public void removeListener(Name eventName, WebEventListener l) { _emMan.removeListener(eventName, l); }
    public void fireEvent(WebEvent ev) { _emMan.fireEvent(ev); }


//======================================================================
//------------------ VisMenuBar Methods --------------------------------
//------------------ all are pass through to the VisMenuBar class ------
//======================================================================

    public void toggleShowMenuBarPopup(MiniPlotWidget mpw) { getVisMenuBar().toggleVisibleSpecial(mpw); }
    public void hideMenuBarPopup() { getVisMenuBar().hide(); }
    public void showMenuBarPopup() { getVisMenuBar().show(); }
    public void setMenuBarPopupPersistent(boolean p) { getVisMenuBar().setPersistent(p); }
    public void setMenuBarMouseOverHidesReadout(boolean hides) { getVisMenuBar().setMouseOverHidesReadout(hides); }
    public Widget getMenuBarInline() { return getVisMenuBar().getInlineLayout(); }
    public Widget getMenuBarInlineStatusLine() { return getVisMenuBar().getInlineStatusLine(); }
    public boolean isMenuBarPopup() { return getVisMenuBar().isPopup(); }
    public boolean isMenuBarVisible() {return getVisMenuBar().isVisible();}
    public Widget getMenuBarWidget() {return getVisMenuBar().getWidget();}


    public VisMenuBar getVisMenuBar() {
        if (menuBar==null)  {
            menuBar= new VisMenuBar(toolBarIsPopup);
            menuBar.setLeftOffset(toolPopLeftOffset);
        }
        return menuBar;
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    private void loadVisCommands(Map<String, GeneralCommand> commandMap) {

        WebAppProperties appProp = Application.getInstance().getProperties();
        appProp.load((PropFile) GWT.create(ColorTableFile.class));
        appProp.load((PropFile) GWT.create(VisMenuBarFile.class));
        appProp.load((PropFile) GWT.create(ReadoutSideFile.class));


        commandMap.put(GridCmd.CommandName,           new GridCmd());
        commandMap.put(ZoomDownCmd.CommandName,       new ZoomDownCmd());
        commandMap.put(ZoomUpCmd.CommandName,         new ZoomUpCmd());
        commandMap.put(ZoomOriginalCmd.CommandName,   new ZoomOriginalCmd());
        commandMap.put(ZoomFitCmd.CommandName,        new ZoomFitCmd());
        commandMap.put(ZoomFillCmd.CommandName,       new ZoomFillCmd());
        commandMap.put(RestoreCmd.CommandName,        new RestoreCmd());
        commandMap.put(ExpandCmd.CommandName,         new ExpandCmd());
        commandMap.put(SelectAreaCmd.CommandName,     new SelectAreaCmd());
        commandMap.put(FitsHeaderCmd.CommandName,     new FitsHeaderCmd());
        commandMap.put(FitsDownloadCmd.CommandName,   new FitsDownloadCmd());
        commandMap.put(ColorTable.CommandName,        new ColorTable());
        commandMap.put(Stretch.CommandName,           new Stretch());
        commandMap.put(LayerCmd.CommandName,          new LayerCmd());
        commandMap.put(RotateNorthCmd.CommandName,    new RotateNorthCmd());
        commandMap.put(RotateCmd.COMMAND_NAME,        new RotateCmd());
        commandMap.put(FlipImageCmd.COMMAND_NAME,     new FlipImageCmd());
        commandMap.put(DistanceToolCmd.CommandName,   new DistanceToolCmd());
        commandMap.put(CenterPlotOnQueryCmd.CommandName, new CenterPlotOnQueryCmd());
        commandMap.put(MarkerToolCmd.CommandName,     new MarkerToolCmd());
        commandMap.put(NorthArrowCmd.CommandName,     new NorthArrowCmd());
        commandMap.put(IrsaCatalogCmd.CommandName,    new IrsaCatalogCmd());
        commandMap.put(LoadDS9RegionCmd.COMMAND_NAME, new LoadDS9RegionCmd());

        commandMap.put(LockImageCmd.CommandName, new LockImageCmd());
        commandMap.put(ImageSelectCmd.CommandName, new ImageSelectCmd());

        commandMap.put(ShowColorOpsCmd.COMMAND_NAME, new ShowColorOpsCmd());

        commandMap.put("zscaleLinear", new QuickStretchCmd("zscaleLinear", RangeValues.STRETCH_LINEAR));
        commandMap.put("zscaleLog", new QuickStretchCmd("zscaleLog", RangeValues.STRETCH_LOG));
        commandMap.put("zscaleLogLog", new QuickStretchCmd("zscaleLogLog", RangeValues.STRETCH_LOGLOG));


        commandMap.put("stretch99", new QuickStretchCmd("stretch99", 99F));
        commandMap.put("stretch98", new QuickStretchCmd("stretch98", 98F));
        commandMap.put("stretch97", new QuickStretchCmd("stretch97", 97F));
        commandMap.put("stretch95", new QuickStretchCmd("stretch95", 95F));
        commandMap.put("stretch90", new QuickStretchCmd("stretch90", 90F));
        commandMap.put("stretch85", new QuickStretchCmd("stretch85", 85F));
        commandMap.put("stretch85", new QuickStretchCmd("stretchSigma", -2F, 10F, RangeValues.SIGMA));


        for (int i = 0; (i < 22); i++) {
            commandMap.put("colorTable" + i, new ChangeColorCmd("colorTable" + i, i));
        }
    }



    private void addLayerButton() {

        if (!_layerButtonAdded && Application.getInstance().getToolBar() != null) {
            LayerCmd cmd = (LayerCmd) _commandMap.get(LayerCmd.CommandName);
            if (cmd != null && _toolbarLayerButton == null) {
                _toolbarLayerButton = new Toolbar.CmdButton("Plot Layers", "Plot Layers",
                                                            "Control layers on the plot", cmd);
            }
            Application.getInstance().getToolBar().addButton(_toolbarLayerButton);
            _layerButtonAdded = true;
        }
    }

    private void removeLayerButton() {
        if (_layerButtonAdded && Application.getInstance().getToolBar() != null) {
            Application.getInstance().getToolBar().removeButton(_toolbarLayerButton.getName());
            _layerButtonAdded = false;
        }
    }


    private void findNewSelected() {
        if (_statusMap.containsKey(_primarySel) && _statusMap.get(_primarySel) == PopoutStatus.Disabled) {
            PlotWidgetGroup badGroup = _primarySel.getGroup();
            MiniPlotWidget firstChoice = null;
            MiniPlotWidget secondChoice = null;
            for (MiniPlotWidget mpw : getAll()) {
                if (mpw.getGroup().size() > 1 && mpw.getGroup() != badGroup) {
                    firstChoice = mpw;
                    break;
                }
                secondChoice = mpw;
            }
            setSelectedWidget(firstChoice != null ? firstChoice : secondChoice);
        }
    }


    private void updateTitleFeedback() {
        MiniPlotWidget mpwPrim = getMiniPlotWidget();

        for (MiniPlotWidget mpwItem : getAll()) {
            if (mpwItem.getPlotView() != null) {
                WebPlot p = mpwItem.getPlotView().getPrimaryPlot();
                String val;
                if (p != null && !mpwItem.getHideTitleDetail()) {
                    val = ZoomUtil.convertZoomToString(p.getZoomFact());

                    if (p.isRotated()) {
                        if (p.getRotationType() == PlotState.RotateType.NORTH) {
                            val += ", North";
                        } else {
                            val += ", " + _nf.format(p.getRotationAngle()) + "&#176;";
                        }
                    }

                    if (mpwItem == mpwPrim) {
                        if (_zoomLevelPopup != null) _zoomLevelPopup.setHTML(val);
                    }

                    String span = "&nbsp;&nbsp;&nbsp;&nbsp;<span style=\"color: #49a344;\">" + val;
                    if (mpwItem.getPlotView().isTaskWorking()) {
                        span += "&nbsp;&nbsp;&nbsp;<img style=\"width:10px;height:10px;\" src=\"" + GwtUtil.LOADING_ICON_URL + "\" >";
                    }
                    span += "</span>";
                    mpwItem.setSecondaryTitle(span);
                }
                else {
                    mpwItem.setSecondaryTitle("");
                }
            }
        }
        MiniPlotWidget.forceExpandedUIUpdate();
    }


    private void layout() {
        _mouseReadout = new WebMouseReadout(mouseReadoutWide);
        _mouseReadout.setDisplayMode(WebMouseReadout.DisplayMode.Group);
        if (mouseReadoutWide) {
            _mouseReadout.setDisplaySide(WebMouseReadout.Side.IRSA_LOGO);
        }
        else {
            _mouseReadout.setDisplaySide(WebMouseReadout.Side.Right);
        }


    }

//======================================================================
//------------------ Convenience package methods to fire events --------
//======================================================================

    void fireRemoved(MiniPlotWidget mpw) {
        fireEvent(new WebEvent<MiniPlotWidget>(this, Name.FITS_VIEWER_REMOVED, mpw));
    }

    void fireAdded(MiniPlotWidget mpw) {
        fireEvent(new WebEvent<MiniPlotWidget>(this, Name.FITS_VIEWER_ADDED, mpw));
    }

    void fireTearDown() {
        fireEvent(new WebEvent<AllPlots>(this, Name.ALL_FITS_VIEWERS_TEARDOWN, this));
    }

    void firePlotWidgetChange(MiniPlotWidget mpw) {
        fireEvent(new WebEvent<MiniPlotWidget>(this, Name.FITS_VIEWER_CHANGE, mpw));
    }

    public void fireAllPlotTasksCompleted() {
        fireEvent(new WebEvent<MiniPlotWidget>(this, Name.ALL_PLOT_TASKS_COMPLETE));
    }





//======================================================================
//------------------ Package Methods                    ----------------
//------------------ should only be called by           ----------------
//------------------ by the ServerTask classes in this  ----------------
//------------------ package that are called for plotting --------------
//======================================================================

    void hideMouseReadout() {
        _mouseReadout.hideMouseReadout();
        DeferredCommand.addPause();
        DeferredCommand.addPause();
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                _mouseReadout.hideMouseReadout();
            }
        });
    }

//======================================================================
//------------------ Package Methods              ----------------------
//------------------ should only be called by Vis ----------------------
//======================================================================

    void initAllPlots() {
        if (!initialized) {
            loadVisCommands(_commandMap);
            initialized = true;
            _pvListener = new MPWListener();
            layout();
        }
    }


//======================================================================
//------------------ Package Methods              ----------------------
//------------------ should only be called by MiniPlotWidget -----------
//======================================================================

    static void loadPrivateVisCommands(Map<String, GeneralCommand> commandMap,
                                       MiniPlotWidget mpw) {
        commandMap.put(CropCmd.CommandName,new CropCmd(mpw));
        commandMap.put(AreaStatCmd.CommandName, new AreaStatCmd(mpw));
        commandMap.put(DataFilterInCmd.CommandName, new DataFilterInCmd(mpw));
        commandMap.put(DataFilterOutCmd.CommandName, new DataFilterOutCmd(mpw));
        commandMap.put(FlipRightCmd.CommandName,new FlipRightCmd(mpw));
        commandMap.put(FlipLeftCmd.CommandName,new FlipLeftCmd(mpw));
    }


//======================================================================
//------------------ Package Methods              ----------------------
//------------------ should only be called by PlotWidgetGroup ----------
//======================================================================


    /**
     * add a new MiniPlotWidget.
     * don't call this method until MiniPlotWidget.getPlotView() will return a non-null value
     *
     * @param mpw the MiniPlotWidget to add
     */
    void addMiniPlotWidget(MiniPlotWidget mpw) {
        _allMpwList.add(mpw);
        _primarySel = mpw;

        if (!_groups.contains(mpw.getGroup())) _groups.add(mpw.getGroup());

        initAllPlots();

        WebPlotView pv = mpw.getPlotView();
        pv.addListener(_pvListener);
        _mouseReadout.addPlotView(pv);
        fireAdded(mpw);
        getVisMenuBar().updateVisibleWidgets();
    }


//======================================================================
//------------------ Inner Classes -------------------------------------
//======================================================================

    static class ColorTable extends MenuGenerator.MenuBarCmd {
        public static final String CommandName= "colorTable";
        public ColorTable() { super(CommandName); }

        @Override
        protected Image createCmdImage() {
            VisIconCreator ic= VisIconCreator.Creator.getInstance();
            String iStr= this.getIconProperty();
            if (iStr!=null && iStr.equals("colorTable.Icon"))  {
                return new Image(ic.getColorTable());
            }
            return null;
        }
    }

    static class Stretch extends MenuGenerator.MenuBarCmd {
        public static final String CommandName= "stretchQuick";
        public Stretch() { super(CommandName); }

        @Override
        protected Image createCmdImage() {
            VisIconCreator ic= VisIconCreator.Creator.getInstance();
            String iStr= this.getIconProperty();
            if (iStr!=null && iStr.equals("stretchQuick.Icon"))  {
                return new Image(ic.getStretchQuick());
            }
            return null;
        }
    }

    private class MPWListener implements WebEventListener {

        public void eventNotify(WebEvent ev) {
            Name n = ev.getName();
            if (n == Name.REPLOT) {
                ReplotDetails details = (ReplotDetails) ev.getData();
                if (details.getReplotReason() == ReplotDetails.Reason.IMAGE_RELOADED ||
                        details.getReplotReason() == ReplotDetails.Reason.ZOOM) {
                    updateTitleFeedback();
                }
                addLayerButton();
            } else if (n == Name.PLOT_ADDED || n == Name.PLOT_REMOVED || n == Name.PRIMARY_PLOT_CHANGE) {
                updateTitleFeedback();
            } else if (n == Name.PLOT_TASK_WORKING || n == Name.PLOT_TASK_COMPLETE) {
                updateTitleFeedback();
            }
            _emMan.fireEvent(ev);
        }
    }


    private class TearDownListen implements WebEventListener {
        public void eventNotify(WebEvent ev) { tearDownPlots(); }
    }


}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
 * HOWEVER USED.
 *
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 *
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
 * OF THE SOFTWARE.
 */

