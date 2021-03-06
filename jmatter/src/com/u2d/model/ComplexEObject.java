/*
 * Created on Mar 2, 2005
 */
package com.u2d.model;

import java.awt.datatransfer.Transferable;
import java.awt.*;
import java.util.List;
import javax.swing.tree.TreeModel;
import com.u2d.element.Command;
import com.u2d.element.Field;
import com.u2d.field.Association;
import com.u2d.find.Searchable;
import com.u2d.pattern.State;
import com.u2d.persist.PersistorListener;
import com.u2d.pubsub.AppEventNotifier;
import com.u2d.view.ComplexEView;
import com.u2d.view.EView;
import com.u2d.type.atom.DateTime;

/**
 * @author Eitan Suez
 */
public interface ComplexEObject 
      extends EObject, AppEventNotifier,
              PersistorListener, Searchable,
              Transferable,
              Typed, PostChangeNotifier
                                          
{
   public ComplexType type();

   public void initialize();

   // state-related methods
   public void setStartState();
   public void restoreState();
   public void setReadState();
   public boolean isEditState();
   public void setEditState();
   public boolean isTransientState();
   public void setTransientState();
   public boolean isEditableState();
   public boolean isNullState();
   public void setNullState();
   public State getState();
   // note: was protected.  forced to make public after package restructuring
   public void setState(State state);
   public void pushState(State state);
   public void popState();

   // this is not so nice.  make all states extend from a base state that contains all transitions
   public void cancelTransition();

   public List childFields();
   // Note: do not confuse with field() (which returns wrapping field);
   public Field field(String propName);
   public Association association(String propName);
   public Command command(String commandName);
   public Command command(String commandName, State state);
   public String defaultCommandName();
   public Command defaultCommand();
   public void setEditor(Editor editor);
   public void clearEditor();
   
   /* ** View-Related ** */
   public ComplexEView getIconView();
   public ComplexEView getListItemView();
   public ComplexEView getFormView();
   public ComplexEView getTabBodyView();
   public ComplexEView getExpandableView();
   public ComplexEView getTreeView();

   public boolean hasCustomMainTabPanel();
   public EView mainTabPanel();

   // conveniences..
   public void save();
   public boolean doSave();
   public void delete();
   public void refresh();
   public AbstractListEO list();

   public void setValue(EObject value, boolean forCopy);
   
   public TreeModel treeModel();

   public Long getID();
   public void setID(Long id);
   public Long getVersion();
   public void setVersion(Long version);

   public DateTime getCreatedOn();

   public boolean isMeta();
   
   public String viewTitle();

   public Color colorCode();

}
