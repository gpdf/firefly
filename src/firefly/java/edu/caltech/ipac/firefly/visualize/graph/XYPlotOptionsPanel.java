/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.graph;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.gen2.table.client.SelectionGrid;
import com.google.gwt.gen2.table.event.client.RowSelectionEvent;
import com.google.gwt.gen2.table.event.client.RowSelectionHandler;
import com.google.gwt.gen2.table.event.client.TableEvent;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.data.form.DoubleFieldDef;
import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.*;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.input.SuggestBoxInputField;
import edu.caltech.ipac.firefly.ui.input.ValidationInputField;
import edu.caltech.ipac.firefly.ui.panels.CollapsiblePanel;
import edu.caltech.ipac.firefly.ui.table.BasicTable;
import edu.caltech.ipac.firefly.util.MinMax;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.WebProp;
import edu.caltech.ipac.util.expr.Expression;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.FieldDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author tatianag
 *         $Id: $
 */
public class XYPlotOptionsPanel extends Composite {
    private static WebClassProperties _prop= new WebClassProperties(XYPlotOptionsDialog.class);
    private final XYPlotBasicWidget _xyPlotWidget;
    private final XYPlotMeta _defaultMeta;

    private MinMaxPanel xMinMaxPanel;
    private MinMaxPanel yMinMaxPanel;
    private HTML xMinMaxPanelDesc;
    private HTML yMinMaxPanelDesc;
    //private HTML tableInfo;
    private SimpleInputField plotStyle;
    private CheckBox plotGrid;
    private CheckBox plotError;
    private CheckBox plotSpecificPoints;
    private CheckBox xLogScale;
    private CheckBox yLogScale;
    private InputField xColFld;
    private InputField yColFld;
    private Expression xColExpr;
    private Expression yColExpr;
    private InputField xNameFld;
    private InputField xUnitFld;
    private InputField yNameFld;
    private InputField yUnitFld;
    // density plot fields
    CollapsiblePanel densityPlotPanel;
    private InputField binning;
    private InputField xBinsFld;
    private InputField yBinsFld;
    private InputField shading;
    // aspect ratio fields
    private InputField xyRatioFld;
    private InputField stretchFld;

    private List<String> numericCols;
    //private SimpleInputField maxPoints;
    private boolean setupOK = true;

    private ShowColumnsDialog xColDialog;
    private ShowColumnsDialog yColDialog;

    ScrollPanel _mainPanel = new ScrollPanel();
    private static boolean suspendEvents = false;


    public XYPlotOptionsPanel(XYPlotBasicWidget widget) {
        _xyPlotWidget = widget;
        _defaultMeta = _xyPlotWidget.getPlotMeta().deepCopy();
        layout(widget.getPlotData());
        _xyPlotWidget.addListener(new XYPlotBasicWidget.NewDataListener() {
             public void newData(XYPlotData data) {
                 setup();
             }
        });
        this.initWidget(_mainPanel);
    }

    public void setVisible(boolean v) {
        if (v) {
            setup();
        }
        super.setVisible(v);
    }


