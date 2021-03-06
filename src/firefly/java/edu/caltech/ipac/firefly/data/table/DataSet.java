/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.table;

import edu.caltech.ipac.firefly.data.fuse.DatasetInfoConverter;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.PropertyChangeSupport;
import edu.caltech.ipac.util.CollectionUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;


/**
 * This is a utility class use to represent a set of data, similiar to ResultSet which can be pass to to a client. All
 * reference of index starts from 0. <p>
 * <p/>
 * <b>NOTE:</b> Note that this implementation is not synchronized. If multiple threads access a DataSet instance
 * concurrently, and at least one of the threads modifies the list structurally, it must be synchronized externally.
 *
 * @author loi
 * @version $Id: DataSet.java,v 1.35 2012/01/12 18:22:53 loi Exp $
 */
public class DataSet implements TableDataView, Serializable {

    private transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private transient DatasetInfoConverter datasetInfoProvider;
    //    private transient TreeSet<Integer> highlightedRows = new TreeSet<Integer>();
    private ArrayList<Column> columns;
    private int highlightedRow;
    private BaseTableData model;
    private int totalRows;
    private int firstRowIdx;
    private TableMeta meta;
    //    private transient TreeSet<Integer> selectedRows = new TreeSet<Integer>();
    private SelectionInfo selectInfo = new SelectionInfo();


    public DataSet() {
        this(new Column[0]);
    }

    public DataSet(Column[] meta) {
        model = new BaseTableData(new String[0]);
        setColumns(meta);
    }

    public DataSet(BaseTableData model) {
        this.model = model;
        setColumns(createColumns(model));
    }

    private Column[] createColumns(TableData model) {
        List<String> colNames = model.getColumnNames();
        Column[] cols = new BaseTableColumn[colNames.size()];
        for (int i = 0; i < colNames.size(); i++) {
            cols[i] = new BaseTableColumn(colNames.get(i));
        }
        return cols;
    }

    public DatasetInfoConverter getDatasetInfoConverter() {
        return datasetInfoProvider;
    }

    public void setDatasetInfoProvider(DatasetInfoConverter datasetInfoProvider) {
        this.datasetInfoProvider = datasetInfoProvider;
    }

    public TableData getModel() {
        return model;
    }

    public void setModel(TableData model) {
        TableData old = this.model;
        this.model = (BaseTableData) model;
        this.model.setHasAccessCName(getMeta().getAttribute(TableMeta.HAS_ACCESS_CNAME));
        pcs.firePropertyChange(MODEL_LOADED, old, model);
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
        selectInfo.setRowCount(totalRows);
    }

    public int getStartingIdx() {
        return firstRowIdx;
    }

    public void setStartingIdx(int index) {
        this.firstRowIdx = index;
    }

    public TableMeta getMeta() {
        return meta;
    }

    /**
     * Sets the meta identification for this DataSet.
     */
    public void setMeta(TableMeta meta) {
        this.meta = meta;
    }

    /**
     * Returns all of the columns data for this DataSet
     *
     * @return
     */
    public List<Column> getColumns() {
        return Collections.unmodifiableList((List<? extends Column>) columns.clone());
    }

    public void moveColumn(Column col, int toIdx) {
        if (columns.remove(col)) {
            columns.add(toIdx, col);
        }
    }

    public void addColumn(Column col) {
        columns.add(col);
        model.addColumn((columns.size() - 1), col.getName());
    }

    public void addColumn(int index, Column col) {
        columns.add(index, col);
    }

    public void removeColumn(Column col) {
        columns.remove(col);
    }

    /**
     * Returns the columns data for the given column.
     *
     * @param colIdx index of the column
     * @return
     */
    public Column getColumn(int colIdx) {
        return columns.get(colIdx);
    }

    public Column findColumn(String colName) {
        for (Column c : columns) {
            if (c.getName().equals(colName)) {
                return c;
            }
        }
        return null;
    }

    public int findColumnIdx(String colName) {
        Column c = findColumn(colName);
        if (c != null) {
            return columns.indexOf(c);
        } else {
            return -1;
        }
    }

    public void highlight(int rowIdx) {

        if (rowIdx >= 0 && rowIdx < this.getTotalRows()) {
            int oldv = highlightedRow;
            highlightedRow = rowIdx;
            pcs.firePropertyChange(ROW_HIGHLIGHTED, oldv, rowIdx);
        }
    }

    public void clearHighlighted() {
        int oldv = highlightedRow;
        highlightedRow = -1;
        pcs.firePropertyChange(ROW_CLEARHIGHLIGHTED, oldv, highlightedRow);
    }

