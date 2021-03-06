package com.u2d.view.swing;

import com.u2d.model.Editor;
import com.u2d.model.EObject;
import com.u2d.model.ComplexEObject;
import com.u2d.model.AtomicEObject;
import com.u2d.view.EView;
import com.u2d.view.swing.atom.AtomicViewReadOnly;
import com.u2d.element.Field;
import com.u2d.element.Command;
import com.u2d.field.CompositeField;
import com.u2d.field.Association;
import com.u2d.ui.UIUtils;
import com.jeta.forms.components.panel.FormPanel;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.beans.PropertyChangeEvent;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: Eitan Suez
 * Date: Apr 29, 2008
 * Time: 12:36:48 PM
 */
public abstract class AbeilleForm extends FormPane implements IFormView
{
   protected java.util.List<EView> _views = new ArrayList<EView>();

   protected FormPanel formPanel()
   {
      String clsName = getEObject().getClass().getName();
      String formName = clsName.replace('.', File.separatorChar) + ".jfrm";
      return formPanel(formName);
   }
   protected FormPanel formPanel(String resourcePath)
   {
      FormPanel formPanel = new FormPanel(resourcePath);
      formPanel.setOpaque(false);
      return formPanel;
   }

   protected JComponent getView(EObject eo)
   {
      EView view = eo.getMainView();
      _views.add(view);
      return (JComponent) view;
   }
   protected AtomicViewReadOnly getAtomicViewReadOnly(AtomicEObject aeo)
   {
      AtomicViewReadOnly view = new AtomicViewReadOnly(aeo);
      _views.add(view);
      return view;
   }
   protected JComponent getAssociationView(Association association)
   {
      EView view = SwingViewMechanism.getInstance().getAssociationView(association);
      _views.add(view);
      return (JComponent) view;
   }
   protected JComponent getCommandView(Command cmd)
   {
      return getCommandView(cmd, eo());
   }
   protected JComponent getCommandView(Command cmd, EObject eo)
   {
      EView view = SwingViewMechanism.getInstance().getCommandView(cmd, eo);
      _views.add(view);
      return (JComponent) view;
   }

   public void detach()
   {
      stopListeningForValidations();
      _vPnls.clear();

      for (EView view : _views)
      {
         view.detach();
      }
   }

   public void stateChanged(ChangeEvent e) { }
   public void propertyChange(PropertyChangeEvent evt) { }

   private ComplexEObject eo() { return (ComplexEObject) getEObject(); }

   protected boolean _editable = false;
   public void setEditable(boolean editable)
   {
      _editable = editable;

      if (_editable)
      {
         listenForValidations();
      }
      else
      {
         stopListeningForValidations();
         resetValidations();
      }
      
      for (EView view : _views)
      {
         if (!(view instanceof Editor)) continue;

         EObject eo = view.getEObject();
         if (eo == null) return;  // see comment in FormView
         Field field = eo.field();

         if (field != null && field.isComposite() && editable && !field.isIndexed())
         {
            CompositeField cfield = ((CompositeField) field);
            if (cfield.isReadOnly() ||
                  ( cfield.isIdentity() && !(eo().isTransientState()) )
               )
               continue;
         }

         ((Editor) view).setEditable(editable);
      }
   }
   public boolean isEditable() { return _editable; }

   public int transferValue()
   {
      int count = 0;
      for (EView view : _views)
      {
         if (view instanceof Editor)
         {
            Field field = view.getEObject().field();

            if (field != null && field.isComposite() && !field.isIndexed())
            {
               CompositeField cfield = ((CompositeField) field);
               if (cfield.isReadOnly() ||
                     ( cfield.isIdentity() && !(eo().isTransientState()) )
                  )
                  continue;
            }

            count += ((Editor) view).transferValue();
         }
      }

      return count;
   }

   public int validateValue()
   {
      return eo().validate();
   }

   public boolean isMinimized() { return false; }

   public void focusField()
   {
      UIUtils.focusFirstEditableField(AbeilleForm.this);
   }

   // validation-related work..
   private java.util.Collection<ValidationNoticePanel> _vPnls = new HashSet<ValidationNoticePanel>();

   protected ValidationNoticePanel getValidationPanel(EObject eo)
   {
      ValidationNoticePanel vPnl = new ValidationNoticePanel(eo, eo());
      _vPnls.add(vPnl);
      return vPnl;
   }

   private void resetValidations()
   {
      for (ValidationNoticePanel vPnl : _vPnls)
      {
         vPnl.reset();
      }
   }
   private void listenForValidations()
   {
      for (ValidationNoticePanel vPnl : _vPnls)
      {
         vPnl.startListening();
      }
   }
   private void stopListeningForValidations()
   {
      for (ValidationNoticePanel vPnl : _vPnls)
      {
         vPnl.stopListening();
      }
   }

   
}