    private void layout(final XYPlotData data) {

        // Plot Error
        plotError = GwtUtil.makeCheckBox("XYPlotOptionsDialog.plotError");
        plotError.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (plotError.getValue() && !plotError.isEnabled()) {
                    // should not happen
                } else {
                    XYPlotMeta meta = _xyPlotWidget.getPlotMeta();
                    meta.setPlotError(plotError.getValue());
                    _xyPlotWidget.updateMeta(meta, true); // preserve zoom
                }
            }
        });

        // Plot Specific Points
        plotSpecificPoints = GwtUtil.makeCheckBox("XYPlotOptionsDialog.plotSpecificPoints");
        plotSpecificPoints.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (plotSpecificPoints.getValue() && !plotSpecificPoints.isEnabled()) {
                    //should not happen
                } else {
                    XYPlotMeta meta = _xyPlotWidget.getPlotMeta();
                    meta.setPlotSpecificPoints(plotSpecificPoints.getValue());
                    _xyPlotWidget.updateMeta(meta, true); // preserve zoom
                }
            }
        });


        // Alternative Columns
        HTML colPanelDesc = GwtUtil.makeFaddedHelp(
                "For X and Y, enter a column or an expression<br>"+
                "ex. log(col); 100*col1/col2; col1-col2");

        ColExpressionOracle oracle = new ColExpressionOracle();
        FieldDef xColFD = FieldDefCreator.makeFieldDef("XYPlotOptionsDialog.x.col");
        xColFld = new ValidationInputField(new SuggestBoxInputField(xColFD, oracle));
        FieldDef yColFD = FieldDefCreator.makeFieldDef("XYPlotOptionsDialog.y.col");
        yColFld = new ValidationInputField(new SuggestBoxInputField(yColFD, oracle));

        // column selection
        Widget xColSelection = GwtUtil.makeLinkButton("Cols", "Select X column", new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (xColDialog == null) xColDialog = new ShowColumnsDialog("Choose X", "Set X", _xyPlotWidget.getColumns(), xColFld);
                xColDialog.show();
                //showChooseColumnPopup("Choose X", xColFld);
            }
        });
        Widget yColSelection = GwtUtil.makeLinkButton("Cols", "Select Y column", new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (yColDialog == null) yColDialog = new ShowColumnsDialog("Choose Y", "Set Y", _xyPlotWidget.getColumns(), yColFld);
                yColDialog.show();
                //showChooseColumnPopup("Choose Y", yColFld);
            }
        });

        FormBuilder.Config config = new FormBuilder.Config(FormBuilder.Config.Direction.VERTICAL,
                50, 0, HorizontalPanel.ALIGN_LEFT);
        xNameFld = FormBuilder.createField("XYPlotOptionsDialog.x.name");
        xUnitFld = FormBuilder.createField("XYPlotOptionsDialog.x.unit");
        Widget xNameUnit = FormBuilder.createPanel(config, xNameFld, xUnitFld);
        CollapsiblePanel xNameUnitCP = new CollapsiblePanel("X Label/Unit", xNameUnit, false);

        yNameFld = FormBuilder.createField("XYPlotOptionsDialog.y.name");
        yUnitFld = FormBuilder.createField("XYPlotOptionsDialog.y.unit");
        Widget yNameUnit = FormBuilder.createPanel(config, yNameFld, yUnitFld);
        CollapsiblePanel yNameUnitCP = new CollapsiblePanel("Y Label/Unit", yNameUnit, false);

        FlexTable colPanel = new FlexTable();
        DOM.setStyleAttribute(colPanel.getElement(), "padding", "5px");
        colPanel.setCellSpacing(8);

        colPanel.setHTML(0, 0, "X: ");
        colPanel.setWidget(0, 1, xColFld);
        colPanel.setWidget(0, 2, xColSelection);

        xLogScale = GwtUtil.makeCheckBox("XYPlotOptionsDialog.xLogScale");
        xLogScale.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (!suspendEvents) {
                    if (xLogScale.getValue() && !xLogScale.isEnabled()) {
                        // should not happen
                    } else {
                        XYPlotMeta meta = _xyPlotWidget.getPlotMeta();
                        meta.setXScale(xLogScale.getValue() ? XYPlotMeta.LOG_SCALE : XYPlotMeta.LINEAR_SCALE);
                        _xyPlotWidget.updateMeta(meta, true); // preserve zoom
                    }
                }
            }
        });
        colPanel.setWidget(0, 3, xLogScale);

        colPanel.setWidget(1, 1, xNameUnitCP);
        colPanel.setHTML(2, 0, "Y: ");
        colPanel.setWidget(2, 1, yColFld);
        colPanel.setWidget(2, 2, yColSelection);
        colPanel.setWidget(3, 1, yNameUnitCP);

        yLogScale = GwtUtil.makeCheckBox("XYPlotOptionsDialog.yLogScale");
        yLogScale.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (!suspendEvents) {
                    if (yLogScale.getValue() && !yLogScale.isEnabled()) {
                        // should not happen
                    } else {
                        XYPlotMeta meta = _xyPlotWidget.getPlotMeta();
                        meta.setYScale(yLogScale.getValue() ? XYPlotMeta.LOG_SCALE : XYPlotMeta.LINEAR_SCALE);
                        _xyPlotWidget.updateMeta(meta, true); // preserve zoom
                    }
                }
            }
        });
        colPanel.setWidget(2, 3, yLogScale);

        // Plot Style
        plotStyle = SimpleInputField.createByProp("XYPlotOptionsDialog.plotStyle");
        plotStyle.getField().addValueChangeHandler(new ValueChangeHandler<String>(){
            public void onValueChange(ValueChangeEvent<String> ev) {
                if (!suspendEvents) {
                    String value = plotStyle.getValue();
                    if (value != null) {
                        XYPlotMeta meta = _xyPlotWidget.getPlotMeta();
                        meta.setPlotStyle(XYPlotMeta.PlotStyle.getPlotStyle(value));
                        _xyPlotWidget.updateMeta(meta, true); // preserve zoom
                    }
                }
            }
        });

        // Gridlines
        plotGrid = GwtUtil.makeCheckBox("XYPlotOptionsDialog.grid");
        plotGrid.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (!suspendEvents) {
                    XYPlotMeta meta = _xyPlotWidget.getPlotMeta();
                    meta.setNoGrid(!plotGrid.getValue());
                    _xyPlotWidget.setGridlines();
                }
            }
        });

        // Y MIN and MAX
        xMinMaxPanelDesc = GwtUtil.makeFaddedHelp(getXMinMaxDescHTML(data == null ? null: data.getXDatasetMinMax()));
        yMinMaxPanelDesc = GwtUtil.makeFaddedHelp(getYMinMaxDescHTML(data == null ? null: data.getYDatasetMinMax()));

        FormBuilder.Config cX = new FormBuilder.Config(FormBuilder.Config.Direction.HORIZONTAL,
                                                      50, 5, HorizontalPanel.ALIGN_LEFT);

        xMinMaxPanel = new MinMaxPanel("XYPlotOptionsDialog.x.min", "XYPlotOptionsDialog.x.max", cX);

        xColFld.addValueChangeHandler(new ValueChangeHandler<String>(){

            public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
                suspendEvents = true;
                // reset scale to linear
                xLogScale.setValue(false);
                //xLogScale.setEnabled(false);
                xNameFld.reset();
                xUnitFld.reset();
                // clear xMinMaxPanel
                xMinMaxPanel.getMinField().reset();
                xMinMaxPanel.getMaxField().reset();
            }
        });

        FormBuilder.Config cY = new FormBuilder.Config(FormBuilder.Config.Direction.HORIZONTAL,
                                                      50, 5, HorizontalPanel.ALIGN_LEFT);

        yMinMaxPanel = new MinMaxPanel("XYPlotOptionsDialog.y.min", "XYPlotOptionsDialog.y.max", cY);

        yColFld.addValueChangeHandler(new ValueChangeHandler<String>(){

            public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
                suspendEvents = true;
                // reset scale to linear
                yLogScale.setValue(false);
                //yLogScale.setEnabled(false);
                yNameFld.reset();
                yUnitFld.reset();
                // clear xMinMaxPanel
                yMinMaxPanel.getMinField().reset();
                yMinMaxPanel.getMaxField().reset();
            }
        });


        //maxPoints = SimpleInputField.createByProp("XYPlotOptionsDialog.maxPoints");

        String bprop = _prop.makeBase("apply");
        String bname = WebProp.getName(bprop);
        String btip = WebProp.getTip(bprop);

        Button apply = new Button(bname, new ClickHandler() {
            public void onClick(ClickEvent ev) {
                if (xMinMaxPanel.validate() && yMinMaxPanel.validate() && validateColumns() &&
                        validateDensityPlotParams() && xyRatioFld.validate()) {

                    // current list of column names
                    List<TableDataView.Column> columnLst = _xyPlotWidget.getColumns();
                    List<String> cols = new ArrayList<String>(columnLst.size());
                    for (TableDataView.Column c : columnLst) {
                        cols.add(c.getName());
                    }

                    XYPlotMeta meta = _xyPlotWidget.getPlotMeta();

                    meta.setXScale(xLogScale.getValue() ? XYPlotMeta.LOG_SCALE : XYPlotMeta.LINEAR_SCALE);
                    meta.setYScale(yLogScale.getValue() ? XYPlotMeta.LOG_SCALE : XYPlotMeta.LINEAR_SCALE);
                    meta.setPlotStyle(XYPlotMeta.PlotStyle.getPlotStyle(plotStyle.getValue()));
                    meta.setNoGrid(!plotGrid.getValue());

                    meta.userMeta.setXLimits(getMinMaxValues(xMinMaxPanel));
                    meta.userMeta.setYLimits(getMinMaxValues(yMinMaxPanel));

                    // Columns
                    if (xColExpr != null) {
                        meta.userMeta.xColExpr = xColExpr;
                        meta.userMeta.setXCol(null);
                    } else {
                        String xCol = xColFld.getValue();
                        if (StringUtils.isEmpty(xCol) || xCol.equals(meta.findDefaultXColName(cols))) {
                            xCol = null;
                        }
                        meta.userMeta.setXCol(xCol);
                        meta.userMeta.xColExpr = null;
                    }

                    if (yColExpr != null) {
                        meta.userMeta.yColExpr = yColExpr;
                        meta.userMeta.setYCol(null);
                        nonDefaultYColumn(meta, true);
                    } else {
                        String yCol = yColFld.getValue();
                        String errorCol;
                        boolean defaultYCol = yCol.equals(meta.findDefaultYColName(cols));
                        if (StringUtils.isEmpty(yCol) || defaultYCol) {
                            yCol = null;
                            errorCol = null;
                            plotError.setEnabled(true);
                            plotSpecificPoints.setEnabled(true);
                        } else {
                            nonDefaultYColumn(meta, false);
                            errorCol = "_"; // no error column for non-default y column
                        }
                        meta.userMeta.setYCol(yCol);
                        meta.userMeta.yColExpr = null;
                        meta.userMeta.setErrorCol(errorCol);
                    }
                    if (!StringUtils.isEmpty(xNameFld.getValue())) {
                        meta.userMeta.xName = xNameFld.getValue();
                    } else { meta.userMeta.xName = null; }
                    if (!StringUtils.isEmpty(xUnitFld.getValue())) {
                        meta.userMeta.xUnit = xUnitFld.getValue();
                    } else { meta.userMeta.xUnit = null; }
                    if (!StringUtils.isEmpty(yNameFld.getValue())) {
                        meta.userMeta.yName = yNameFld.getValue();
                    } else { meta.userMeta.yName = null; }
                    if (!StringUtils.isEmpty(yUnitFld.getValue())) {
                        meta.userMeta.yUnit = yUnitFld.getValue();
                    } else { meta.userMeta.yUnit = null; }

                    // aspect ratio fields
                    meta.userMeta.stretchToFill = stretchFld.getValue().equals("fill");
                    if (StringUtils.isEmpty(xyRatioFld.getValue())) {
                        if (meta.userMeta.aspectRatio > 0) {
                            meta.userMeta.aspectRatio = -1;
                        }
                    } else {
                        meta.userMeta.aspectRatio = ((DoubleFieldDef)xyRatioFld.getFieldDef()).getDoubleValue(xyRatioFld.getValue());
                    }

                    // density plot parameters
                    if (binning.getValue().equals("user")) {
                        meta.userMeta.samplingXBins = Integer.parseInt(xBinsFld.getValue());
                        meta.userMeta.samplingYBins =  Integer.parseInt(yBinsFld.getValue());
                    } else {
                        meta.userMeta.samplingXBins = 0;
                        meta.userMeta.samplingYBins = 0;
                    }
                    meta.userMeta.logShading = shading.getValue().equals("log");

                    //meta.setMaxPoints(Integer.parseInt(maxPoints.getValue()));

                    try {
                        _xyPlotWidget.updateMeta(meta, false);
                    } catch (Exception e) {
                        PopupUtil.showError("Update failed", e.getMessage());
                    }
                }
            }
        });
        apply.setTitle(btip);


        Button cancel = new Button("Reset", new ClickHandler() {
            public void onClick(ClickEvent ev) {
                restoreDefault();
            }
        });
        cancel.setTitle("Restore default values");


        VerticalPanel vbox = new VerticalPanel();
        vbox.setSpacing(5);
        vbox.add(plotError);
        vbox.add(plotSpecificPoints);

        vbox.add(colPanelDesc);
        vbox.add(colPanel);

        HorizontalPanel hp = new HorizontalPanel();
        hp.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        hp.add(plotGrid);
        GwtUtil.setStyles(plotGrid, "paddingLeft", "15px", "paddingBottom", "5px");
        hp.add(plotStyle);
        GwtUtil.setStyle(plotStyle, "paddingLeft", "20px");
        vbox.add(hp);
        //vbox.add(plotStyle);
        //vbox.add(plotGrid);

        // aspect ratio
        FormBuilder.Config configAR = new FormBuilder.Config(FormBuilder.Config.Direction.VERTICAL,
                70, 0, HorizontalPanel.ALIGN_LEFT);
        xyRatioFld = FormBuilder.createField("XYPlotOptionsDialog.xyratio");
        stretchFld = FormBuilder.createField("XYPlotOptionsDialog.stretch");
        stretchFld.addValueChangeHandler(new ValueChangeHandler(){
            @Override
            public void onValueChange(ValueChangeEvent event) {
                if (stretchFld.getValue().equals("fill") && StringUtils.isEmpty(xyRatioFld.getValue())) {
                    xyRatioFld.setValue("1");
                }
            }
        });
        VerticalPanel arParams = new VerticalPanel();
        DOM.setStyleAttribute(arParams.getElement(), "paddingLeft", "10px");
        arParams.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        arParams.setSpacing(5);
        arParams.add(GwtUtil.makeFaddedHelp("Fix display aspect ratio by setting the field below.<br>"+
            "Leave it blank to use all available space."));
        arParams.add(FormBuilder.createPanel(configAR, xyRatioFld, stretchFld));
        //Widget aspectRatioPanel = new CollapsiblePanel("Aspect Ratio", arParams, false);
        //vbox.add(aspectRatioPanel);

        // density plot parameters
        binning = FormBuilder.createField("XYPlotOptionsDialog.binning");
        binning.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent event) {
                xBinsFld.reset();
                yBinsFld.reset();

                boolean enabled = binning.getValue().equals("user");
                xBinsFld.getFocusWidget().setEnabled(enabled);
                yBinsFld.getFocusWidget().setEnabled(enabled);
            }
        });
        shading = FormBuilder.createField("XYPlotOptionsDialog.shading");
        xBinsFld = FormBuilder.createField("XYPlotOptionsDialog.x.bins");
        yBinsFld = FormBuilder.createField("XYPlotOptionsDialog.y.bins");
        boolean enabled = binning.getValue().equals("user");
        xBinsFld.getFocusWidget().setEnabled(enabled);
        yBinsFld.getFocusWidget().setEnabled(enabled);
        VerticalPanel binningParams = new VerticalPanel();
        FormBuilder.Config configDP1 = new FormBuilder.Config(FormBuilder.Config.Direction.VERTICAL,
                50, 0, HorizontalPanel.ALIGN_LEFT);
        FormBuilder.Config configDP2 = new FormBuilder.Config(FormBuilder.Config.Direction.VERTICAL,
                110, 0, HorizontalPanel.ALIGN_LEFT);
        binningParams.add(FormBuilder.createPanel(configDP1,binning));
        binningParams.add(FormBuilder.createPanel(configDP2, xBinsFld, yBinsFld));
        binningParams.add(FormBuilder.createPanel(configDP1,shading));
        densityPlotPanel = new CollapsiblePanel("Binning Options", binningParams, false);
        vbox.add(densityPlotPanel);

        VerticalPanel vbox1 = new VerticalPanel();
        vbox1.add(xMinMaxPanelDesc);
        vbox1.add(xMinMaxPanel);
        vbox1.add(yMinMaxPanelDesc);
        vbox1.add(yMinMaxPanel);
        vbox1.add(arParams);
        //if (_xyPlotWidget instanceof XYPlotWidget) {
        //    tableInfo = GwtUtil.makeFaddedHelp(((XYPlotWidget)_xyPlotWidget).getTableInfo());
        //    vbox1.add(tableInfo);
        //    vbox1.add(maxPoints);
        //} else {
        //    vbox1.add(maxPoints);
        //    maxPoints.setVisible(false);
        //}

        CollapsiblePanel cpanel = new CollapsiblePanel("More Options", vbox1, false);

        vbox.add(cpanel);

        //vbox.add(addToDefault);
        Widget buttons = GwtUtil.leftRightAlign(new Widget[]{cancel}, new Widget[]{apply, HelpManager.makeHelpIcon("visualization.chartoptions")});
        buttons.addStyleName("base-dialog-buttons");
        vbox.add(buttons);

        _mainPanel.setWidget(vbox);
    }



    public boolean setupError() {
        return !setupOK;
    }


    private void restoreDefault() {
        XYPlotMeta currentMeta = _xyPlotWidget.getPlotMeta();
        XYPlotMeta newMeta =_defaultMeta.deepCopy();
        newMeta.setChartSize(currentMeta.xSize, currentMeta.ySize);
        _xyPlotWidget.updateMeta(newMeta, false); // don't preserve zoom selection
        setup();
    }

    /*
        Sync the form with current meta and data
     */
    private void setup() {

        suspendEvents = true;
        setupOK = true;
        xColDialog = null;
        yColDialog = null;
        XYPlotMeta meta = _xyPlotWidget.getPlotMeta();

        plotStyle.setValue(meta.plotStyle().key);
        plotError.setValue(meta.plotError());
        plotSpecificPoints.setValue(meta.plotSpecificPoints());
        plotGrid.setValue(!meta.noGrid());

        // set X and Y columns first, since other fields might be dependent on them
        setupXYColumnFields();

        setFldValue(xNameFld, meta.userMeta.xName);
        setFldValue(xUnitFld, meta.userMeta.xUnit);
        setFldValue(yNameFld, meta.userMeta.yName);
        setFldValue(yUnitFld, meta.userMeta.yUnit);


        XYPlotData data = _xyPlotWidget.getPlotData();
        if (data != null) {

            if (data.hasError() && plotError.isEnabled()) plotError.setVisible(true);
            else plotError.setVisible(false);

            if (data.hasSpecificPoints() && plotSpecificPoints.isEnabled() && data.getCurveData().size()>0) {
                String desc = data.getSpecificPoints().getDescription();
                if (StringUtils.isEmpty(desc)) { desc = "Specific Points"; }
                plotSpecificPoints.setHTML("Plot "+desc);
                plotSpecificPoints.setVisible(true);
            } else plotSpecificPoints.setVisible(false);

            if (data.getCurveData().size() == 0 || data.isSampled()) {
                // only specific points to plot
                plotStyle.setVisible(false);
            } else {
                plotStyle.setVisible(true);
            }

            MinMax minMax = data.getXMinMax();
            if (meta.getXScale() instanceof LogScale || (minMax.getMin()>0 && minMax.getMax()/minMax.getMin()>4)) {
                xLogScale.setEnabled(true);
                xLogScale.setVisible(true);
            } else {
                xLogScale.setEnabled(false);
                xLogScale.setVisible(false);
            }
            xLogScale.setValue(meta.getXScale() instanceof LogScale && xLogScale.isEnabled());
            //xLogScale.setValue(meta.getXScale() instanceof LogScale);

            // same for y
            minMax = plotError.getValue() ? data.getWithErrorMinMax() :data.getYMinMax();
            if (meta.getYScale() instanceof LogScale || (minMax.getMin()>0 && minMax.getMax()/minMax.getMin()>4)) {
                yLogScale.setEnabled(true);
                yLogScale.setVisible(true);
            } else {
                yLogScale.setEnabled(false);
                yLogScale.setVisible(false);
            }
            yLogScale.setValue(meta.getYScale() instanceof LogScale && yLogScale.isEnabled());
            //yLogScale.setValue(meta.getYScale() instanceof LogScale);

            // aspect ratio
            if (meta.userMeta != null && meta.userMeta.aspectRatio>0) {
                xyRatioFld.setValue(((DoubleFieldDef)xyRatioFld.getFieldDef()).format(meta.userMeta.aspectRatio));
            } else {
                xyRatioFld.reset();
            }
            stretchFld.setValue(meta.userMeta != null && meta.userMeta.stretchToFill ? "fill" : "fit");

            // density plot parameters
            if (data.isSampled()) {
                densityPlotPanel.setVisible(true);
                binning.setValue((meta.userMeta != null &&
                        meta.userMeta.samplingXBins > 0 && meta.userMeta.samplingYBins > 0) ?
                        "user" : "auto");
                shading.setValue((meta.userMeta != null && meta.userMeta.logShading) ? "log" : "lin");
                int xBins = data.getXSampleBins();
                if (xBins > 0) {
                    xBinsFld.setValue(Integer.toString(xBins));
                }
                int yBins = data.getYSampleBins();
                if (yBins > 0) {
                    yBinsFld.setValue(Integer.toString(yBins));
                }
            } else {
                densityPlotPanel.setVisible(false);
            }

            MinMax yMinMax = data.getYDatasetMinMax();
            DoubleFieldDef yminFD = (DoubleFieldDef)yMinMaxPanel.getMinField().getFieldDef();
            yminFD.setMinValue(Double.NEGATIVE_INFINITY);
            yminFD.setMaxValue(yMinMax.getMax());
            NumberFormat nf_y = NumberFormat.getFormat(MinMax.getFormatString(yMinMax, 3));
            yminFD.setErrMsg("Must be numerical value less than "+nf_y.format(yMinMax.getMax()));
            DoubleFieldDef ymaxFD = (DoubleFieldDef)yMinMaxPanel.getMaxField().getFieldDef();
            ymaxFD.setMinValue(yMinMax.getMin());
            ymaxFD.setMaxValue(Double.POSITIVE_INFINITY);
            ymaxFD.setErrMsg("Must be numerical value greater than "+nf_y.format(yMinMax.getMin()));


            MinMax xMinMax = data.getXDatasetMinMax();
            DoubleFieldDef xminFD = (DoubleFieldDef)xMinMaxPanel.getMinField().getFieldDef();
            xminFD.setMinValue(Double.NEGATIVE_INFINITY);
            xminFD.setMaxValue(xMinMax.getMax());
            NumberFormat nf_x = NumberFormat.getFormat(MinMax.getFormatString(xMinMax, 3));
            xminFD.setErrMsg("Must be numerical value less than "+nf_x.format(xMinMax.getMax()));
            DoubleFieldDef xmaxFD = (DoubleFieldDef)xMinMaxPanel.getMaxField().getFieldDef();
            xmaxFD.setMinValue(xMinMax.getMin());
            xmaxFD.setMaxValue(Double.POSITIVE_INFINITY);
            xmaxFD.setErrMsg("Must be numerical value greater than "+nf_x.format(xMinMax.getMin()));
        }
        MinMax xLimits = meta.userMeta.getXLimits();
        if (xLimits != null) {
            NumberFormat nf = NumberFormat.getFormat(MinMax.getFormatString(xLimits, 3));
            if (xLimits.getMin() != Double.NEGATIVE_INFINITY) {
                xMinMaxPanel.getMinField().setValue(nf.format(xLimits.getMin()));
            } else {
                xMinMaxPanel.getMinField().reset();
            }
            if (xLimits.getMax() != Double.POSITIVE_INFINITY) {
                xMinMaxPanel.getMaxField().setValue(nf.format(xLimits.getMax()));
            } else {
                xMinMaxPanel.getMaxField().reset();
            }
        } else {
            xMinMaxPanel.getMinField().reset();
            xMinMaxPanel.getMaxField().reset();
        }
        MinMax yLimits = meta.userMeta.getYLimits();
        if (yLimits != null) {
            NumberFormat nf = NumberFormat.getFormat(MinMax.getFormatString(yLimits, 3));
            if (yLimits.getMin() != Double.NEGATIVE_INFINITY) {
                yMinMaxPanel.getMinField().setValue(nf.format(yLimits.getMin()));
            } else {
                yMinMaxPanel.getMinField().reset();
            }
            if (yLimits.getMax() != Double.POSITIVE_INFINITY) {
                yMinMaxPanel.getMaxField().setValue(nf.format(yLimits.getMax()));
            } else {
                yMinMaxPanel.getMaxField().reset();
            }
        } else {
            yMinMaxPanel.getMinField().reset();
            yMinMaxPanel.getMaxField().reset();
        }
        xMinMaxPanelDesc.setHTML(getXMinMaxDescHTML(data == null ? null: data.getXDatasetMinMax()));
        yMinMaxPanelDesc.setHTML(getYMinMaxDescHTML(data == null ? null: data.getYDatasetMinMax()));

        //if (meta.getMaxPoints() > 0) {
        //    maxPoints.setValue(meta.getMaxPoints()+"");
        //}
        //if (_xyPlotWidget instanceof XYPlotWidget) {
        //    tableInfo.setHTML(((XYPlotWidget)_xyPlotWidget).getTableInfo());
        //}

        setupOK = (xMinMaxPanel.validate() && yMinMaxPanel.validate() &&
                validateColumns() && (data==null || !data.isSampled() || validateDensityPlotParams()) &&
                xyRatioFld.validate());
        suspendEvents = false;
    }

    private void nonDefaultYColumn(XYPlotMeta meta, boolean isExpression) {

        if (plotError.getValue()) {
            plotError.setValue(false);
            meta.setPlotError(false);
        }
        if (plotSpecificPoints.getValue() && !isExpression) {
            plotSpecificPoints.setValue(false);
            meta.setPlotSpecificPoints(false);
        }
        // error and specific points only make sense for default y column
        plotError.setEnabled(false);
        if (!isExpression) {
            plotSpecificPoints.setEnabled(false);
        }
        meta.userMeta.setErrorCol("_");  // no error column for non-default y column
    }

    private void setFldValue(InputField fld, String value) {
        if (StringUtils.isEmpty(value)) {
            fld.reset();
        } else {
            fld.setValue(value);
        }

    }

    private void setupXYColumnFields() {
        xColFld.reset();
        yColFld.reset();
        numericCols = new ArrayList<String>();

        if (_xyPlotWidget != null && _xyPlotWidget.getPlotData() != null) {
            List<TableDataView.Column> columnLst = _xyPlotWidget.getColumns();
            for (TableDataView.Column c : columnLst) {
                if (!c.getType().equals("char")) {
                    numericCols.add(c.getName());
                }
            }
            XYPlotData data = _xyPlotWidget.getPlotData();

            XYPlotMeta meta = _xyPlotWidget.getPlotMeta();
            if (meta.userMeta != null && meta.userMeta.xColExpr != null) {
                xColFld.setValue(meta.userMeta.xColExpr.getInput());
            } else {
                String xCol = data.getXCol();
                if (numericCols.indexOf(xCol) > -1) xColFld.setValue(xCol);
            }
            if (meta.userMeta != null && meta.userMeta.yColExpr != null) {
                yColFld.setValue(meta.userMeta.yColExpr.getInput());
            } else {
                String yCol = data.getYCol();
                if (numericCols.indexOf(yCol) > -1) yColFld.setValue(yCol);
            }
       }
    }



    private MinMax getMinMaxValues(MinMaxPanel panel) {

        DoubleFieldDef minFD = (DoubleFieldDef)panel.getMinField().getFieldDef();
        boolean isSet = false;

        String minStr = panel.getMinField().getValue();
        double min = Double.NEGATIVE_INFINITY;
        if (!StringUtils.isEmpty(minStr)) {
            min = minFD.getDoubleValue(minStr);
            isSet = true;
        }
        String maxStr = panel.getMaxField().getValue();
        double max = Double.POSITIVE_INFINITY;
        if (!StringUtils.isEmpty(maxStr)) {
            max = minFD.getDoubleValue(maxStr);
            isSet = true;
        }
        return isSet? new MinMax(min, max) : null;
    }

    private String getXMinMaxDescHTML(MinMax xMinMax) {
        String desc = "Remove out-of-bound points by defining a new X range.<br>";
        if (xMinMax != null) {
            NumberFormat nf_x = NumberFormat.getFormat(MinMax.getFormatString(xMinMax, 3));
            desc += "Dataset min X: "+nf_x.format(xMinMax.getMin())+", max X: "+nf_x.format(xMinMax.getMax());
        }
        return desc;
    }

    private String getYMinMaxDescHTML(MinMax yMinMax) {
        String desc = "Remove out-of-bound points by defining a new Y range.<br>";
        if (yMinMax != null) {
            NumberFormat nf_y = NumberFormat.getFormat(MinMax.getFormatString(yMinMax, 3));
            desc += "Dataset min Y: "+nf_y.format(yMinMax.getMin())+", max Y: "+nf_y.format(yMinMax.getMax());
        }
        return desc;
    }

    private boolean validateDensityPlotParams() {
        if (binning.getValue().equals("auto")) return true;
        String xBinsStr = xBinsFld.getValue();
        String yBinsStr = yBinsFld.getValue();
        if (StringUtils.isEmpty(xBinsStr)) {
            xBinsFld.forceInvalid("Empty value is not allowed for user defined binning");
            return false;
        }
        if (StringUtils.isEmpty(yBinsStr)) {
            yBinsFld.forceInvalid("Empty value is not allowed for user defined binning");
            return false;
        }
        if (!xBinsFld.validate() || !yBinsFld.validate()) {
            return false;
        }
        int xBins = Integer.parseInt(xBinsStr);
        int yBins = Integer.parseInt(yBinsStr);
        if (xBins*yBins > 10000) {
            yBinsFld.forceInvalid("Total number of bins can not exceed 10000.");
            return false;
        } else {
            return true;
        }
    }

    private boolean validateColumns() {
        boolean valid = xColFld.validate() && yColFld.validate();

        if (!valid) return false;

        String xCol = xColFld.getValue();
        if (!numericCols.contains(xCol)) {
            //check for expression
            xColExpr = validateAndSetExpression(xColFld);
            valid = (xColExpr != null);
        } else {
            xColExpr = null;
        }
        String yCol = yColFld.getValue();
        if (!numericCols.contains(yCol)) {
            // check for expression
            yColExpr = validateAndSetExpression(yColFld);
            valid = valid && (yColExpr != null);
        } else {
            yColExpr = null;
        }
        return valid;
    }

    private Expression validateAndSetExpression(InputField fld) {
        // check that the expression is parsable
        Expression expr = new Expression(fld.getValue(),numericCols);
        if (!expr.isValid()) {
            fld.forceInvalid(expr.getErrorMessage());
            return null;
        } else {
            return expr;
        }

    }



    public class ColExpressionOracle extends SuggestOracle {

        @Override
        public void requestSuggestions(Request request, Callback callback) {
            String text= request.getQuery();
            resolveCol(text, request, callback);
        }

        @Override
        public boolean isDisplayStringHTML() {
            return true;
        }
    }

    public class ColExpressionOracleCallback implements AsyncCallback<List<String>> {
        private String prior;
        private SuggestOracle.Callback cb;
        private SuggestOracle.Request request;
        private boolean completed= false;

        ColExpressionOracleCallback(String prior, SuggestOracle.Request request, SuggestOracle.Callback cb) {
            this.prior= prior;
            this.request= request;
            this.cb= cb;
        }

        public void onFailure(Throwable caught) {
            if (!completed) {
                List<ColSuggestion> sugList= new ArrayList<ColSuggestion>(0);
                SuggestOracle.Response response= new SuggestOracle.Response(sugList);
                cb.onSuggestionsReady(request,response);
            }
            completed= true;
        }

        public void onSuccess(List<String> result) {
            if (!completed) {
                List<ColSuggestion> sugList= new ArrayList<ColSuggestion>(result.size());
                for(String col : result)  sugList.add(new ColSuggestion(prior, col));
                SuggestOracle.Response response= new SuggestOracle.Response(sugList);
                cb.onSuggestionsReady(request,response);
            }
            completed= true;
        }
    }

    public class ColSuggestion implements SuggestOracle.Suggestion {
        private String prior;
        private String col;

        ColSuggestion(String prior, String col) {
            this.prior = prior;
            this.col= col;
        }

        public String getDisplayString() {
            return format(col);
        }

        public String getReplacementString() {
            return prior+col;
        }

        private String format(String s) { return "&nbsp;"+ s +"&nbsp;&nbsp;"; }

    }

    private void resolveCol(String text, SuggestOracle.Request request, SuggestOracle.Callback callback) {
        if (!StringUtils.isEmpty(text)) {
            String prior = "";
            String token = "";
            int priorIdx = -1;
            for (int i = text.length()-1; i>=0; i--) {
                Character c = text.charAt(i);
                if (!Character.isLetterOrDigit(c) && !c.equals('_')) {
                    priorIdx = i;
                    break;
                }
            }
            if (priorIdx > 0) prior = text.substring(0, priorIdx+1);
            if (priorIdx < text.length()) token = text.substring(priorIdx+1);

            AsyncCallback<List<String>> cb = new ColExpressionOracleCallback(prior, request, callback);
            List<String> matchingCols = getMatchingCols(token);
            if (matchingCols == null || matchingCols.size()==0) {
                matchingCols = numericCols;
            }
            cb.onSuccess(matchingCols);
        }
    }

    private List<String> getMatchingCols(String token) {
        if (_xyPlotWidget != null && numericCols != null && numericCols.size()>1) {
            if (!StringUtils.isEmpty(token)) {
                ArrayList<String> matchingCols = new ArrayList<String>();
                for (String c : numericCols) {
                    if (c.startsWith(token)) {
                        matchingCols.add(c);
                    }
                }
                return matchingCols;
            }
        }
        return null;
    }

    /** popup to choose columns
    private void showChooseColumnPopup(String title, final InputField fld) {
        BaseTableData defTD = new BaseTableData(new String[]{"Column", "Units", "Type", "Description"});
        for (TableDataView.Column c : _xyPlotWidget.getColumns()) {
            String units = c.getUnits();
            String type = c.getType();
            // numeric columns only
            if (StringUtils.isEmpty(type) || !c.getType().startsWith("c")) {
                defTD.addRow(new String[]{c.getName(), StringUtils.isEmpty(units)? "" : units, c.getType(), c.getShortDesc()});
            }
        }
        DataSet defDS = new DataSet(defTD);
        final BasicTable colTable = new BasicTable(defDS);
        colTable.setColumnWidth(0, 80);
        colTable.setColumnWidth(1, 50);
        colTable.setColumnWidth(2, 50);
        colTable.setColumnWidth(3, 100);
        colTable.addStyleName("expand-fully");
        InfoPanel infoPanel = new InfoPanel();
        infoPanel.setSize("320px", "190px");
        infoPanel.setWidget(colTable);

        final PopupPane popup = new PopupPane(title, infoPanel, false, true);
        popup.alignTo(fld, PopupPane.Align.TOP_LEFT_POPUP_BOTTOM, 20, -10);
        colTable.getDataTable().setSelectionEnabled(true);
        colTable.getDataTable().setSelectionPolicy(SelectionGrid.SelectionPolicy.ONE_ROW);
        colTable.getDataTable().addRowSelectionHandler(new RowSelectionHandler() {
            public void onRowSelection(RowSelectionEvent event) {
                Set<TableEvent.Row> srows = event.getSelectedRows(); // should be one row
                for (TableEvent.Row r : srows) {
                    int idx = r.getRowIndex();
                    TableData.Row row = colTable.getRows().get(idx);
                    final String col = String.valueOf(row.getValue(0));
                    String type = String.valueOf(row.getValue(2));
                    if (StringUtils.isEmpty(type) || !type.startsWith("c")) {
                        fld.setValue(col);
                        // can not get focus on text fields, if hiding this way
                        // popup.hide();
                    }
                    return;
                }

            }
        });

        popup.setDefaultSize(330,200);
        popup.show();
    }
     */

    private static class InfoPanel extends SimplePanel implements RequiresResize {
        public void onResize() {
            String height = this.getParent().getOffsetHeight()+"px";
            String width = this.getParent().getOffsetWidth()+"px";
            this.setSize(width, height);
            Widget w = this.getWidget();
            if (w instanceof BasicTable) {
                resizeTable((BasicTable) w, getParent().getOffsetWidth(),getParent().getOffsetHeight());
            }
        }

        private void resizeTable(BasicTable t, int width, int height) {
            int colCount= t.getDataTable().getColumnCount();
            int beforeLastColumnWidth = 0;
            int lastColWidth;
            if (colCount > 1) {
                for (int i=0; i<colCount-1;i++) {
                    beforeLastColumnWidth += t.getColumnWidth(i);
                }
                lastColWidth = width - beforeLastColumnWidth;
                if (lastColWidth > 50) {
                    t.setColumnWidth(colCount-1, lastColWidth-50);
                }
            }
            t.setSize(width+"px", height+"px");
        }
    }

    /*
    ShowColumnsDialog getColumnSelectionDialog(Widget parent, final InputField fld) {
        final ShowColumnsDialog dialog = new ShowColumnsDialog(parent, _xyPlotWidget.getColumns());
        dialog.getTable().getDataTable().setSelectionEnabled(true);
        dialog.getTable().getDataTable().setSelectionPolicy(SelectionGrid.SelectionPolicy.ONE_ROW);
        dialog.getTable().getDataTable().addRowSelectionHandler(new RowSelectionHandler() {
            public void onRowSelection(RowSelectionEvent event) {
                Set<TableEvent.Row> srows = event.getSelectedRows();
                for(TableEvent.Row r : srows) {
                    int idx = r.getRowIndex();
                    TableData.Row row = dialog.getTable().getRows().get(idx);
                    final String col = String.valueOf(row.getValue(0));
                    String type = String.valueOf(row.getValue(2));
                    if (!type.startsWith("c")) {
                        fld.setValue(col);
                    }
                    return;
                }

            }
        });

        dialog.setVisible(true, PopupPane.Align.BOTTOM_RIGHT, 2, 2);
        return dialog;
    }
    */

    private static class ShowColumnsDialog extends BaseDialog {

        private InputField _fld;
        private String _selectedCol;

        public ShowColumnsDialog(String title, String applyText, List<TableDataView.Column> cols, InputField fld) {
            super(fld, ButtonType.OK_CANCEL, PopupType.STANDARD, title, false, false, "visualization.xyplotViewer");
            _fld = fld;
            _selectedCol = null;
            Button b = this.getButton(BaseDialog.ButtonID.OK);
            b.setText(applyText);

            BaseTableData defTD = new BaseTableData(new String[]{"Column", "Units", "Type", "Description"});
            for (TableDataView.Column c : cols) {
                String units = c.getUnits();
                String type = c.getType();
                // numeric columns only
                if (StringUtils.isEmpty(type) || !c.getType().startsWith("c")) {
                    defTD.addRow(new String[]{c.getName(), StringUtils.isEmpty(units)? "" : units, c.getType(), c.getShortDesc()});
                }
            }

            DataSet defDS = new DataSet(defTD);
            final BasicTable table = new BasicTable(defDS);

            table.getDataTable().setSelectionEnabled(true);
            table.getDataTable().setSelectionPolicy(SelectionGrid.SelectionPolicy.ONE_ROW);
            table.getDataTable().addRowSelectionHandler(new RowSelectionHandler() {
                public void onRowSelection(RowSelectionEvent event) {
                    Set<TableEvent.Row> srows = event.getSelectedRows();
                    for(TableEvent.Row r : srows) {
                        int idx = r.getRowIndex();
                        TableData.Row row = table.getRows().get(idx);
                        final String col = String.valueOf(row.getValue(0));
                        _selectedCol = col;
                        return;
                    }

                }
            });

            table.setColumnWidth(0, 80);
            table.setColumnWidth(1, 50);
            table.setColumnWidth(2, 50);
            table.setColumnWidth(3, 250);
            table.addStyleName("expand-fully");
            InfoPanel infoPanel = new InfoPanel();
            infoPanel.setWidget(table);
            setWidget(infoPanel);
            setDefaultContentSize(470, 200);

        }

        @Override
        protected void inputComplete() {
            if (!StringUtils.isEmpty(_selectedCol))
            _fld.setValue(_selectedCol);
            setVisible(false);
        }

        @Override
        protected void inputCanceled() {
            setVisible(false);
        }

        @Override
        public void show() {
            setVisible(true, PopupPane.Align.TOP_LEFT_POPUP_BOTTOM, 20, -10);
        }
    }
}
