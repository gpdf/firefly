/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.dyn.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import edu.caltech.ipac.firefly.data.dyn.xstream.AccessTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.FieldGroupTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LabelTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.PreDefFieldTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.HelpTag;
import edu.caltech.ipac.firefly.server.dyn.DynServerData;
import edu.caltech.ipac.firefly.server.dyn.DynServerUtils;
import edu.caltech.ipac.util.dd.MappedFieldDefSource;
import edu.caltech.ipac.util.action.ActionConst;

public class FieldGroupConverter implements Converter {

    public boolean canConvert(Class clazz) {
        return clazz.equals(FieldGroupTag.class);
    }

    public void marshal(Object value, HierarchicalStreamWriter writer,
                        MarshallingContext context) {
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {

        DynServerData dataStore = DynServerData.getInstance();

        FieldGroupTag fieldGroupTag = new FieldGroupTag();
        String xidFlag = null;

        String attrVal = reader.getAttribute("xid");
        if (attrVal != null) {
            xidFlag = attrVal;
            fieldGroupTag.setXid(attrVal);
        }

        attrVal = reader.getAttribute("ref-xid");
        if (attrVal != null) {
            fieldGroupTag = (FieldGroupTag) DynServerUtils.copy((FieldGroupTag) dataStore.getProjectXid(attrVal));
            if (fieldGroupTag == null) {
                fieldGroupTag = new FieldGroupTag();
            }
        }

        attrVal = reader.getAttribute("type");
        if (attrVal != null) {
            fieldGroupTag.setType(attrVal);
        }

        attrVal = reader.getAttribute("typeName");
        if (attrVal != null) {
            fieldGroupTag.setTypeName(attrVal);
        }

        attrVal = reader.getAttribute("direction");
        if (attrVal != null) {
            fieldGroupTag.setDirection(attrVal);
        }

        attrVal = reader.getAttribute("align");
        if (attrVal != null) {
            fieldGroupTag.setAlign(attrVal);
        }

        attrVal = reader.getAttribute("labelWidth");
        if (attrVal != null) {
            fieldGroupTag.setLabelWidth(attrVal);
        }

        attrVal = reader.getAttribute("height");
        if (attrVal != null) {
            fieldGroupTag.setHeight(attrVal);
        }

        attrVal = reader.getAttribute("width");
        if (attrVal != null) {
            fieldGroupTag.setWidth(attrVal);
        }

        attrVal = reader.getAttribute("spacing");
        if (attrVal != null) {
            fieldGroupTag.setSpacing(attrVal);
        }

        attrVal = reader.getAttribute("downloadRestriction");
        if (attrVal != null) {
            fieldGroupTag.setDownloadRestriction(attrVal);
        }

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String childName = reader.getNodeName();
            if (childName.equalsIgnoreCase("Title")) {
                fieldGroupTag.setTitle(reader.getValue());

            } else if (childName.equalsIgnoreCase("Tooltip")) {
                fieldGroupTag.setTooltip(reader.getValue());

            } else if (childName.equalsIgnoreCase("Access")) {
                AccessTag at = (AccessTag) context.convertAnother(
                        fieldGroupTag, AccessTag.class);
                fieldGroupTag.setAccess(at);

            } else if (childName.equalsIgnoreCase("FieldGroup")) {
                FieldGroupTag fg = (FieldGroupTag) context.convertAnother(fieldGroupTag,
                        FieldGroupTag.class);
                fieldGroupTag.addUIComponent(fg);

            } else if (childName.equalsIgnoreCase("PreDefField")) {
                PreDefFieldTag pdf = (PreDefFieldTag) context.convertAnother(
                        fieldGroupTag, PreDefFieldTag.class);
                fieldGroupTag.addUIComponent(pdf);

            } else if (childName.equalsIgnoreCase("Label")) {
                LabelTag l = new LabelTag(reader.getValue());
                fieldGroupTag.addUIComponent(l);

            } else if (childName.equalsIgnoreCase("Help")) {
                HelpTag h = (HelpTag) context.convertAnother(fieldGroupTag, HelpTag.class);
                fieldGroupTag.addUIComponent(h);

            } else {
                // all other FieldDefs
                MappedFieldDefSource fd = new MappedFieldDefSource(childName);

                // hidden field
                if (childName.equalsIgnoreCase("HIDDEN")) {
                    fd.set(ActionConst.MASK, "[HIDDEN]");
                }

                // orientation attribute
                if (childName.equalsIgnoreCase("EnumString")) {
                    String orient = reader.getAttribute("orientation");
                    if (orient != null) {
                        fd.set(ActionConst.ORIENTATION, orient);
                    }
                }

                // sciNote attribute
                if (childName.equalsIgnoreCase("Double") || childName.equalsIgnoreCase("Float")) {
                    String sciAllowed = reader.getAttribute("scientificAllowed");
                    if (sciAllowed != null) {
                        fd.set(ActionConst.SCIENTIFIC_ALLOWED, sciAllowed);
                    }
                }

                // minBound attribute
                if (childName.equalsIgnoreCase("Double") || childName.equalsIgnoreCase("Float") || childName.equalsIgnoreCase("Date")) {
                    String minBound = reader.getAttribute("minBoundType");
                    if (minBound != null) {
                        fd.set(ActionConst.MIN_BOUND_TYPE, minBound);
                    }
                }

                // maxBound attribute
                if (childName.equalsIgnoreCase("Double") || childName.equalsIgnoreCase("Float") || childName.equalsIgnoreCase("Date")) {
                    String maxBound = reader.getAttribute("maxBoundType");
                    if (maxBound != null) {
                        fd.set(ActionConst.MAX_BOUND_TYPE, maxBound);
                    }
                }

                // units attribute
                if (childName.equalsIgnoreCase("DEGREE")) {
                    String units = reader.getAttribute("units");
                    if (units != null) {
                        fd.set(ActionConst.UNITS, units);
                    }
                }

                String formatValue = "";
                String itemList = "";

                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    String elemName = reader.getNodeName();

                    if (elemName.equalsIgnoreCase("Cached")) {
                        fd.set(elemName, "true");

                    } else if (elemName.equalsIgnoreCase("ID")) {
                        String elemValue = reader.getValue();
                        fd.set(ActionConst.ID , elemValue);

                    } else if (elemName.equalsIgnoreCase("NullAllowed")) {
                        fd.set(elemName, "true");

                    } else if (elemName.equalsIgnoreCase("Format")) {
                        // special case for Format - set outside of while loop
                        String elemValue = reader.getValue();
                        if (formatValue.length() == 0) {
                            formatValue += elemValue;
                        } else {
                            formatValue += ";" + elemValue;
                        }

                    } else if (elemName.equalsIgnoreCase("EnumValue")) {
                        // special case for EnumValue
                        String itemId = reader.getAttribute("id");
                        String itemTitle = null;
                        String itemDesc = null;
                        String itemIntVal = null;

                        while (reader.hasMoreChildren()) {
                            reader.moveDown();
                            String itemElemName = reader.getNodeName();
                            String itemElemValue = reader.getValue();
                            if (itemElemName.equalsIgnoreCase("Title"))
                                itemTitle = itemElemValue;
                            else if (itemElemName.equalsIgnoreCase("ShortDescription"))
                                itemDesc = itemElemValue;
                            else if (itemElemName.equalsIgnoreCase("IntValue"))
                                itemIntVal = itemElemValue;

                            reader.moveUp();
                        }


                        // set outside of while loop
                        if (itemList.length() == 0) {
                            itemList += itemId;
                        } else {
                            itemList += " " + itemId;
                        }

                        // set item title
                        fd.set(itemId + "." + ActionConst.TITLE, itemTitle);

                        // set item description (optional)
                        if (itemDesc != null) {
                            fd.set(itemId + "." + ActionConst.SHORT_DESCRIPTION, itemDesc);
                        }

                        // set item intValue (optional)
                        if (itemIntVal != null) {
                            fd.set(itemId + "." + ActionConst.INT_VALUE, itemIntVal);
                        }

                    } else if (elemName.equalsIgnoreCase("ValidateMask")) {
                        String validateMask = reader.getValue();
                        if (validateMask.length() > 0) {
                            fd.set(ActionConst.PATTERN, validateMask);
                        }
                    } else {
                        // all other cases
                        String elemValue = reader.getValue();
                        fd.set(elemName, elemValue);
                    }

                    reader.moveUp();
                }

                // set pattern, if available
                if (formatValue.length() > 0) {
                    fd.set(ActionConst.PATTERN, formatValue);
                }


                // set items, if available
                if (itemList.length() > 0) {
                    fd.set(ActionConst.ITEMS, itemList);
                }


                fieldGroupTag.addUIComponent(fd);

            }

            reader.moveUp();
        }

        if (xidFlag != null) {
            dataStore.addProjectXid(xidFlag, fieldGroupTag);
        }

        return fieldGroupTag;
    }

}

