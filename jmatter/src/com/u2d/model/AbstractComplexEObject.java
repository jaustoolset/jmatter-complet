/*
 * Created on Jan 19, 2004
 */
package com.u2d.model;

import java.util.*;
import javax.swing.event.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.beans.*;
import java.awt.datatransfer.*;
import com.u2d.element.*;
import com.u2d.field.*;
import com.u2d.find.inequalities.IdentityInequality;
import com.u2d.pattern.*;
import com.u2d.pubsub.*;
import com.u2d.type.Choice;
import com.u2d.type.composite.LoggedEvent;
import com.u2d.type.atom.BooleanEO;
import com.u2d.type.atom.DateTime;
import com.u2d.type.atom.FileWEO;
import com.u2d.view.*;
import com.u2d.xml.XMLExport;
import com.u2d.reflection.CommandAt;
import com.u2d.reflection.ParamAt;
import com.u2d.json.JSON;
import com.u2d.list.CompositeList;

/**
 * @author Eitan Suez
 */
public abstract class AbstractComplexEObject extends AbstractEObject
            implements ComplexEObject
{
   protected ComplexType _type = null;

   protected transient State _transientState, _editState, _readState, _nullState;
   protected transient Map _stateMap = new HashMap();
   {
      _transientState = new TransientState();
      _stateMap.put(_transientState.getName(), _transientState);

      _editState = new EditState();
      _stateMap.put(_editState.getName(), _editState);

      _readState = new ReadState();
      _stateMap.put(_readState.getName(), _readState);

      _nullState = new NullState();
      _stateMap.put(_nullState.getName(), _nullState);
   }

   protected transient State _currentState;

   public static String[] readOnly = {"createdOn", "deleted", "deletedOn"};


   public AbstractComplexEObject()
   {
      _currentState = _nullState;
   }

   public void initialize()
   {
      Field field;
      for (int i=0; i<childFields().size(); i++)
      {
         field = (Field) childFields().get(i);
         if (field.isAggregate())
         {
            ((AbstractComplexEObject) field.get(this)).initialize();
         }
      }
   }

   public ComplexType type()
   {
      if (_type == null)
         _type = ComplexType.forObject(this);
      return _type;
   }

   public void onLoad()
   {
      restoreState();
      vmech().onMessage("Loaded "+title());
      
      Field child;
      for (int i=0; i<childFields().size(); i++)
      {
         child = (Field) childFields().get(i);
         if (child.isComposite() && child.isIndexed())
         {
            CompositeList list = (CompositeList) child.get(this);
            list.onLoad();
         }
      }
   }
   public void onDelete()
   {
      vmech().onMessage("Deleted "+title());
      setNullState();
      fireAppEventNotification("ONDELETE", this);
   }
   public void onBeforeSave()
   {
      Field child;
      for (int i=0; i<childFields().size(); i++)
      {
         child = (Field) childFields().get(i);
         if (child.isComposite() && child.isIndexed())
         {
            CompositeList list = (CompositeList) child.get(this);
            list.onBeforeSave();
         }
      }
      fireAppEventNotification("ONBEFORESAVE");
   }
   public void onSave()
   {
      vmech().onMessage("Saved "+title());
      // dillema here:  don't really know you're coming from an EditState Exit:
      popState();
      fireAppEventNotification("ONSAVE");
   }
   public void onBeforeCreate()
   {
      _createdOn.setValue(new Date());
      fireAppEventNotification("ONBEFORECREATE", this);
   }
   public void onCreate()
   {
      vmech().onMessage("Created "+title());
      setStartState();
      fireAppEventNotification("ONCREATE", this);
      type().fireAppEventNotification("ONCREATE", this);
   }

   // state-related methods

   public void setStartState()
   {
      setReadState();
      setState(startState(), true /* shallow */);
   }
   public void restoreState()
   {
      setReadState();
      setState(restoredState(), true /* shallow */);
   }

   // subclasses should override this:
   public State startState() { return _readState; }
   public State restoredState() { return _readState; }

   public void setReadState() { setState(_readState, true); }

   public boolean isEditState() { return _currentState == _editState; }
   public void setEditState() { setState(_editState); }

   public boolean isTransientState() { return _currentState == _transientState; }
   public void setTransientState() { setState(_transientState); }

   public boolean isEditableState()
   {
      return (_currentState instanceof EditableState);
   }

   public boolean isNullState() { return _currentState == _nullState; }
   public void setNullState() { setState(_nullState); }

   public State getState() { return _currentState; }


   // note: was protected.  forced to make public after package restructuring
   public void setState(State state) { setState(state, false); }

   protected void setState(State state, boolean shallow)
   {
      _currentState = (State) _stateMap.get(state.getName());
      if (_currentState == null)
         // an indication that state is a readstate subclass (as in user.lockedstate)
         // which will not exist on its fields when doing a deep setstate invocation.
         // in this case, the field's state should be set to readstate
      {
         _currentState = _readState;
      }
      fireStateChanged();

      if (!isEditableState()) clearEditor();

      if (shallow) return;

      for (Iterator itr = childFields().iterator(); itr.hasNext(); )
      {
         Field field = (Field) itr.next();
         field.setState(this, state);
      }
   }
   private Stack _stateStack = new Stack();
   protected void pushState(State state)
   {
      _stateStack.push(_currentState);
      setState(state);
   }
   protected void popState()
   {
      if (_stateStack.isEmpty())
      {
//         System.out.println("stack empty!");
         return;
      }
      State popped = (State) _stateStack.pop();
      setState(popped);
   }


   public void cancelTransition()
   {
      if (isEditableState())
         ((EditableState) getState()).Cancel(null);
   }


   private FieldParent fieldParent()
   {
      Field field = field();
      return (field instanceof FieldParent) ?
         (FieldParent) field : (FieldParent) type();
   }
   public List childFields() { return fieldParent().fields(); }
   public Field field(String propName) { return fieldParent().field(propName); }

   // convenience for retrieving a list of fields by name..
   public List<Field> fieldSublist(String[] fieldnameList)
   {
      List<Field> sublist = new ArrayList<Field>();
      for (int i=0; i<fieldnameList.length; i++)
      {
         sublist.add(field(fieldnameList[i]));
      }
      return sublist;
   }


   protected Map _associations = new HashMap();
   public Association association(String propName)
   {
      Field field = field(propName);
      if (field == null)
      {
         throw new IllegalArgumentException("No such property: "+propName+ " on field parent "+fieldParent());
      }
      if ( !field.isAssociable() )
      {
         throw new IllegalArgumentException("Property "+propName+" is not associable.");
      }

      Associable associable = (Associable) field;
      if (_associations.get(propName) == null)
      {
         _associations.put(propName, associable.association(this));
      }

      return (Association) _associations.get(propName);
   }

   public Command command(String commandName)
   {
      return command(commandName, _readState);
   }
   public Command command(String commandName, State state)
   {
      Onion commands = type().commands(state);
      return (Command) commands.find(Command.finder(commandName));
   }
   public Onion commands()
   {
      return type().commands(_currentState);
   }
   public String defaultCommandName() { return "Open"; }
   public Command defaultCommand() { return command(defaultCommandName()); }


   private transient Editor _editor = null;

   public void setEditor(Editor editor)
   {
      _editor = editor;
      vmech().setEditable(_editor, true);
   }

   public void clearEditor()
   {
      if (_editor == null) return;
      vmech().setEditable(_editor, false);
      _editor = null;
   }


   public javax.swing.Icon iconSm()
   {
      return IconLoader.instanceIcon(this, "16", ComplexType.DEFAULTICON_SM);
   }

   public javax.swing.Icon iconLg()
   {
      return IconLoader.instanceIcon(this, "32", ComplexType.DEFAULTICON_LG);
   }

   public abstract Title title();

   public String toString()
   {
//      List fields = fields();
//      Field field = null;
//      StringBuffer text = new StringBuffer("");
//      for (int i=0; i<fields.size(); i++)
//      {
//         field = (Field) fields.get(i);
//         text.append(field.getPath()+": "+field.get(this).toString()+"; ");
//      }
//      return text.toString();
      return title().toString();
   }

   public boolean isEmpty()
   {
      Field field = null;
      for (Iterator itr = childFields().iterator(); itr.hasNext(); )
      {
         field = (Field) itr.next();
         
         if ( "createdOn".equals(field.name()) || "deleted".equals(field.name())
               || "deletedOn".equals(field.name()))
            continue;

         if (!field.isEmpty(this))
            return false;
      }
      return true;
   }

   public boolean equals(Object obj)
   {
      if (obj == null) return false;
      if (obj == this) return true;
      if (!(obj instanceof AbstractComplexEObject))
         return false;
      if (!obj.getClass().equals(getClass()))
      {
           return false;
      }
      ComplexEObject ceo = (ComplexEObject) obj;
      if (ceo.childFields().size() != childFields().size()) return false;
      List fields = childFields();
      Field field = null;
      for (int i=0; i<fields.size(); i++)
      {
         field = (Field) fields.get(i);
         if (!field.get(this).equals(field.get(ceo)))
         {
            return false;
         }
      }
      return true;
   }

   private boolean clsNamesSameExceptCGILibEnhancer(Object obj)
   {
      String clsName1 = cleanCGILibEnhancer(obj);
      String clsName2 = cleanCGILibEnhancer(this);
      return (clsName1.equals(clsName2));
   }
   public static String cleanCGILibEnhancer(Object obj)
   {
      String clsName = obj.getClass().getName();
      int idx = clsName.indexOf("$$EnhancerByCGLIB$$");
      if (idx > 0) clsName = clsName.substring(0, idx);
      return clsName;
   }


   /* ** View-Related ** */
   public ComplexEView getIconView() { return vmech().getIconView(this); }
   public ComplexEView getListItemView() { return vmech().getListItemView(this); }
   public ComplexEView getFormView() { return vmech().getFormView(this); }
   public ComplexEView getTabBodyView() { return vmech().getTabBodyView(this); }
   public ComplexEView getExpandableView() { return vmech().getExpandableView(this); }
   public ComplexEView getTreeView() { return vmech().getTreeView(this); }

   public EView getView() { return getListItemView(); }

   public EView getMainView()
   {
      return vmech().getAlternateView(this, new String[] {"formview", "collapsedview", "omniview"});
   }


   /* ** PropertyChangeSupport "Support" ** */
   protected transient SwingPropertyChangeSupport _changeSupport = new SwingPropertyChangeSupport(this);

   public void firePropertyChange(String propertyName, Object oldValue, Object newValue)
   {
      _changeSupport.firePropertyChange(propertyName, oldValue, newValue);
   }
   public void firePropertyChange(PropertyChangeEvent event)
   {
      _changeSupport.firePropertyChange(event);
   }
   public void firePropertyChange(String propertyName, int oldValue, int newValue)
   {
      firePropertyChange(propertyName, new Integer(oldValue), new Integer(newValue));
   }
   public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue)
   {
      firePropertyChange(propertyName, new Boolean(oldValue), new Boolean(newValue));
   }

   public void addPropertyChangeListener(PropertyChangeListener listener)
   {
      _changeSupport.addPropertyChangeListener(listener);
   }
   public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
   {
      _changeSupport.addPropertyChangeListener(propertyName, listener);
   }

   public void removePropertyChangeListener(PropertyChangeListener listener)
   {
      _changeSupport.removePropertyChangeListener(listener);
   }
   public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener)
   {
      _changeSupport.removePropertyChangeListener(propertyName, listener);
   }

   /*
   * note this code is from the perspective of the ui
   * it gives editor (ui) a chance to transfer entered
   * data to the object and then validates the data.
   * cancel restores a saved copy.
   * 
   * @return success
   */
   public boolean doSave()
   {
      int errorCount = _editor.transferValue();
      if (errorCount > 0)
      {
         String plural = (errorCount == 1) ? "" : "s";
         fireValidationException("[Syntax errors in "+errorCount+" form field"+plural+".]", true);
         return false;
      }

      errorCount = validate();
      if (errorCount > 0)
      {
         String plural = (errorCount == 1) ? "" : "s";
         fireValidationException("["+errorCount+" validation error"+plural+".]", true);
         return false;
      }

      fireValidationException("");  // reset any validation messages from last attempt

      // now we can do the actual save against the persistence mechanism..
      persistor().save(this);
      return true;
   }

   public int validate()
   {
      if (field() == null)
         return type().validate(this);
      return 0;
   }


   // convenience..
   public void save()
   {
      persistor().save(this);
   }
   public void delete()
   {
      persistor().delete(this);
   }

   private transient ComplexEObject _copy;
   public void saveCopy()
   {
      _copy = (ComplexEObject) makeCopy();
   }

   // replaces values (deep) but not references (to preserve view integrity)
   public void restoreCopy()
   {
      transferCopy(this, _copy, true);
   }

   private void transferCopy(ComplexEObject target, ComplexEObject copy, boolean forCopy)
   {
      List fields = target.type().fields();
      Field field = null;
      for (int i=0; i<fields.size(); i++)
      {
         field = (Field) fields.get(i);
         EObject valueCopy = field.get(copy);

         if (field.isAggregate())
         {
            ComplexEObject value = (ComplexEObject) field.get(target);
            transferCopy(value, (ComplexEObject) valueCopy, forCopy);
         }
         else
         {
            if (forCopy && (field.isAssociation() || field.isIndexed()) )
               continue;  // omit associations because in copy operations
                          // causes original to lose association

            field.set(target, valueCopy);
         }
      }
   }

   // deep copy
   public EObject makeCopy()
   {
      ComplexEObject target = type().instance();
      target.setValue(this, true);
      return target;
   }

   public void setValue(EObject value)
   {
      setValue(value, false);
   }

   public void setValue(EObject value, boolean forCopy)
   {
      if (value == null)
         throw new IllegalArgumentException("setValue does not accept null");
      if (!(value instanceof ComplexEObject))
         throw new IllegalArgumentException("Invalid type on set (" +
               value.getClass().getName() + "); " +
               "must be ComplexEObject");

      ComplexEObject ceo = (ComplexEObject) value;
      Class fromClass = value.getClass();
      Class thisClass = this.getClass();
      if (!thisClass.isAssignableFrom(fromClass))
      {
         throw new IllegalArgumentException("Cannot set value of type "+fromClass.getName() +
               " onto an object of type "+thisClass.getName());
      }

      transferCopy(this, ceo, forCopy);
   }

   /* ** State Related Code ** */

   public class TransientState extends EditableState
   {
      @CommandAt(mnemonic='s')
      public String Save(CommandInfo cmdInfo)
      {
//         System.out.println("Transient.save");
         try
         {
            if (doSave())
               log(LoggedEvent.INFO, cmdInfo.getCommand(), "New Object Created/Persisted");
            return null;
         }
         catch (org.hibernate.exception.ConstraintViolationException ex)
         {
            log(LoggedEvent.ERROR, cmdInfo.getCommand(), DUPLICATE_KEY_CONSTRAINT_ERROR_MSG);
            fireValidationException(DUPLICATE_KEY_CONSTRAINT_ERROR_MSG);
            return DUPLICATE_KEY_CONSTRAINT_ERROR_MSG;
         }
         catch (org.hibernate.StaleObjectStateException ex)
         {
            fireValidationException(STALE_OBJECT_MSG);
            return null;
         }
      }
      @CommandAt(mnemonic='c')
      public void Cancel(CommandInfo cmdInfo)
      {
//         System.out.println("Transient.cancel");
         setNullState();
      }
      @CommandAt
      public void Copy(CommandInfo cmdInfo)
      {
         ComplexEObject copy = (ComplexEObject) makeCopy();
         type().bufferCopy(copy);
      }
      @CommandAt
      public void Paste(CommandInfo cmdInfo)
      {
         ComplexEObject copy = type().bufferCopy();
         if (copy == null)
         {
            return;
         }
         transferCopy(AbstractComplexEObject.this, copy, true);
      }
      @CommandAt(mnemonic='l')
      public void SaveAndClose(CommandInfo cmdInfo)
      {
         if (doSave())
         {
            vmech().dismiss(cmdInfo.getSource());
         }
      }
   }

   private static String DELETE_CONSTRAINT_ERROR_MSG =
         "Cannot delete objects still referenced by other objects in the system";
   private static String DUPLICATE_KEY_CONSTRAINT_ERROR_MSG =
         "Cannot save object:  duplicate value for a unique field";
   private static String STALE_OBJECT_MSG = 
         "Object has been modified by another user;  please refresh object";


   public class ReadState extends State
   {
      @CommandAt
      public ComplexEObject Open(CommandInfo cmdInfo)
      {
         refresh();
         return AbstractComplexEObject.this;
      }
      
      @CommandAt
      public void Copy(CommandInfo cmdInfo)
      {
         ComplexEObject copy = (ComplexEObject) makeCopy();
         type().bufferCopy(copy);
      }

      @CommandAt(mnemonic='e')
      public ComplexEObject Edit(CommandInfo cmdInfo)
      {
//         System.out.println("Read.edit");
         refresh();

         saveCopy();
         pushState(_editState);

         if (cmdInfo.getSource() instanceof Editor)
         {
            setEditor((Editor) cmdInfo.getSource());
            return null;
         }
         return AbstractComplexEObject.this;
      }

      @CommandAt(isSensitive = true)
      public String Delete(CommandInfo cmdInfo)
      {
//         System.out.println("Read.delete");
         try
         {
            delete();
            return null;
         }
         catch (org.hibernate.exception.ConstraintViolationException ex)
         {
            log(LoggedEvent.ERROR, cmdInfo.getCommand(), DELETE_CONSTRAINT_ERROR_MSG);
            return DELETE_CONSTRAINT_ERROR_MSG;
         }
      }

      @CommandAt
      public void ExportToXML(CommandInfo cmdInfo) throws Exception
      {
         XMLExport.export(cmdInfo, AbstractComplexEObject.this);
      }
      @CommandAt
      public String ExportToJSON(CommandInfo cmdInfo, @ParamAt("Save to:") FileWEO file) throws Exception
      {
         JSON.writeJson(file.fileValue(), AbstractComplexEObject.this);
         return file.fileValue().getName() + " created.";
      }
      
      @CommandAt(mnemonic='r')
      public void Refresh(CommandInfo cmdInfo)
      {
         refresh();
      }
      
   }


   public static String[] commandOrderEditState =
                           {"Save", "Cancel", "Copy", "Paste"};
   public static String[] commandOrderTransientState =
                           {"Save", "Cancel", "Copy", "Paste"};
   public static String[] commandOrderReadState =
                           {"Open", "Edit", "Delete", "Copy"};


   public class EditState extends EditableState
   {
      @CommandAt(mnemonic='s')
      public String Save(CommandInfo cmdInfo)
      {
//         System.out.println("Edit.save");
         try
         {
            if (doSave())
               log(LoggedEvent.INFO, cmdInfo.getCommand(), "Object updated");
            return null;
         }
         catch (org.hibernate.exception.ConstraintViolationException ex)
         {
            log(LoggedEvent.ERROR, cmdInfo.getCommand(), DUPLICATE_KEY_CONSTRAINT_ERROR_MSG);
            return DUPLICATE_KEY_CONSTRAINT_ERROR_MSG;
         }
         catch (org.hibernate.StaleObjectStateException ex)
         {
            fireValidationException(STALE_OBJECT_MSG);
            return null;
         }
      }
      @CommandAt(mnemonic='c')
      public void Cancel(CommandInfo cmdInfo)
      {
//         System.out.println("Edit.cancel");
         restoreCopy();
         popState();
      }
      @CommandAt
      public void Copy(CommandInfo cmdInfo)
      {
         ComplexEObject copy = (ComplexEObject) makeCopy();
         type().bufferCopy(copy);
      }
      @CommandAt
      public void Paste(CommandInfo cmdInfo)
      {
         ComplexEObject copy = type().bufferCopy();
         if (copy == null) return;
         transferCopy(AbstractComplexEObject.this, copy, true);
      }
      @CommandAt(mnemonic='l')
      public void SaveAndClose(CommandInfo cmdInfo)
      {
         if (doSave())
         {
            vmech().dismiss(cmdInfo.getSource());
         }
      }
   }


   public class NullState extends State {}


   // Part of contract with persistence mechanism (should be formalized via an interface):
   private Long _id;
   public Long getID() { return _id; }
   public void setID(Long id) { _id = id; }
   
   // Hibernate version field, for Optimistic Locking
   private Long _version;
   public Long getVersion() { return _version; }
   private void setVersion(Long version) { _version = version; }
   

   protected final DateTime _createdOn = new DateTime(new Date());
   public DateTime getCreatedOn() { return _createdOn; }

   private final BooleanEO _deleted = new BooleanEO(false);
   public BooleanEO getDeleted() { return _deleted; }

   private final DateTime _deletedOn = new DateTime(new Date());
   public DateTime getDeletedOn() { return _deletedOn; }

   //  Deleted Data Storage Feature:
   //  =============================
   // - filter out deleted items in listings, search fields, etc..
   // - special folder:  deleted items (ordered by deletion date)
   // - an undelete feature (removes deleted flag)
   // - rework 'delete' implementation
   // - a script to really delete deleted items, flexible enough where
   //     can use deletion date as a cutoff mechanism (e.g. "really delete all 
   //     deleted items that were deleted more than a week ago")
   //     


   // ========== implementation of Transferrable interface for dnd ===============
   public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
   {
      if (!isDataFlavorSupported(flavor))
          throw new UnsupportedFlavorException(flavor);
      return this;
   }

   public DataFlavor[] getTransferDataFlavors()
   {
      return new DataFlavor[] { type().getFlavor() };
   }

   public boolean isDataFlavorSupported(DataFlavor f)
   {
      // was:
      //  return f.equals(type().getFlavor());
      
      // now: make sure that type() is assignable from flavor's type
      return type().getJavaClass().isAssignableFrom(f.getRepresentationClass());
   }



   // ====  app event support ...

   private transient AppEventSupport _support = new AppEventSupport(this);
   public void addAppEventListener(String evtType, AppEventListener l)
   {
      _support.addAppEventListener(evtType, l);
   }
   public void removeAppEventListener(String evtType, AppEventListener l)
   {
      _support.removeAppEventListener(evtType, l);
   }
   public void fireAppEventNotification(String evtType)
   {
      _support.fireAppEventNotification(evtType);
   }
   public void fireAppEventNotification(String evtType, Object target)
   {
      _support.fireAppEventNotification(evtType, target);
   }



   // == searchable implementation..
   public List getInequalities()
   {
      if (field() == null)
         return new IdentityInequality(type()).getInequalities();
      return new IdentityInequality(field()).getInequalities();
   }


   // == misc..
   public ComplexEObject createInstance(Class typeClass)
   {
      return ComplexType.forClass(typeClass).instance();
   }


   // ==== tree model..

   private TreeModel _treeModel;
   public TreeModel treeModel()
   {
      if (_treeModel == null) _treeModel = new ETreeModel();
      return _treeModel;
   }

   class ETreeModel implements TreeModel
   {
      public Object getRoot()
      {
         return AbstractComplexEObject.this;
      }
      public Object getChild(Object parent, int index)
      {
         if (parent instanceof AbstractListEO)
         {
            AbstractListEO leo = (AbstractListEO) parent;
            return leo.getElementAt(index);
         }

         ComplexEObject ceo = (ComplexEObject) parent;
         Field field = null;
         int count = 0;
         for (int i=0; i<ceo.childFields().size(); i++)
         {
            field = (Field) ceo.childFields().get(i);
            if (field.isAtomic() || field.isChoice())
               continue;
            if (count == index)
            {
               return field.get(ceo);
            }
            count++;
         }

         throw new RuntimeException("EObject TreeModel Failed to find child for parent: "
               + parent + " at index " + index);

      }
      public int getChildCount(Object parent)
      {
         if (parent instanceof AbstractListEO)
         {
            AbstractListEO leo = (AbstractListEO) parent;
            return leo.getSize();
         }

         ComplexEObject ceo = (ComplexEObject) parent;

         int count = 0;
         Field field = null;
         for (int i=0; i<ceo.childFields().size(); i++)
         {
            field = (Field) ceo.childFields().get(i);
            if (field.isAtomic() || field.isChoice())
               continue;
            count++;
         }
         return count;

      }
      public boolean isLeaf(Object node)
      {
         if (node instanceof AbstractListEO) return false;
         if (!(node instanceof ComplexEObject)) return true;
         if (node instanceof Choice) return true;
         if (node instanceof NullComplexEObject) return true;
         return (getChildCount(node) == 0);
      }

      public int getIndexOfChild(Object parent, Object child)
      {
         if (parent instanceof AbstractListEO)
         {
            AbstractListEO leo = (AbstractListEO) parent;
            for (int i=0; i<leo.getSize(); i++)
            {
               if (child.equals(leo.getElementAt(i)))
                  return i;
            }
            return -1;
         }

         ComplexEObject ceo = (ComplexEObject) parent;

         int index = 0;
         Field field = null;
         for (int i=0; i<ceo.childFields().size(); i++)
         {
            field = (Field) ceo.childFields().get(i);
            if (field.isAtomic() || field.isChoice())
               continue;

            if (child.equals(field.get(ceo)))
               return index;

            index++;
         }
         return -1;

      }

      protected EventListenerList listenerList = new EventListenerList();

      public void addTreeModelListener(TreeModelListener listener)
      {
         listenerList.add(TreeModelListener.class, listener);
      }
      public void removeTreeModelListener(TreeModelListener listener)
      {
         listenerList.remove(TreeModelListener.class, listener);
      }
      public void valueForPathChanged(TreePath path, Object newValue)
      {
         ComplexEObject ceo = null;
         for (int i=0; i<path.getPathCount(); i++)
         {
            ceo = (ComplexEObject) path.getPathComponent(i);
            ceo.fireStateChanged();
         }
      }
   }

   public boolean isMeta() { return false; }

   // convenience..
   public void log(String typeString, EOCommand cmd, String msg)
   {
      app().log(typeString, cmd, msg);
   }
   
   
   public void refresh()
   {
      hbmPersistor().getSession().refresh(this);
   }
   

}
