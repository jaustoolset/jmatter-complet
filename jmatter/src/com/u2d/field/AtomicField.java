/*
 * Created on Jan 21, 2004
 */
package com.u2d.field;

import java.beans.*;
import java.text.ParseException;

import com.u2d.model.*;
import com.u2d.pattern.*;
import com.u2d.validation.Required;
import com.u2d.view.*;
import com.u2d.type.atom.StringEO;

/**
 * @author Eitan Suez
 */
public class AtomicField extends CompositeField
{
   public static String[] fieldOrder = {"name", "label", "required", "defaultValue", 
         "mnemonic", "description"};
   public static String[] readOnly = {"name"};

   protected final StringEO _defaultValue = new StringEO();
   
   public AtomicField() {}

   public AtomicField(FieldParent parent, PropertyDescriptor descriptor)
   {
      super(parent, descriptor);
   }
   
   public AtomicField(FieldParent parent, String name) throws IntrospectionException
   {
      super(parent, name);
   }

   public EView getView(ComplexEObject parent)
   {
      EObject value = get(parent);
      return value.getView();
   }
   
   public StringEO getDefaultValue() { return _defaultValue; }

   public AtomicEObject parseValue(String stringValue) throws java.text.ParseException
   {
      AtomicEObject aeo = newInstance();
      aeo.parseValue(stringValue);
      return aeo;
   }
   
   private AtomicEObject newInstance()
   {
      try
      {
         return (AtomicEObject) getJavaClass().newInstance();
      }
      catch (IllegalAccessException ex)
      {
         System.err.println(ex.getMessage());
         ex.printStackTrace();
      }
      catch (InstantiationException ex)
      {
         System.err.println(ex.getMessage());
         ex.printStackTrace();
      }
      return null;
   }
   
   public void setState(ComplexEObject parent, State state)
   {
      EObject value = get(parent);
      if (parent.isTransientState())
      {
         AtomicEObject aeo = ((AtomicEObject) value);
         if (aeo.isEmpty() && !_defaultValue.isEmpty())
         {
            try
            {
               aeo.parseValue(_defaultValue.stringValue());
            }
            catch (ParseException e)
            {
               System.err.println("Failed to parse default value " +
                     "(" + _defaultValue + ") for field (" + this + ")");
               e.printStackTrace();
            }
         }
      }
      value.fireStateChanged();
   }

   
   public int validate(ComplexEObject parent)
   {
      EObject value = get(parent);
      Required required = getRequired(parent);
      if (required.isit() && value.isEmpty())
      {
         value.fireValidationException(required.getMsg());
         return 1;
      }
      
      if (!value.isEmpty())
      {
         int err = value.validate();
         if (err > 0) return err;
      }
      
      value.fireValidationException("");  // to reset the msg
      return 0;
   }


   public String getSortPropertyName() { return getCleanPath(); }
   public boolean isSortable() { return true; }

}
