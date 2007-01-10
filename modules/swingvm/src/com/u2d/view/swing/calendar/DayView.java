/*
 * Created on Nov 22, 2004
 */
package com.u2d.view.swing.calendar;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.io.IOException;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import com.u2d.calendar.*;
import com.u2d.type.atom.*;
import com.u2d.ui.CustomLabel;
import com.u2d.model.ComplexEObject;

/**
 * @author Eitan Suez
 */
public class DayView extends JPanel implements TimeIntervalView
{
   private static int COLUMN_WIDTH = 300;
   private static int FIRST_COLUMN_WIDTH = 65;
   private static Color SELECTION_BACKGROUND = new Color(0xf0f5fb);

   public static TimeInterval INTERVAL = new TimeInterval(Calendar.HOUR, 24);
   private static TimeInterval DAY_INTERVAL = new TimeInterval(Calendar.HOUR, 12);

   private static java.text.SimpleDateFormat LABEL_DATE_FORMATTER =
      new java.text.SimpleDateFormat("EEEE MMMM dd yyyy");

   private TimeSpan _daySpan;
   private TimeInterval _cellRes = new TimeInterval(Calendar.MINUTE, 30);

   private JTable _table;
   private DayTableModel _model;
   private int _initialRowHeight;
   private JScrollPane _scrollPane;

   private DateEO _eo;
   private JLabel _label = new CustomLabel(16.0f, JLabel.CENTER);

   public DayView(DateEO eo)
   {
      _eo = eo;
      _eo.addChangeListener(new javax.swing.event.ChangeListener()
         {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
               adjustDayspan();
               _label.setText(LABEL_DATE_FORMATTER.format(_eo.dateValue()));

               repaint();
               fireStateChanged();
           }
         });

      adjustDayspan();
      _label.setText(LABEL_DATE_FORMATTER.format(_eo.dateValue()));

      buildTable();
      setupDropHandler();

