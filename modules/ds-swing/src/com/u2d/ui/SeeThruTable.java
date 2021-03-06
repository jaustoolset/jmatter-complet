/*
 * Created on Oct 9, 2003
 */
package com.u2d.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

/**
 * @author Eitan Suez
 */
public class SeeThruTable extends JTable
{

	public SeeThruTable()
	{
		addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent evt)
			{
				if (evt.isPopupTrigger())
					dispatch(evt);
			}
			// for microsoft platform:
			public void mouseReleased(MouseEvent evt)
			{
				if (evt.isPopupTrigger())
					dispatch(evt);
			}
			public void mouseClicked(MouseEvent evt)
			{
				dispatch(evt);
			}
			private void dispatch(MouseEvent evt)
			{
				Point eventLocation = new Point(evt.getX(), evt.getY());
				int rowIndex = rowAtPoint(eventLocation);
				int columnIndex = columnAtPoint(eventLocation);
				if (rowIndex < 0 || columnIndex < 0) return;
				
				Object value = getModel().getValueAt(rowIndex, columnIndex);
				TableCellRenderer renderer = getCellRenderer(rowIndex, columnIndex);
				Component item = renderer.getTableCellRendererComponent(SeeThruTable.this, value, false, false, rowIndex, columnIndex);
				item.dispatchEvent(evt);
			}
		});
	}

}
