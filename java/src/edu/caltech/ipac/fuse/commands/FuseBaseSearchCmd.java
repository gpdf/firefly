package edu.caltech.ipac.fuse.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.fuse.core.SearchAdmin;
import edu.caltech.ipac.fuse.ui.FuseSearchPanel;
import edu.caltech.ipac.fuse.ui.SearchUI;
import edu.caltech.ipac.util.StringUtils;

import java.util.List;

/**
 * Date: Sep 12, 2013
 *
 * @author loi
 * @version $Id: CommonRequestCmd.java,v 1.44 2012/10/03 22:18:11 loi Exp $
 */
public abstract class FuseBaseSearchCmd extends RequestCmd implements FuseSearchPanel.EventHandler {

    private FuseSearchPanel searchPanel;

    public FuseBaseSearchCmd(String commandName) {
        super(commandName);
    }

    public boolean init() {
        searchPanel = new FuseSearchPanel(getSearchUIList());
//        Application.getInstance().getLayoutManager().getRegion(LayoutManager.DROPDOWN_REGION).setDisplay(searchPanel);
        searchPanel.setHandler(this);
        return true;
    }

    protected abstract List<SearchUI> getSearchUIList();


    protected FormHub.Validated validate() {
        return searchPanel == null ? new FormHub.Validated() : searchPanel.validate();
    }

    protected void doExecute(Request req, AsyncCallback<String> callback) {
        if (req == null) return;

        // fill the form's field based on the request parameters.
        if (searchPanel != null) {
            searchPanel.clear();
            searchPanel.populateFields(req);
        }

        if (req.isDoSearch()) {
            // process the search request
            doProcessRequest(req);
        } else {
            Application.getInstance().getLayoutManager().getRegion(LayoutManager.DROPDOWN_REGION).setDisplay(searchPanel);
//            Application.getInstance().getLayoutManager().getRegion(LayoutManager.DROPDOWN_REGION).expand();
        }
        callback.onSuccess("");
    }

    protected void doProcessRequest(Request req) {
        SearchAdmin.getInstance().submitSearch(req);
    }

//====================================================================
//  implements SearchPanel's EventHandler
//====================================================================

    public void onSearch() {
        if (doSearch()) {
            Application.getInstance().getLayoutManager().getRegion(LayoutManager.DROPDOWN_REGION).collapse();
        }
    }

    public void onSearchAndContinue() { doSearch(); }

    public void onClose() { }

//====================================================================
//  Private Methods
//====================================================================

    private boolean doSearch() {
        FormHub.Validated validated = validate();
        if (validated.isValid()) {
            final TableServerRequest req = makeRequestStub(getName(), this.getLabel());
            if (searchPanel != null) {
                searchPanel.populateClientRequest(req, new AsyncCallback<String>() {
                    public void onFailure(Throwable caught) {
                    }

                    public void onSuccess(String result) {
                        if (!hasDuplicate(req)) {
                            SearchAdmin.getInstance().submitSearch(req, searchPanel.getSearchTitle());
                        }
                    }
                });
            }
            return true;
        } else {
            if (StringUtils.isEmpty(validated.getMessage())) {
                GwtUtil.showValidationError();
            } else {
                PopupUtil.showError("Validation Error", validated.getMessage());
            }
            return false;
        }
    }


    private TableServerRequest makeRequestStub(String name, String desc) {
        Request req = new Request(name, true, false);
        req.setIsSearchResult(true);
        req.setIsDrilldownRoot(true);
        req.setDoSearch(true);
        if (desc != null) {
            req.setShortDesc(desc);
        }
        TableServerRequest retval= new TableServerRequest();
        retval.copyFrom(req);
        return retval;
    }

    private boolean hasDuplicate(TableServerRequest req) {
        return false;
    }

}

/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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