/*
 * Created on Nov 22, 2004
 */
package com.u2d.view.swing.simplecal;

import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import com.u2d.calendar.*;
import com.u2d.type.atom.*;
import com.u2d.view.swing.atom.DateView2;
import com.u2d.view.swing.SwingViewMechanism;
import com.u2d.view.EView;
import com.u2d.ui.CustomTabbedPane;
import com.u2d.model.Editor;

/**
 * @author Eitan Suez
 */
public class TimeSheet extends JPanel implements ChangeListener
{
   private EventsSheet _daySheet, _weekSheet;
   private JTabbedPane _tabPane;
   private CardLayout _cardLayout;
   private JPanel _lblPnl;
   private CellResPanel _cellResPanel;
   private final DateEO _position;
   
   private CalEventList _list;

   public TimeSheet(CalEventList list, DateTimeBounds bounds)
   {
      _list = list;
      
      _position = bounds.position();
      _daySheet = new EventsSheet(new DayView(bounds), _list);
      _weekSheet = new EventsSheet(new WeekView(bounds), _list);
      
      _weekSheet.getIntervalView().setCellResolution(bounds.resolution());
      _daySheet.getIntervalView().setCellResolution(bounds.resolution());
      
      setLayout(new BorderLayout());
      JPanel pnl = new JPanel(new BorderLayout());
      pnl.add(northPanel(), BorderLayout.NORTH);
      pnl.add(body(), BorderLayout.CENTER);
      add(pnl, BorderLayout.CENTER);

      JPanel eastPanel = new JPanel(new BorderLayout());
      eastPanel.add(new DateView2(_position), BorderLayout.NORTH);
      add(eastPanel, BorderLayout.EAST);
      
      // double clicking on a cell should initiate the creation of an event..
      addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent evt)
         {
            CalActionEvent tevt = (CalActionEvent) evt;
            Date startDate = tevt.getTime();

            TimeSpan span = new TimeSpan(startDate, CalEvent.DEFAULT_DURATION);
            
            CalEvent calEvt = _list.newEvent(span);

            EView calView = calEvt.getMainView();
            if (calEvt.isEditableState() && calView instanceof Editor)
               calEvt.setEditor((Editor) calView);
            SwingViewMechanism.getInstance().displayView(calView, null);
         }
      });
      
      CalendarDropHandler cdh = new CalendarDropHandler(_list);
      getDayView().addDropListener(cdh);
      getWeekView().addDropListener(cdh);
      
      getWeekView().getSpan().addChangeListener(this);
      
      new Thread() { public void run() {
         _list.fetchEvents(selectedView().getSpan());
         } }.start();
   }
   
   public DateEO currentPosition() { return _position; }
   
   private JPanel northPanel()
   {
      JPanel northPanel = new JPanel(new BorderLayout());
      northPanel.add(heading(), BorderLayout.CENTER);
      return northPanel;
   }
   private JPanel heading()
   {
      JPanel heading = new JPanel();
      heading.setLayout(new BorderLayout());
      _cellResPanel = new CellResPanel(TimeSheet.this);
      heading.add(_cellResPanel, BorderLayout.WEST);
      heading.add(label(), BorderLayout.CENTER);
      heading.add(new NavPanel(TimeSheet.this), BorderLayout.EAST);
      return heading;
   }
   
   private JPanel label()
   {
      JLabel dayLbl = _daySheet.getIntervalView().getLabel();
      JLabel weekLbl = _weekSheet.getIntervalView().getLabel();
      _cardLayout = new CardLayout();
      _lblPnl = new JPanel(_cardLayout);
      _lblPnl.add(dayLbl, "day");
      _lblPnl.add(weekLbl, "week");
      _cardLayout.show(_lblPnl, "week");
      return _lblPnl;
   }
   
   private JComponent body()
   {
      _tabPane = new CustomTabbedPane();
      _tabPane.add("Week View", (JComponent) _weekSheet);
      _tabPane.add("Day View", (JComponent) _daySheet);
      
      _tabPane.addChangeListener(new javax.swing.event.ChangeListener()
         {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
               Component selected = _tabPane.getSelectedComponent();
               String key = (selected == _weekSheet) ? "week" : "day";
               _cardLayout.show(_lblPnl, key);
               TimeIntervalView selectedView = selectedView();
               _cellResPanel.bindTo(selectedView);
               
               // bind to selected span
               TimeIntervalView otherView = (selectedView == getWeekView()) ? getDayView() : getWeekView();
               otherView.getSpan().removeChangeListener(TimeSheet.this);
               selectedView.getSpan().addChangeListener(TimeSheet.this);
               
               if (selectedView == getWeekView())
                 selectedView.getSpan().fireStateChanged();
            }
         });
      
      return _tabPane;
   }


   public void stateChanged(ChangeEvent evt)
   {
      _list.fetchEvents(selectedView().getSpan());
   }

   
   public void shift(boolean forward)
   {
      TimeInterval interval = selectedView().getTimeInterval();
      if (forward)
         _position.add(interval);
      else
         _position.subtract(interval);
   }
   
   public CellResChoice getCellResolution() { return selectedView().getCellResolution(); }
   public void setCellResolution(CellResChoice res) { selectedView().setCellResolution(res); }
   public void addActionListener(ActionListener l)
   {
      getDayView().addActionListener(l);
      getWeekView().addActionListener(l);
   }
   
   public DayView getDayView() { return (DayView) _daySheet.getIntervalView(); }
   public WeekView getWeekView() { return (WeekView) _weekSheet.getIntervalView(); }

   public TimeIntervalView selectedView() { return selectedSheet().getIntervalView(); }
   private EventsSheet selectedSheet() { return (EventsSheet) _tabPane.getSelectedComponent(); }

   public void detach()
   {
      getDayView().getSpan().removeChangeListener(this);
      getWeekView().getSpan().removeChangeListener(this);
      _daySheet.detach();
      _weekSheet.detach();
   }


   public Dimension getPreferredSize() { return new Dimension(800,400); }
   public Dimension getMinimumSize() { return new Dimension(500,250); }
   
   
}