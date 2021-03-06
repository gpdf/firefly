/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.plot.GeomException;

import java.io.File;

/**
 * @author tatianag
 *         $Id: ProcessorFileRetriever.java,v 1.8 2012/07/27 22:23:29 tatianag Exp $
 */
public class ProcessorFileRetriever implements FileRetriever {
    public FileData getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException {
        try {
            TableServerRequest sreq;
            String reqStr = request.getParam(ServerParams.REQUEST);
            if (reqStr != null) {
                sreq = TableServerRequest.parse(reqStr);
            } else {
                sreq= QueryUtil.assureType(TableServerRequest.class, request);
            }
            FileInfo fi = new SearchManager().getFileInfo(sreq);
            if (fi == null) {
                throw new FailedRequestException("Unable to get file location info");
            }
            if (fi.getInternalFilename()== null) {
                throw new FailedRequestException("File not available");
            }
            if (!fi.hasAccess()) {
                throw new SecurityException("Access is not permitted.");
            }
            File f= new File(fi.getInternalFilename());
            return new FileData(f, f.getName());
        } catch (DataAccessException dae) {
            Logger.error(dae);
            throw new FailedRequestException("Unable to get file location info", dae.getMessage(), dae);
        } catch (Exception e) {
            Logger.error(e);
            throw new FailedRequestException("Unable to get file location info", e.getMessage(), e);            
        }
    }
}
