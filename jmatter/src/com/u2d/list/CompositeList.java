/*
 * Created on Mar 9, 2004
 */
package com.u2d.list;

import com.u2d.model.ComplexEObject;
import com.u2d.model.Harvester;
import com.u2d.model.ComplexType;
import com.u2d.view.EView;
import com.u2d.view.ListEView;
import com.u2d.pattern.State;
import com.u2d.app.Tracing;
import java.util.logging.Logger;
import java.util.Iterator;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.beans.PropertyDescriptor;
import java.beans.IntrospectionException;
import static com.u2d.pubsub.AppEventType.*;
import com.u2d.reflection.Cmd;
import com.u2d.element.CommandInfo;

/**
 * @author Eitan Suez
 */
public class CompositeList extends SimpleListEO
{
   private transient boolean _fixedSize = false;
   private ComplexEObject _containingParent;
   private Method _setterMethod;

   private transient Logger _tracer = Tracing.tracer();

   public CompositeList(Class clazz) { super(clazz); }
   public CompositeList(Class clazz, ComplexEObject parent)
   {
      this(clazz);
      _containingParent = parent;
   }
   public CompositeList(Class clazz, ComplexEObject parent, String parentFldname)
   {
      this(clazz, parent);
      workoutSetter(parentFldname);
   }

   private void workoutSetter(String parentFldname)
   {
      try
      {
         PropertyDescriptor descriptor = new PropertyDescriptor(parentFldname, _clazz, null,
                                                                Harvester.makeSetterName(parentFldname));
         _setterMethod = descriptor.getWriteMethod();
      }
      catch (IntrospectionException e)
      {
         String msg = String.format("failed to find write method for field %s on type %s", parentFldname, _clazz);
         _tracer.info(msg);
         e.printStackTrace();
      }
   }

   public boolean isFixedSize() { return _fixedSize; }
   public void setFixedSize(boolean fs) { _fixedSize = fs; }

   public EView getView() { return getMainView(); }
   public EView getMainView() { return getListView(); }

   public ListEView getListView()
   {
      if (type().isChoice())
      {
         return getMultiChoiceView();
      }
      else if (_fixedSize)
      {
         return getTableView();
      }
      else
      {
         return vmech().getEditableListView(this);
      }
   }

   private ListEView getMultiChoiceView()
   {
      return vmech().getMultiChoiceView(this);
   }

   public synchronized void setState(final State state)
   {
      for (Iterator itr = _items.iterator(); itr.hasNext(); )
      {
         ComplexEObject ceo = (ComplexEObject) itr.next();
         ceo.setState(state);
      }
   }
   public synchronized void pushState(State state)
   {
      for (Iterator itr = _items.iterator(); itr.hasNext(); )
      {
         ComplexEObject ceo = (ComplexEObject) itr.next();
         ceo.pushState(state);
      }
   }
   public synchronized void popState()
   {
      for (Iterator itr = _items.iterator(); itr.hasNext(); )
      {
         ComplexEObject ceo = (ComplexEObject) itr.next();
         if (ceo.isTransientState())
         {
            ceo.setStartState();
         }
         else
         {
            ceo.popState();
            if (ceo.isNullState()) // stack is empty after pop..
            {
               ceo.setStartState();
            }
         }

      }
   }
   public synchronized void setStartState()
   {
      for (Iterator itr = _items.iterator(); itr.hasNext(); )
      {
         ComplexEObject ceo = (ComplexEObject) itr.next();
         ceo.setStartState();
      }
   }

   @Cmd
   public void AddItem(CommandInfo cmdInfo)
   {
      AddItem(cmdInfo, type());
   }

   @Cmd
   public void AddItem(CommandInfo cmdInfo, ComplexType type)
   {
      ComplexEObject ceo = type.instance();
      add(ceo);
      setParent(ceo);
   }

   public void add(int index, ComplexEObject item)
   {
      if (_containingParent != null) item.setState(_containingParent.getState());
      super.add(index, item);
   }

   public void add(ComplexEObject item)
   {
      if (_containingParent != null) item.setState(_containingParent.getState());
      super.add(item);
   }


   public synchronized void onBeforeSave()
   {
      for (Iterator itr = _items.iterator(); itr.hasNext(); )
      {
         ComplexEObject ceo = (ComplexEObject) itr.next();
         ceo.onBeforeSave();
      }
   }

   public synchronized void onLoad()
   {
      for (Iterator itr = _items.iterator(); itr.hasNext(); )
      {
         ComplexEObject ceo = (ComplexEObject) itr.next();
         setParent(ceo);
         ceo.onLoad();
      }
   }

   public void setParent(ComplexEObject ceo)
   {
      if (_setterMethod != null && _containingParent != null)
      {
         try
         {
            _setterMethod.invoke(ceo, _containingParent);
         }
         catch (IllegalAccessException e)
         {
            _tracer.info("failed to set back relationship for "+ceo+" in composite list "+field());
            e.printStackTrace();
         }
         catch (InvocationTargetException e)
         {
            _tracer.info("failed to set back relationship for "+ceo+" in composite list "+field());
            e.printStackTrace();
         }
      }
   }

   public synchronized void clear()
   {
      ComplexEObject item;
      int size = _items.size();
      for (int i=0; i<size; i++)
      {
         item = (ComplexEObject) _items.get(i);
         item.removeAppEventListener(DELETE, this);
      }
      _items.clear();
      fireIntervalRemoved(this, 0, size);
   }

}