    public int getHighlighted() {
        return highlightedRow;
    }

    /**
     * rowIdx is the absolute row index of the whole table.
     *
     * @param rowIdx
     */
    public void select(Integer... rowIdx) {
        for (Integer i : rowIdx) {
            selectInfo.select(i);
        }
        pcs.firePropertyChange(ROW_SELECTED, selectInfo, rowIdx);
    }

    public void setSelectionInfo(SelectionInfo selectInfo) {
        this.selectInfo = selectInfo;
        pcs.firePropertyChange(ROW_SELECTED, selectInfo, selectInfo.getSelected().toArray(new Integer[0]));
    }

    public SelectionInfo getSelectionInfo() {
        return selectInfo;
    }

    /**
     * rowIdx is the absolute row index of the whole table.
     *
     * @param rowIdx
     */
    public boolean isSelected(int rowIdx) {
        return selectInfo.isSelected(rowIdx);
    }

    public boolean isSelectAll() {
        return selectInfo.isSelectAll();
    }

    public void selectAll() {
        selectInfo.selectAll();
        pcs.firePropertyChange(ROW_SELECT_ALL, null, selectInfo);
    }

    /**
     * rowIdx is the absolute row index of the whole table.
     *
     * @param rowIdx
     */
    public void deselect(Integer... rowIdx) {
        if (rowIdx != null && rowIdx.length > 0) {
            for (int i : rowIdx) {
                selectInfo.deselect(i);
            }
            pcs.firePropertyChange(ROW_DESELECTED, selectInfo, rowIdx);
        }
    }

    public List<Integer> getSelected() {
        SortedSet<Integer> ss = selectInfo.getSelected();
        ArrayList<Integer> list = new ArrayList<Integer>();
        list.addAll(ss);
        return list;
    }

    public void deselectAll() {
        selectInfo.deselectAll();
        pcs.firePropertyChange(ROW_DESELECT_ALL, null, selectInfo);
    }

    public void addPropertyChangeListener(String type, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(type, listener);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }


    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }


    /**
     * Defines the columns data information for this DataSet
     *
     * @param meta
     */
    public void setColumns(Column[] meta) {
        this.columns = new ArrayList<Column>(meta.length);
        this.columns.addAll(Arrays.asList(meta));
    }

    /**
     * get a subset of the current DataSet. Only the TableModel is cloned.  The rest are references.
     *
     * @param fromIdx
     * @param toIdx
     * @return
     */
    public DataSet subset(int fromIdx, int toIdx) {

        DataSet newval = emptyCopy();
        newval.firstRowIdx = fromIdx;

        int beginIdx = fromIdx - firstRowIdx;
        int endIdx = Math.min(beginIdx + toIdx - fromIdx, getModel().size());

        if (beginIdx < endIdx) {
            ArrayList<BaseTableData.RowData> result = new ArrayList<BaseTableData.RowData>();
            for (int i = beginIdx; i < endIdx; i++) {
                result.add(model.getRow(i));
            }
            newval.model.getRows().addAll(result);
            newval.totalRows = result.size();
        }
        return newval;
    }


    /**
     * get a subset of the current DataSet. Only the TableModel is cloned.  The rest are references.
     *
     * @param filter interface to filter the correct rows for the new DataSet
     * @return the filtered DataSet
     */
    public DataSet subset(CollectionUtil.Filter<BaseTableData.RowData> filter) {
        List<BaseTableData.RowData> inRows = this.getModel().getRows();
        ArrayList<BaseTableData.RowData> outRows = new ArrayList<BaseTableData.RowData>();
        CollectionUtil.filter(inRows, outRows, filter);

        DataSet newval = emptyCopy();
        newval.firstRowIdx = 0;
        newval.totalRows = outRows.size();
        newval.model.getRows().addAll(outRows);
        return newval;
    }

    public DataSet clone() {
        DataSet newval = new DataSet();
        newval.columns = (ArrayList<Column>) columns.clone();
        newval.model = model.clone();
        newval.meta = meta.clone();
        return newval;
    }

    private DataSet emptyCopy() {
        DataSet newval = new DataSet();
        newval.columns = columns;
        newval.model = model.clone();
        newval.model.getRows().clear();
        newval.meta = meta;
        return newval;
    }

    public int getSize() {
        return model == null ? 0 : model.getSize();
    }

    public boolean hasAccess(int index) {
        return model != null && model.hasAccess(index);
    }
}

