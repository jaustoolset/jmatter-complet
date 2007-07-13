/*
 * Created on Nov 22, 2004
 */
package com.u2d.view.swing.simplecal;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;

import com.u2d.calendar.CalEvent;
import com.u2d.calendar.CellResChoice;
import com.u2d.type.atom.*;

import javax.swing.*;

/**
 * @author Eitan Suez
 */
public interface TimeIntervalView
{
   public TimeInterval getTimeInterval();
   public TimeSpan getSpan();
   public javax.swing.JLabel getLabel();
   public Rectangle getBounds(CalEvent event);
   public void addActionListener(java.awt.event.ActionListener l);
   public CellResChoice getCellResolution();
   public void setCellResolution(CellResChoice cellRes);
   public void addChangeListener(javax.swing.event.ChangeListener l);
   public void addAdjustmentListener(AdjustmentListener l);
   public JScrollPane getScrollPane();
   public void addPropertyChangeListener(String property, PropertyChangeListener l);
   public void removePropertyChangeListener(String property, PropertyChangeListener l);
}