      setLayout(new BorderLayout());
      _scrollPane = new JScrollPane(_table);
      add(_scrollPane, BorderLayout.CENTER);
   }

   private void buildTable()
   {
      _model = new DayTableModel();
      _table = new JTable();

      _table.setAutoCreateColumnsFromModel(false);
      _table.setModel(_model);

      TableColumn column = new TableColumn(0, FIRST_COLUMN_WIDTH,
                                           new RowHeaderCellRenderer(), null);
      column.setMinWidth(FIRST_COLUMN_WIDTH);
      column.setMaxWidth(FIRST_COLUMN_WIDTH);
      column.setIdentifier("times");
      _table.addColumn(column);

      _table.setGridColor(Color.lightGray);
      _table.setShowGrid(true);
      _table.setRowSelectionAllowed(false);
      _table.setColumnSelectionAllowed(true);
      _table.getTableHeader().setReorderingAllowed(true);
      _table.setSelectionBackground(SELECTION_BACKGROUND);

      // table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);  // no good
      // no setting to autoresize cells..unfortunate.. must somehow set height of table programmatically as container 
      // changes size??  or extend JTable layout management?  this kind of sucks.  at this point i'm wondering 
      // whether using a JTable was the right thing to do...

      _table.addMouseListener(new MouseAdapter() {
         public void mouseClicked(MouseEvent evt)
         {
            if (evt.getClickCount() == 2)
            {
               int colidx = _table.getSelectedColumn();
               if (colidx == 0) return;  // dblclick on times labels does nothing

               TableColumnModel tcmodel = _table.getColumnModel();
               TableColumn column = tcmodel.getColumn(colidx);

               fireActionEvent(getSelectedTime(), (Schedulable) column.getIdentifier());
            }
         }
      });

      _initialRowHeight = _table.getRowHeight();

      _table.addComponentListener(new ComponentAdapter()
      {
         public void componentResized(ComponentEvent evt)
         {
            updateRowHeight();
         }
      });
   }


   private void setupDropHandler()
   {
      DropTarget dropTarget = new DropTarget();
      try
      {
         dropTarget.addDropTargetListener(new DropTargetAdapter()
         {
            public void drop(final java.awt.dnd.DropTargetDropEvent dropTargetDropEvent)
            {
               Transferable t = dropTargetDropEvent.getTransferable();

               Object transferObject = null;
               try
               {
                  DataFlavor flavor = t.getTransferDataFlavors()[0];
                  transferObject = t.getTransferData(flavor);
               }
               catch (UnsupportedFlavorException ex)
               {
                  System.err.println("UnsupportedFlavorException: "+ex.getMessage());
                  ex.printStackTrace();
                  dropTargetDropEvent.rejectDrop();
               }
               catch (IOException ex)
               {
                  System.err.println("IOException: "+ex.getMessage());
                  ex.printStackTrace();
                  dropTargetDropEvent.rejectDrop();
               }

               Point location = dropTargetDropEvent.getLocation();
               int rowIndex = _table.rowAtPoint(location);
               Date timeSlot = getSelectedTime(rowIndex);

               TableColumnModel tcmodel = _table.getColumnModel();
               int colIndex = _table.columnAtPoint(location);
               TableColumn column = tcmodel.getColumn(colIndex);
               Schedulable schedulable = (Schedulable) column.getIdentifier();

               if (transferObject instanceof CalEvent)
               {
                  final CalEvent calEvent = (CalEvent) transferObject;

                  TimeSpan moved = calEvent.timeSpan().move(timeSlot);
                  calEvent.timeSpan(moved);  // update time span for cal event
                  calEvent.schedulable(schedulable);  // update schedulable for cal event

                  calEvent.fireStateChanged();

                  new Thread()
                  {
                     public void run()
                     {
                        calEvent.save();
                        SwingUtilities.invokeLater(new Runnable()
                        {
                           public void run()
                           {
                              dropTargetDropEvent.dropComplete(true);
                           }
                        });
                     }
                  }.start();
               }
               else if (transferObject instanceof ComplexEObject)
               {
                  fireDropEvent(new CalDropEvent(this, timeSlot, schedulable, 
                                                 (ComplexEObject) transferObject,
                                                 dropTargetDropEvent));
               }
               else
               {
                  dropTargetDropEvent.rejectDrop();
                  return;
               }

            }
         });
         _table.setDropTarget(dropTarget);
      }
      catch (TooManyListenersException ex)
      {
         System.err.println("TooManyListenersException: "+ex.getMessage());
         ex.printStackTrace();
      }
   }

   private Date getSelectedTime()
   {
      return getSelectedTime(_table.getSelectedRow());
   }

   // translate cell position into start day and time
   private Date getSelectedTime(int rowidx)
   {
      Calendar cal = Calendar.getInstance();
      cal.setTime(_daySpan.startDate());
      cal.add(Calendar.MINUTE, rowidx*(int)_cellRes.getMilis()/(1000*60));
      return cal.getTime();
   }

   private void adjustDayspan()
   {
      Calendar cal = Calendar.getInstance();
      cal.setTime(_eo.dateValue());
      cal.set(Calendar.HOUR_OF_DAY, 7);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      _daySpan = new TimeSpan(cal.getTime(), DAY_INTERVAL); // 7 AM - 7 PM
   }

   private void updateRowHeight()
   {
      int vpheight = _scrollPane.getViewport().getSize().height;
      int height = _table.getSize().height;
      if (vpheight >= height)
      {
         int newRowHeight = (int) ((double) vpheight / _table.getRowCount());
         newRowHeight = Math.max(_initialRowHeight, newRowHeight);
         //System.out.println("new Row height is "+newRowHeight+"; table row count is "+getRowCount());
         _table.setRowHeight(newRowHeight);
      }
      else
      {
         _table.setRowHeight(_initialRowHeight);
      }
      _table.repaint();
   }

   public TimeSpan getSpan() { return _daySpan; }

   public TimeInterval getCellRes() { return _cellRes; }
   public void setCellResolution(TimeInterval cellRes)
   {
      _cellRes = cellRes;
      _model.updateCellRes();
      updateRowHeight();
   }

   public Rectangle getBounds(CalEvent event)
   {
      TimeSpan span = event.timeSpan();
      Date startDate = _daySpan.startDate();
      TimeSpan daySpan = new TimeSpan(startDate, span.startDate());
      double distance = daySpan.distance(_cellRes);
      distance %= _model._distance;

      int rowHeight = _table.getRowHeight();
      int yPos = (int) (distance * rowHeight) + _table.getTableHeader().getHeight();
      //System.out.println("yPos: "+yPos+"; distance: "+distance);

      int eventHeight = (int) ( ( span.duration().getMilis() * rowHeight ) / _cellRes.getMilis() );
      eventHeight = Math.max(eventHeight, rowHeight);

      Calendar cal = Calendar.getInstance();
      cal.setTime(span.startDate());

      // this is tricky because i've introduced into dayview the
      // idea of hiding a column (actually having to remove the 
      // column from the table to "hide" it).

      int xPos = _table.getColumn("times").getWidth();
      TableColumnModel tcmodel = _table.getColumnModel();
      int i=1;

      if ( (i+1) > _table.getColumnCount() )
         return new Rectangle(0, 0, 0, 0);

      TableColumn column = tcmodel.getColumn(i++);
      while (!event.schedulable().equals(column.getIdentifier()))
      {
         xPos += column.getWidth();
         if ( (i+1) > _table.getColumnCount() )
            return new Rectangle(0, 0, 0, 0);
         column = tcmodel.getColumn(i++);
      }

      int eventWidth = column.getWidth();

      Rectangle bounds = new Rectangle(xPos, yPos, eventWidth, eventHeight);

      Point offset = _scrollPane.getViewport().getViewPosition();
      bounds.x -= offset.x - 1;
      bounds.y -= offset.y - 1;

      return bounds;
   }

   public void addAdjustmentListener(AdjustmentListener l)
   {
      _scrollPane.getVerticalScrollBar().addAdjustmentListener(l);
      if (l instanceof TableColumnModelListener)
         _table.getColumnModel().addColumnModelListener((TableColumnModelListener) l);
   }


   private java.util.List _schedules = new ArrayList();
   public void addSchedule(Schedule schedule)
   {
      _schedules.add(schedule);

      TableColumn column = new TableColumn(_schedules.size(), COLUMN_WIDTH,
                                           new DefaultTableCellRenderer(), null);
      column.setIdentifier(schedule.getSchedulable());
      _table.addColumn(column);
   }

   public void removeSchedule(Schedule schedule)
   {
      TableColumn column = _table.getColumn(schedule.getSchedulable());
      _table.removeColumn(column);
      _schedules.remove(schedule);
   }

   public void removeSchedules()
   {
      Iterator itr = _schedules.iterator();
      Schedule schedule = null;
      while (itr.hasNext())
      {
         schedule = (Schedule) itr.next();
         TableColumn column = _table.getColumn(schedule.getSchedulable());
         _table.removeColumn(column);
      }
      _schedules.clear();
   }

   private Map _colMap = new HashMap();
   public void setScheduleVisible(Schedule schedule, boolean visible)
   {
      if (visible)
      {
         TableColumn column = (TableColumn) _colMap.get(schedule.getSchedulable());
         _table.addColumn(column);
      }
      else
      {
         TableColumn column = _table.getColumn(schedule.getSchedulable());
         _colMap.put(schedule.getSchedulable(), column);
         _table.removeColumn(column);
      }
   }

   class DayTableModel extends AbstractTableModel
   {
      private int _numCellsInDay;
      private double _distance;
      private TimeEO[] _times;

      DayTableModel()
      {
         updateCellRes();
      }

      private void updateCellRes()
      {
         _numCellsInDay = _daySpan.numIntervals(_cellRes);
         _distance = _daySpan.distance(_cellRes);
         _times = new TimeEO[_numCellsInDay];

         Iterator itr = _daySpan.iterator(_cellRes);
         int i=0;
         while (itr.hasNext())
         {
            _times[i++] = (TimeEO) itr.next();
         }
         fireTableStructureChanged();
      }

      public int getRowCount() { return _numCellsInDay; }
      public int getColumnCount()
      {
         return _schedules.size() + 1;
      }

      public String getColumnName(int column)
      {
         if (column == 0)
            return " ";
         Schedule schedule = (Schedule) _schedules.get(column - 1);
         return schedule.getSchedulable().title().toString();
      }

      public boolean isCellEditable(int nRow, int nCol) { return false; }

      public Object getValueAt(int nRow, int nCol)
      {
         if (nCol > 0)
            return "";
         return _times[nRow].toString();
      }

   }

   public TimeInterval getTimeInterval() { return INTERVAL; }
   public JLabel getLabel() { return _label; }

   /************************************************************************
    * List of observers.
    */

   private ActionListener subscribers = null;

   /** Add a listener that's notified when the user scrolls the
    *  selector or picks a date.
    *  @see com.holub.ui.Date_selector
    */
    public synchronized void addActionListener(ActionListener l)
    {
      subscribers = AWTEventMulticaster.add(subscribers, l);
    }

   /** Remove a listener.
    *  @see com.holub.ui.Date_selector
    */
    public synchronized void removeActionListener(ActionListener l)
    {
      subscribers = AWTEventMulticaster.remove(subscribers, l);
    }

   /** Notify the listeners of a scroll or select
    */
   private void fireActionEvent( Date date, Schedulable schedulable )
   {
      if (subscribers != null)
          subscribers.actionPerformed( new CalActionEvent(this, date, schedulable) );
   }
   

   /*****************************************************************/


   /* ** State Change Support Code ** */
   protected transient ChangeEvent _changeEvent = null;
   protected transient EventListenerList _listenerList = new EventListenerList();

   public void addChangeListener(ChangeListener l)
   {
      _listenerList.add(ChangeListener.class, l);
   }

   public void removeChangeListener(ChangeListener l)
   {
      _listenerList.remove(ChangeListener.class, l);
   }

   protected void fireStateChanged()
   {
      Object[] listeners = _listenerList.getListenerList();

      for (int i = listeners.length - 2; i >= 0; i -= 2)
      {
         if (listeners[i]==ChangeListener.class)
         {
            if (_changeEvent == null)
               _changeEvent = new ChangeEvent(this);
            ((ChangeListener)listeners[i+1]).stateChanged(_changeEvent);
         }
      }
   }

   /*****************************************************************/
   
   protected transient EventListenerList _dropListenerList = new EventListenerList();
   public void addDropListener(DropListener l)
   {
      _dropListenerList.add(DropListener.class, l);
   }
   public void removeDropListener(DropListener l)
   {
      _dropListenerList.remove(DropListener.class,  l);
   }
   protected void fireDropEvent(CalDropEvent dropEvent)
   {
      Object[] listeners = _dropListenerList.getListenerList();
      
      for (int i=listeners.length-2; i>=0; i-=2)
      {
         if (listeners[i]==DropListener.class)
         {
            ((DropListener)listeners[i+1]).itemDropped(dropEvent);
         }
      }
   }
   /*****************************************************************/

   public String toString() { return "Day View"; }
}
