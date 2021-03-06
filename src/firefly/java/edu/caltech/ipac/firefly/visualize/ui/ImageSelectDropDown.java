/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.ImageSelectDropDownCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.SimpleTargetPanel;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.visualize.PlotWidgetFactory;
import edu.caltech.ipac.util.dd.ValidationException;


/**
 * @author Trey Roby
 */
public class ImageSelectDropDown {
    private boolean showing= false;
    private final Widget mainPanel;
    private ImageSelectPanel imSelPanel;
    private ImageSelectPanel2 imSelPanel2;
    private BaseDialog.HideType hideType= BaseDialog.HideType.AFTER_COMPLETE;
    private SubmitKeyPressHandler keyPressHandler= new SubmitKeyPressHandler();
    private final boolean useNewPanel;
    private final ImageSelectPanelPlotter plotter;
    private boolean inProcess= false;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public ImageSelectDropDown(PlotWidgetFactory plotFactory, boolean useNewPanel, ImageSelectPanelPlotter plotter) {
        this.useNewPanel= useNewPanel;
        this.plotter= plotter;
        createContents(plotFactory);
        mainPanel= createContents(plotFactory);
    }


    private Widget createContents(PlotWidgetFactory plotFactory) {

        if (useNewPanel) {
            imSelPanel2= new ImageSelectPanel2(new DropDownComplete(), plotter);
        }
        else  {
            imSelPanel= new ImageSelectPanel(null,true,null,new DropDownComplete(),plotFactory);
        }
        HorizontalPanel buttons= new HorizontalPanel();
        buttons.addStyleName("base-dialog-buttons");
        buttons.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        GwtUtil.setStyle(buttons, "paddingRight", "80px");

        Button ok= new Button("Load");
        ok.addStyleName("highlight-text");
        ok.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent ev) { doOK(); }
        });

        buttons.add(ok);
        buttons.add(HelpManager.makeHelpIcon("basics.catalog"));

        VerticalPanel vp= new VerticalPanel();
        Widget content;
        if (useNewPanel) {
            content= GwtUtil.centerAlign(imSelPanel2.getMainPanel());
        }
        else {
            content= GwtUtil.centerAlign(imSelPanel.getMainPanel());
        }
        vp.add(content);
        vp.add(buttons);

        vp.setCellHorizontalAlignment(content, VerticalPanel.ALIGN_CENTER);
        vp.setSize("95%", "450px");
        vp.setSpacing(3);
        content.setSize("95%", "95%");
        content.addStyleName("component-background");

        if (useNewPanel) {
            addKeyPressToAll(imSelPanel2.getMainPanel());
        }
        else {
            addKeyPressToAll(imSelPanel.getMainPanel());
        }

        return vp;
    }


    private void addKeyPressToAll(Widget inWidget) {
        if (inWidget instanceof HasWidgets) {
            HasWidgets container= (HasWidgets)inWidget;
            for (Widget w : container) {
                if (w instanceof InputField) {
                    InputField f= (InputField)w;
                    if (f.getFocusWidget()!=null) {
                        f.getFocusWidget().addKeyPressHandler(keyPressHandler);
                    }
                }
                else if (w instanceof SimpleTargetPanel) {
                    SimpleTargetPanel sp= (SimpleTargetPanel)w;
                    if (sp.getInputField()!=null && sp.getInputField().getFocusWidget()!=null) {
                        sp.getInputField().getFocusWidget().addKeyPressHandler(keyPressHandler);
                    }
                }
                else {
                    addKeyPressToAll(w);
                }
            }
        }
    }




    private void doOK() {
        try {
            if (validateInput()) {
                inputComplete();
            }
        } catch (ValidationException e) {
            PopupUtil.showError("Error", e.getMessage());
        }
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void hide() {
        showing= false;
        hideOnSearch();
        Application.getInstance().getToolBar().getDropdown().close();
    }

    protected void hideOnSearch() { }

    public void show() {
        showing= true;
        if (useNewPanel) {
            imSelPanel2.showPanel();
        }
        else {
            imSelPanel.showPanel();
        }
        Application.getInstance().getToolBar().getDropdown().setTitle("Select Image");
        Application.getInstance().getToolBar().getDropdown().setContent(mainPanel,true,null, ImageSelectDropDownCmd.COMMAND_NAME);
    }



    private void inputComplete() {
        inProcess= true;
        if (hideType== BaseDialog.HideType.BEFORE_COMPLETE) hide();
        if (useNewPanel) {
            imSelPanel2.inputComplete();
        }
        else {
            imSelPanel.inputComplete();
        }
        if (hideType== BaseDialog.HideType.AFTER_COMPLETE) hide();
        inProcess= false;
    }

    public boolean isInProcess() { return inProcess; }

    protected boolean validateInput() throws ValidationException {
        if (useNewPanel) {
            return imSelPanel2.validateInput();
        }
        else {
            return imSelPanel.validateInput();
        }
    }


    private class DropDownComplete implements ImageSelectPanel.PanelComplete, ImageSelectPanel2.PanelComplete {
        public void performInputComplete() {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public BaseDialog.HideType getHideAlgorythm() {
            return hideType;
        }

        public void setHideAlgorythm(BaseDialog.HideType hideType) {
            ImageSelectDropDown.this.hideType= hideType;
        }

        public void hide() {
            ImageSelectDropDown.this.hide();
        }
    }


    public class SubmitKeyPressHandler implements KeyPressHandler {
        public void onKeyPress(KeyPressEvent ev) {
            final int keyCode = ev.getNativeEvent().getKeyCode();
            char charCode = ev.getCharCode();
            if ((keyCode == KeyCodes.KEY_ENTER || charCode == KeyCodes.KEY_ENTER) && ev.getRelativeElement() != null) {
                DeferredCommand.addCommand(new Command() {
                    public void execute() { doOK();  }
                });
            }
        }
    }

}
