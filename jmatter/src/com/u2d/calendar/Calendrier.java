/*
 * Created on Apr 13, 2004
 */
package com.u2d.calendar;

import com.u2d.model.*;
import com.u2d.view.*;
import com.u2d.type.atom.*;
import com.u2d.app.Tracing;
import com.u2d.list.PlainListEObject;
import com.u2d.find.*;

import java.io.*;
import java.util.*;

import org.hibernate.Criteria;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Expression;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;

/**
 * Note:  Named so as not to conflict/confuse with java.util.Calendar
 * 
 * @author Eitan Suez
 */
public class Calendrier extends AbstractComplexEObject
                        implements Serializable, EventManager, DateTimeBounded, QueryReceiver
{
   private transient Calendarable _cal;
   private Title _title = new Title("Calendar");
   private DateTimeBounds _bounds = new DateTimeBounds();

   private List _schedulables = new ArrayList();
   private final TimeSpan _span = new TimeSpan();
   private AbstractListEO _schedules = new PlainListEObject(Schedule.class);

   private Query _query, _previousQuery;
   

   public Calendrier() { setStartState(); }
   public Calendrier(Calendarable cal)
   {
      this();
      _cal = cal;
      setQuery(new SimpleQuery(eventType()));
   }

   public CalEvent newDefaultCalEvent(TimeSpan span)
   {
      ComplexType eventType = ComplexType.forClass(_cal.defaultCalEventType());
      CalEvent calEvt = (CalEvent) eventType.instance();
      calEvt.timeSpan(span);
      calEvt.schedulable((Schedulable) _schedulables.get(0));
      return calEvt;
   }

   public CalEvent newEvent(TimeSpan span) { return newDefaultCalEvent(span); }

   public Calendarable calendarable() { return _cal; }

   public DateTimeBounds bounds() { return _bounds; }
   public void bounds(DateTimeBounds bounds) { _bounds = bounds; }

   // TODO: revisit
//   public Onion commands()  // hack
//   {
//      return type().commands(getState()).subList(0, 1);
//   }

   public AbstractListEO schedules()
   {
      AbstractListEO schedules = loadSchedules();
      _schedulables = new ArrayList();
      Schedule schedule;
      for (int i=0; i<schedules.getSize(); i++)
      {
         schedule = (Schedule) schedules.getElementAt(i);
         schedule.inCalendarContext(this);
         _schedulables.add(schedule.getSchedulable());
      }
      return schedules;
   }

   private ComplexType eventType()
   {
      Class eventClass = _cal.defaultCalEventType();
      return ComplexType.forClass(eventClass);
   }


   public ComplexType queryType() { return eventType(); }

   public void setQuery(Query query)
   {
      setQuery(query, _span);
   }
   public synchronized void setQuery(Query query, TimeSpan span)
   {
      _previousQuery = _query;
      _query = query;
      fetchEvents(span);
   }
   public synchronized void setSpan(TimeSpan span)
   {
      fetchEvents(span);
   }

   private Criteria criteria()
   {
      Criteria criteria = _query.getCriteria();

      Junction junction = Expression.conjunction();
      
      Class evtClass = eventType().getJavaClass();
      
//      if (!_schedulables.isEmpty())
//      {
//         String schedulableFieldname = CalEvent.schedulableFieldname(evtClass);
//         junction.add(Expression.in(schedulableFieldname , _schedulables));
//      }
      
      String timespanFieldname = CalEvent.timespanFieldname(evtClass);
      junction.add(Expression.ge(timespanFieldname + ".start", _span.startDate()));
      junction.add(Expression.le(timespanFieldname + ".end", _span.endDate()));
      criteria.add(junction);

      return criteria;
   }
   
   public void fetchEvents(TimeSpan span)
   {
      // optimization: when switch from weekview to dayview, already have events..
      if ( (_previousQuery != null && _previousQuery.equals(_query)) &&
           (_span != null && (_span.equals(span) || _span.containsCompletely(span))) )
      {
         for (int i=0; i<_schedules.getSize(); i++)
         {
            Schedule s = (Schedule) _schedules.get(i);
            s.getCalEventList().fireContentsChanged(this, 0, s.getCalEventList().getSize());
         }
         return;
      }
      _span.setValue(span);
      fetchCurrentSpan();
   }
   
   private void fetchCurrentSpan()
   {
      Tracing.tracer().info("calendrier: fetching events for time span: "+_span);
      addEventsToSchedules(criteria().list());
   }

   private void addEventsToSchedules(List events)
   {
      Map<Schedule, List> scheduleEvents = new HashMap<Schedule, List>();
      for (Iterator itr = _schedules.iterator(); itr.hasNext(); )
      {
         Schedule s = (Schedule) itr.next();
         scheduleEvents.put(s, new ArrayList());
      }
      
      for (Iterator itr = events.iterator(); itr.hasNext(); )
      {
         CalEvent event = (CalEvent) itr.next();
         event.onLoad();
         Schedule s = event.schedulable().schedule();
         List l = scheduleEvents.get(s);
         l.add(event);
      }
      
      for (Schedule s : scheduleEvents.keySet())
      {
         s.getCalEventList().setItems(scheduleEvents.get(s));
      }
   }

   void addEvent(CalEvent event, Schedule schedule)
   {
      schedule.getCalEventList().add(event);
   }

   public EView getMainView() { return getCalendarView(); }
   public ComplexEView getCalendarView() { return vmech().getCalendarView(this); }


   public Title title() { return _title; }

   // =======

   private AbstractListEO loadSchedules()
   {
      AbstractListEO schedulables = _cal.schedulables();
      if (schedulables == null)
      {
         return new PlainListEObject(Schedulable.class);
      }

      // ensure that list of schedules is in sync with list of resources
      // (e.g. rooms, providers)
      ScheduleSynchronizer _scheduleSynchronizer = new ScheduleSynchronizer();
      schedulables.addListDataListener(_scheduleSynchronizer);
      _scheduleSynchronizer.contentsChanged(null);
      
      return _schedules;
   }

   class ScheduleSynchronizer implements ListDataListener
   {
      public void intervalAdded(ListDataEvent e)
      {
         Schedulable schedulable = null;
         for (int i=e.getIndex0(); i<=e.getIndex1(); i++)
         {
            schedulable = (Schedulable) _cal.schedulables().getElementAt(i);
            _schedules.add(schedulable.schedule());
         }
      }

      public void intervalRemoved(ListDataEvent e)
      {
         Schedulable schedulable = null;
         for (int i=e.getIndex0(); i<=e.getIndex1(); i++)
         {
            schedulable = (Schedulable) _cal.schedulables().getElementAt(i);
            _schedules.remove(schedulable.schedule());
         }
      }

      public void contentsChanged(ListDataEvent e)
      {
         Schedulable schedulable;  Schedule schedule;
         List list = new ArrayList();

         for (Iterator schedsItr = _cal.schedulables().iterator(); 
               schedsItr.hasNext(); )
         {
            schedulable = (Schedulable) schedsItr.next();
            schedule = schedulable.schedule();
            list.add(schedule);
         }
         _schedules.setItems(list);
      }
   }

}