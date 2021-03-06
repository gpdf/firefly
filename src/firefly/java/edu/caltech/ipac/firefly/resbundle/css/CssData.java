/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.resbundle.css;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

/**
 * User: roby
 * Date: Jan 27, 2010
 * Time: 12:47:16 PM
 */
public interface CssData extends ClientBundle {
    @ClientBundle.Source("firefly-bundle.css")
    FireflyCss getFireflyCss();

    @Source("tab_bg.png")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Horizontal)
    public ImageResource tabBackground();

    @Source("invalid_line.gif")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Horizontal)
    public ImageResource invalidLine();

    @Source("text-bg.gif")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Horizontal)
    public ImageResource textBackground();

    @Source("bg_listgradient.png")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Horizontal)
    public ImageResource bgListGradient();

    @Source("popup_bg-horizontal.jpg")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Horizontal)
    public ImageResource popupBackgroundHorizontal();

    @Source("popup_bg-vertical.jpg")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Vertical)
    public ImageResource popupBackgroundVertical();


    @Source("gxt/light-hd.gif")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Horizontal)
    public ImageResource lightHd();

    @Source("gxt/tb-blue.gif")
    @ImageResource.ImageOptions(repeatStyle= ImageResource.RepeatStyle.Horizontal)
    public ImageResource tbBlue();



    public static class Creator  {
        private final static CssData _instance= (CssData) GWT.create(CssData.class);
        public static CssData getInstance() { return _instance; }
    }

}
