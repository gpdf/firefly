/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.core.background.ScriptAttributes;
import edu.caltech.ipac.firefly.server.query.BackgroundEnv;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Date: Feb 11, 2007
 *
 * @author Trey Roby
 * @version $Id: RetrieveDownloadScript.java,v 1.2 2011/02/02 01:11:11 roby Exp $
 */
@Deprecated
public class RetrieveDownloadScript extends BaseHttpServlet {

    private static final Logger.LoggerImpl _log= Logger.getLogger();

    public static final String ID_PARAM= "packageID";
    public static final String TYPE_PARAM= "type";
    public static final String FILE_PARAM= "fname";
    public static final String DS_PARAM= "dataSource";

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

        String packageID= req.getParameter(ID_PARAM); // required
        String type= req.getParameter(TYPE_PARAM);
        String fname= req.getParameter(FILE_PARAM);
        String dataSourceDesc= req.getParameter(DS_PARAM);


        if (StringUtils.isEmpty(packageID)) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND,"packageID is return cannot create download script" );
            _log.warn("Cannot create download script packageID is required, returning 404");
            return;
        }

        if (StringUtils.isEmpty(fname)) fname= "download-file";
        if (StringUtils.isEmpty(type)) type= "wget";
        if (StringUtils.isEmpty(dataSourceDesc)) dataSourceDesc= "";


        List<ScriptAttributes> attList= new ArrayList<ScriptAttributes>(2);
        if (type==null || type.equals("wget")) {
            attList.add(ScriptAttributes.Wget);
        }
        if (type==null || type.equals("wget-unzip")) {
            attList.add(ScriptAttributes.Wget);
            attList.add(ScriptAttributes.Unzip);
        }
        else if (type.equals("curl")) {
            attList.add(ScriptAttributes.Curl);
        }
        else if (type.equals("curl-unzip")) {
            attList.add(ScriptAttributes.Curl);
            attList.add(ScriptAttributes.Unzip);
        }
        else if (type.equals("list")) {
            attList.add(ScriptAttributes.URLsOnly);
        }
        else {
            attList.add(ScriptAttributes.Wget);
        }


        BackgroundEnv.ScriptRet retval= BackgroundEnv.createDownloadScript(packageID, fname, dataSourceDesc, attList);
        if (retval==null) {
            _log.warn("could not create download script, BackgroundEnv.createDownloadScript returned null, returning 404");
            return;

        }

        File downloadFile= retval.getFile();
        if (downloadFile==null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND,"could not create download script" );
            _log.warn("could not create download script, BackgroundEnv.createDownloadScript().getFile() return null, returning 404");
        }
        else if (!downloadFile.canRead()) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "could not create download script");
            _log.warn("could not create download script, BackgroundEnv.createDownloadScript().getFile() returned unreadable, returning 404");
        }
        else {
            res.addHeader("Cache-Control", "max-age=60");
            long fileLength= downloadFile.length();
            if(fileLength <= Integer.MAX_VALUE) res.setContentLength((int)fileLength);
            else                                res.addHeader("Content-Length", fileLength+"");
            String ext= FileUtil.getExtension(downloadFile);
            if (ext!=null) fname= FileUtil.setExtension(ext,fname,true);
            res.addHeader("Content-Disposition",
                          "attachment; filename="+fname);
            FileUtil.writeFileToStream(downloadFile,res.getOutputStream());
            _log.briefInfo("Downloading package retrieval script: "+ downloadFile.getPath());
        }
    }
}
