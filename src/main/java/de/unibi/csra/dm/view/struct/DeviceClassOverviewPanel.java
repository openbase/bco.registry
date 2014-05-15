/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.csra.dm.view.struct;

import de.unibi.csra.dm.exception.NotAvailableException;
import de.unibi.csra.dm.struct.DeviceClass;
import java.util.List;
import javax.swing.JTable;

/**
 *
 * @author mpohling
 */
public class DeviceClassOverviewPanel extends AbstractOverviewPanel<DeviceClass> {

	/**
	 * Creates new form DeviceClassOverviewPanel
	 */
	public DeviceClassOverviewPanel() {

	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

	@Override
	protected List<DeviceClass> getContextList() {
		return deviceManager.getDeviceClassList();
	}

	@Override
	protected String[] getContextLables() {
		String[] contextLables = {"ID","Name", "ProductNumber", "Category", "Description"};
		return contextLables;
	}

	@Override
	protected void updateContextData(final DeviceClass context, final Object[] contextData) {
		contextData[0] = context.getId();
		contextData[1] = context.getName();
		contextData[2] = context.getProductNumber();
		contextData[3] = context.getCategory();
		contextData[4] = context.getDescription();
	}

	@Override
	protected void remove(final DeviceClass deviceClass) {
		deviceManager.removeDeviceClass(deviceClass.getId());
	}

	@Override
	protected void edit(final DeviceClass deviceClass) {
		DeviceClassEditorFrame.edit(deviceClass);
	}

	@Override
	protected void add() {
		DeviceClass deviceClass = new DeviceClass();
		edit(deviceClass);
	}

	@Override
	protected DeviceClass getSelection(final JTable contextTable) throws NotAvailableException {
		return deviceManager.getDeviceClass((String) contextTable.getModel().getValueAt(contextTable.convertRowIndexToModel(contextTable.getSelectedRow()), 0));
	}
}
