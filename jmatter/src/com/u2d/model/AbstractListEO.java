/*
 * Created on Oct 11, 2004
 */
package com.u2d.model;

import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import com.u2d.element.*;
import com.u2d.field.Association;
import com.u2d.field.AssociationField;
import com.u2d.list.CSVExport;
import com.u2d.pattern.ListChangeNotifier;
import com.u2d.pattern.Onion;
import com.u2d.pattern.Block;
import com.u2d.pattern.Filter;
import com.u2d.pubsub.*;
import static com.u2d.pubsub.AppEventType.*;
import com.u2d.view.ListEView;
import com.u2d.reflection.Cmd;
import com.u2d.reflection.Arg;
import com.u2d.json.JSON;
import com.u2d.type.atom.FileWEO;
import com.u2d.type.atom.FileEO;
import javax.swing.table.*;
import org.json.JSONObject;

/**
 * @author Eitan Suez
 */
public abstract class AbstractListEO extends AbstractEObject
   implements ListChangeNotifier, ListModel, AppEventListener, Typed
{
   protected List _items = new ArrayList<EObject>();
   
   // EObject interface:
   // ============================================
   
   public Title title()
   {
      return type().title().appendParens(""+getTotal());
   }

   public abstract boolean isEmpty();

   public int validate()
   {
      int count = 0;
      for (Iterator itr = _items.iterator(); itr.hasNext(); )
      {
         EObject item = (EObject) itr.next();
         count += item.validate();
      }
      return count;
   }

   public Icon iconSm() { return listtype().iconSm(); }
   public Icon iconLg() { return listtype().iconLg(); }
   public String iconSmResourceRef() { return listtype().iconSmResourceRef(); }
   public String iconLgResourceRef() { return listtype().iconLgResourceRef(); }

   public static String[] commandOrder = {"Open", "ExportToCSV", "Print"};
   
   private ListType _listtype;
   public synchronized ListType listtype()
   {
      if (_listtype == null)
         _listtype = ListType.forClass(getClass(), getJavaClass());
      return _listtype;
   }

   public Onion commands()
   {
      Onion commands = listtype().commands();
      if (getSize()<2) return commands;
      
      Onion batchableInstanceCmds = 
            ((EObject) getElementAt(0)).commands().filter(new Filter() {
         public boolean exclude(Object item)
         {
            Command cmd = (Command) item;
            return ! cmd.batchable();
         }
      });
      if (batchableInstanceCmds.isEmpty()) return commands;

      List batchedInstanceCommands = new ArrayList();
      for (Iterator itr=batchableInstanceCmds.deepIterator(); itr.hasNext(); )
      {
         Command cmd = (Command) itr.next();
         // not yet supporting commands other than eocommands..
         if (cmd instanceof EOCommand) {
            batchedInstanceCommands.add(new BatchableCommand((EOCommand) cmd));
         }
      }
      
      commands = new Onion(commands);
      commands.addAll(batchedInstanceCommands);
      return commands;
   }

   public Command command(String commandName) { return listtype().command(commandName); }
   public Onion filteredCommands()
   {
      return commands().filter(Command.commandFilter(this));
   }


   public void setValue(EObject eo)
   {
      if (!(eo instanceof AbstractListEO))
         throw new IllegalArgumentException("Invalid type on set;  must be AbstractListEO");
      
      AbstractListEO leo = (AbstractListEO) eo;
      setItems(leo.getItems());
   }

   
   public abstract ComplexType type();
   public abstract Class getJavaClass();
   // temporary hack (see paramlistview):
   public ComplexType baseType() { return type(); }

   public String toString() { return title().toString(); }

   public synchronized List getItems() { return _items; }
   
   // convenience..
   public void setItems(Set<EObject> items)
   {
      List<EObject> list = new ArrayList<EObject>();
      list.addAll(items);
      setItems(list);
   }
   private List filterDuplicates(List list)
   {
      Set set = new HashSet(list);
      if (list.size() == set.size()) return list;
      return new ArrayList(set);
   }
   public void setItems(List<EObject> items)
   {
      synchronized(this)
      {
         if (_items == items) return;

         items = filterDuplicates(items);
         removeDeleteListeners();
         _items = items;
         addDeleteListeners();
      }
      fireContentsChanged(this, 0, _items.size());
   }
   public synchronized void restoreItems(List<EObject> items)
   {
      if (_items == items) return;
      _items = items;
   }
   
   protected void removeDeleteListeners() { updateListeners(true); }
   protected void addDeleteListeners() { updateListeners(false); }

   protected void updateListeners(boolean clear)
   {
      ComplexEObject ceo;
      for (Iterator itr = _items.iterator(); itr.hasNext(); )
      {
         ceo = (ComplexEObject) itr.next();
         
         if (ceo.isMeta()) continue;
         
         if (clear)
            ceo.removeAppEventListener(DELETE, this);
         else
            ceo.addAppEventListener(DELETE, this);
      }
   }
   
   public Iterator iterator() { return _items.iterator(); }

   public void forEach(Block block)
   {
      for (Iterator itr = _items.iterator(); itr.hasNext(); )
      {
         ComplexEObject ceo = (ComplexEObject) itr.next();
         block.each(ceo);
      }
   }

   
   public void add(int index, ComplexEObject item)
   {
      synchronized(this)
      {
         if (contains(item)) return;
         _items.add(index, item);
         item.addAppEventListener(DELETE, this);
      }
      fireIntervalAdded(this, index, index);
   }
   public void add(ComplexEObject item)
   {
      add(_items.size(), item);
   }
   
   public void onEvent(AppEvent evt)
   {
      remove((ComplexEObject) evt.getEventInfo());
   }

   public void remove(ComplexEObject item)
   {
      int index = -1;
      synchronized(this)
      {
         index = _items.indexOf(item);
         if (index >= 0)
         {
            item.removeAppEventListener(DELETE, this);
            _items.remove(item);
         }
      }
      if (index >=0)
      {
         fireIntervalRemoved(this, index, index);
      }
   }

   /**
    * Remove all items from list.
    */
   public void clear()
   {
      int size;
      synchronized(this)
      {
         size = _items.size();
         for (Object item : _items)
         {
            ComplexEObject ceo = (ComplexEObject) item;
            ceo.removeAppEventListener(DELETE, this);
         }
         _items.clear();
      }
      fireIntervalRemoved(this, 0, size);
   }
   
   public synchronized boolean contains(Object item)
   {
      return _items.contains(item);
   }
   
   /* ** ===== ListModel implementation ===== ** */
   
   public synchronized Object getElementAt(int index) { return _items.get(index); }
   public synchronized EObject first() { return (EObject) _items.get(0); }
   public synchronized EObject get(int i) { return (EObject) _items.get(i); }
   public synchronized EObject last() { return (EObject) _items.get(_items.size()-1); }
   
   public abstract int getSize();
   public abstract int getTotal();
   
   /* ** ===== TableModel implementation ===== ** */
   
   // must use an inner class because i want to extend from AbstractTableModel
   //  because it "provides default implementations for most of the methods 
   //  in the TableModel interface."
   
   protected TableModel _tableModel = null;
   public TableModel tableModel()
   {
      if (_tableModel == null)
         _tableModel = new LEOTableModel();
      return _tableModel;
   }
   public void useTableModel(TableModel model)
   {
      _tableModel = model;
   }
   
   public TableModel tableModel(final String[] fieldNames)
   {
      List<Field> fields = new ArrayList<Field>();
      for (int i=0; i<fieldNames.length; i++)
      {
         fields.add(type().field(fieldNames[i]));
      }
      return tableModel(fields);
   }
   public TableModel tableModel(String[] fieldNames, boolean setAsDefault)
   {
      TableModel model = tableModel(fieldNames);
      if (setAsDefault)
      {
         _tableModel = model;
      }
      return model;
   }

   public TableModel tableModel(final List fields)
   {
      return new AbstractTableModel()
      {
         public int getRowCount() { return getSize(); }
         public int getColumnCount() { return fields.size(); }
      
         public String getColumnName(int column)
         {
            Field field = (Field) fields.get(column);
            return field.label();
         }
      
         public Class getColumnClass(int column)
         {
            Field field = (Field) fields.get(column);
            if (field.isAssociation())
            {
               return Association.class;
            }
            else
            {
               return field.getJavaClass();
            }
         }
      
         public synchronized Object getValueAt(int row, int column)
         {
            ComplexEObject ceo = (ComplexEObject) _items.get(row);
            Field field = (Field) fields.get(column);
            if (field.isAssociation())
            {
               return ((AssociationField) field).association(ceo);
            }
            else
            {
               return field.get(ceo);
            }
         }

         public boolean isCellEditable(int row, int column)
         {
            Object o = getValueAt(row, column);
            if (o instanceof Association)
            {
               Association a = (Association) o;
               ComplexEObject parentObject = a.parent();
               return (parentObject != null && parentObject.isEditableState());
            }

            EObject value = (EObject) getValueAt(row, column);
            EObject parentObject = value.parentObject();
            return ( parentObject != null &&
                     parentObject instanceof ComplexEObject &&
                     ((ComplexEObject) parentObject).isEditableState() &&
                     value instanceof AtomicEObject &&
                     !value.field().isReadOnly()
            );
         }

         public synchronized void setValueAt(Object value, int row, int column)
         {
            Field field = (Field) fields.get(column);
            ComplexEObject parent = (ComplexEObject) _items.get(row);
            field.set(parent, value);
         }

      };
   }
   
   public class LEOTableModel extends AbstractTableModel
   {
      /**
       * slight customization here.  fields containing long text (TextEO)
       * are excluded from the tablemodel.
       */
      protected List<Field> _tableFields = new ArrayList<Field>();
      
      public LEOTableModel()
      {
         Field field;
         for (Iterator itr = type().fields().iterator(); itr.hasNext(); )
         {
            field = (Field) itr.next();
            if ( AbstractListEO.class.isAssignableFrom(field.getJavaClass()) ||
                  field.hidden() ||
                  "createdOn".equals(field.name()) ||
                  field.getJavaClass().equals(com.u2d.type.atom.Photo.class) )
               continue;
            _tableFields.add(field);
         }
      }
      public int getRowCount() { return getSize(); }
      public int getColumnCount() { return _tableFields.size() + 1; }
      
      public String getColumnName(int column)
      {
         if (column == 0)
         {
            return type().title().toString();
         }
         Field field = _tableFields.get(column - 1);
         return field.label();
      }
      
      public Class getColumnClass(int column)
      {
         if (column == 0)
         {
            return type().getJavaClass();
         }
         Field field = _tableFields.get(column - 1);
         if (field.isAssociation())
         {
            return Association.class;
         }
         else
         {
            return field.getJavaClass();
         }
      }
      
      public synchronized Object getValueAt(int row, int column)
      {
         ComplexEObject ceo = (ComplexEObject) _items.get(row);
         if (column == 0)
         {
            return ceo;
         }
         Field field = _tableFields.get(column - 1);
         if (field.isAssociation())
         {
            return ((AssociationField) field).association(ceo);
         }
         else
         {
            return field.get(ceo);
         }
      }

      // used for editable tables.

      public boolean isCellEditable(int row, int column)
      {
         Object o = getValueAt(row, column);
         if (o instanceof Association)
         {
            Association a = (Association) o;
            ComplexEObject parentObject = a.parent();
            return (parentObject != null && parentObject.isEditableState());
         }
         
         EObject value = (EObject) getValueAt(row, column);
         EObject parentObject = value.parentObject();
         return ( parentObject != null && 
                  parentObject instanceof ComplexEObject &&
                  ((ComplexEObject) parentObject).isEditableState() &&
                  value instanceof AtomicEObject &&
                  !value.field().isReadOnly()
         );
      }

      public synchronized void setValueAt(Object value, int row, int column)
      {
         if (column == 0) return; // first column of this model not editable
         Field field = _tableFields.get(column - 1);
         ComplexEObject parent = (ComplexEObject) _items.get(row);
         field.set(parent, value);
      }

   }

   /* ** ===== List Change Support code ===== ** */
   
   protected transient EventListenerList _listDataListenerList = new EventListenerList();

   public void addListDataListener(ListDataListener l)
   {
      _listDataListenerList.add(ListDataListener.class, l);
   }

   public void removeListDataListener(ListDataListener l)
   {
      _listDataListenerList.remove(ListDataListener.class, l);
   }
   
   

   public void fireContentsChanged(Object source, int index0, int index1)
   {
      Object[] listeners = _listDataListenerList.getListenerList();
      ListDataEvent e = null;
      
      for (int i = listeners.length - 2; i >= 0; i -= 2) {
         if (listeners[i] == ListDataListener.class) {
            if (e == null) {
               e = new ListDataEvent(source, ListDataEvent.CONTENTS_CHANGED, index0, index1);
            }
            ((ListDataListener)listeners[i+1]).contentsChanged(e);
         }
      }
      
      // should trigger a change event as well:
      fireStateChanged();

      if (_tableModel != null && _tableModel instanceof AbstractTableModel)
         ((AbstractTableModel) _tableModel).fireTableChanged(new TableModelEvent(tableModel()));
   }

   public void fireIntervalAdded(Object source, int index0, int index1)
   {
      Object[] listeners = _listDataListenerList.getListenerList();
      ListDataEvent e = null;

      for (int i = listeners.length - 2; i >= 0; i -= 2) {
         if (listeners[i] == ListDataListener.class) {
            if (e == null) {
               e = new ListDataEvent(source, ListDataEvent.INTERVAL_ADDED, index0, index1);
            }
            ((ListDataListener)listeners[i+1]).intervalAdded(e);
         }         
      }

      // should trigger a change event as well:
      fireStateChanged();

      if (_tableModel != null && _tableModel instanceof AbstractTableModel)
         ((AbstractTableModel) _tableModel).fireTableRowsInserted(index0, index1);
   }

   public void fireIntervalRemoved(Object source, int index0, int index1)
   {
      Object[] listeners = _listDataListenerList.getListenerList();
      ListDataEvent e = null;

      for (int i = listeners.length - 2; i >= 0; i -= 2) {
         if (listeners[i] == ListDataListener.class) {
            if (e == null) {
               e = new ListDataEvent(source, ListDataEvent.INTERVAL_REMOVED, index0, index1);
            }
            ((ListDataListener)listeners[i+1]).intervalRemoved(e);
         }
      }

      // should trigger a change event as well:
      fireStateChanged();

      if (_tableModel != null && _tableModel instanceof AbstractTableModel)
         ((AbstractTableModel) _tableModel).fireTableRowsDeleted(index0, index1);
   }
   
   public ListEView getAssociationView() { return vmech().getListView(this); }
   public ListEView getPickView() { return vmech().getPickView(this); }
   
   
   @Cmd
   public String ExportToCSV(CommandInfo cmdInfo, @Arg("Save to:") FileWEO file) throws Exception
   {
      CSVExport.export(this, file.fileValue());
      return file.fileValue().getName() + " created.";
   }
   @Cmd
   public String ExportToJSON(CommandInfo cmdInfo, @Arg("Save to:") FileWEO file) throws Exception
   {
      JSON.writeJson(file.fileValue(), this);
      return file.fileValue().getName() + " created.";
   }
   @Cmd
   public void ImportJSON(CommandInfo cmdInfo, @Arg("Import from:") FileEO file) throws Exception
   {
      String jsonText = JSON.readTextFile(file.fileValue().getAbsolutePath());
      AbstractListEO list = JSON.fromJsonList(new JSONObject(jsonText), hbmPersistor().getSession());
      Set set = new HashSet(list.getItems());
      hbmPersistor().saveMany(set);
   }
   
   @Cmd
   public AbstractListEO Open(CommandInfo cmdInfo)
   {
      return this;
   }

   
   // pick support
   private Association _association;
   public void setPickState(Association association)
   {
      _association = association;
      fireStateChanged();
   }
   public boolean isPickState() { return (_association != null); }

   private boolean _inContext = false;
   public void setPickState(Association association, boolean inContext)
   {
      setPickState(association);
      _inContext = inContext;
   }

   /**
    * @return whether the list is intended for use in a greater
    * context.  right now this is an evil hack that lets me conditionally
    * control whether list views should automatically close after
    * a 'pick' (in pick state) or not (so when i use this in a wizard
    * picking will not actually terminate my wizard!
    */
   public boolean isInContext() { return _inContext; }

   public void pick(ComplexEObject value)
   {
      _association.associate(value);
      if (!_inContext)
         setPickState(null);
   }

   public int hashCode()
   {
      int hash = 0;
      Object item;
      for (int i=0; i<_items.size(); i++)
      {
         item = _items.get(i);
         hash += 31 * item.hashCode();
      }
      return hash;
   }

   public boolean equals(Object obj)
   {
      if (this == obj) return true;
      if (! (obj instanceof AbstractListEO)) return false;

      AbstractListEO list = (AbstractListEO) obj;
      if (!list.getJavaClass().equals(getJavaClass())) return false;

      return _items.equals(list.getItems());
   }
   
   public String concat(String delimiter)
   {
      StringBuffer text = new StringBuffer("");
      EObject item;
      for (int i=0; i<_items.size()-1; i++)
      {
         item = (EObject) _items.get(i);
         text.append(item.toString()).append(delimiter).append(" ");
      }
      text.append(last().toString());
      return text.toString();
   }
   
   
   
   // convenience list-related methods..
   public static String join(AbstractListEO leo)
   {
      if (leo.getSize() == 0) return "";
      StringBuffer result = new StringBuffer();
      for (int i=0; i<leo.getSize()-1; i++)
      {
         Object item = leo.getElementAt(i);
         result.append(item.toString()).append(", ");
      }
      int lastIdx = leo.getSize() - 1;
      result.append(leo.getElementAt(lastIdx));
      return result.toString();
   }
   public static String join(List l)
   {
      return join(l, ", ");
   }
   public static String join(List l, String separator)
   {
      if (l.size() == 0) return "";
      StringBuffer result = new StringBuffer();
      for (int i=0; i<l.size()-1; i++)
      {
         Object item = l.get(i);
         result.append(item.toString()).append(separator);
      }
      int lastIdx = l.size() - 1;
      result.append(l.get(lastIdx));
      return result.toString();
   }
   
}
