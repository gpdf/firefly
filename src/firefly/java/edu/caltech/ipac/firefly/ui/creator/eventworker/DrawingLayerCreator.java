/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.creator.DataViewCreator;
import edu.caltech.ipac.firefly.ui.creator.drawing.DatasetDrawingLayerProvider;
import edu.caltech.ipac.firefly.visualize.draw.DrawSymbol;
import edu.caltech.ipac.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Date: Aug 4, 2010
 *
 * @author loi
 * @version $Id: DrawingLayerCreator.java,v 1.4 2012/11/30 23:17:01 roby Exp $
 */
public class DrawingLayerCreator implements EventWorkerCreator {


    public static final String DATASET_VIS_QUERY = "DataSetVisQuery";

    private static final String SELECTION = "Selection";
    private static final String DECIMATION = "Decimation";

    public EventWorker create(Map<String, String> params) {

        DatasetDrawingLayerProvider worker = new DatasetDrawingLayerProvider();
        worker.setEnabled(false);
        worker.insertCommonArgs(params);
        if (params.containsKey(CommonParams.COLOR))       worker.setColor(params.get(CommonParams.COLOR));
        if (params.containsKey(CommonParams.MATCH_COLOR)) worker.setMatchColor(params.get(CommonParams.MATCH_COLOR));
        if (params.containsKey(SELECTION))   worker.setSelection(Boolean.parseBoolean(params.get(SELECTION)));
        if (params.containsKey(DECIMATION))  worker.setDecimationFactor(StringUtils.getInt(params.get(DECIMATION),1));
        if (params.containsKey(CommonParams.ENABLING_PREFERENCE)) worker.setEnablingPreferenceKey(params.get(CommonParams.ENABLING_PREFERENCE));

        if (params.containsKey(CommonParams.SYMBOL)) {
            String s = params.get(CommonParams.SYMBOL).trim();
            try {
                DrawSymbol symbol = Enum.valueOf(DrawSymbol.class, s);
                worker.setSymbol(symbol);
            } catch (Exception e) {
                // ignore - just don't set anything
            }
        }


        boolean enableDefColumns= DataViewCreator.getBooleanParam(params, CommonParams.ENABLE_DEFAULT_COLUMNS);
        if (enableDefColumns) {
            worker.enableDefaultColumns();
        }
        else {
            List<String> cenC= DataViewCreator.getListParam(params,CommonParams.CENTER_COLUMNS);
            if (cenC!=null && cenC.size()==2) {
                List<String> corC= DataViewCreator.getListParam(params,CommonParams.CORNER_COLUMNS);
                if (corC!=null && corC.size()==8) {
                    worker.initFallbackCol(cenC.get(0), cenC.get(1),
                                            corC.get(0), corC.get(1),
                                            corC.get(2), corC.get(3),
                                            corC.get(4), corC.get(5),
                                            corC.get(6), corC.get(7) );
                }
                else {
                    worker.initFallbackCol(cenC.get(0),cenC.get(1));
                }
            }

        }




        if (params.containsKey(CommonParams.TYPE)) {
            String s = params.get(CommonParams.TYPE).trim();
            try {
                DatasetDrawingLayerProvider.Type type = Enum.valueOf(DatasetDrawingLayerProvider.Type.class, s);
                worker.setDrawingType(type);
                worker.setEnabled(true);
            } catch (Exception e) {
                // ignore - just don't set anything
            }
        }

        if (params.containsKey(CommonParams.ENABLED))     worker.setEnabled(Boolean.parseBoolean(params.get(CommonParams.ENABLED)));

        String keys = params.get(CommonParams.UNIQUE_KEY_COLUMNS);
        if (keys != null) {
            List<String> keyList = StringUtils.asList(keys, ",");
            worker.setUniqueKeyColumns(keyList);
        }


        return worker;
    }


}


