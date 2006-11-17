package com.u2d.field;

import com.u2d.model.Title;
import com.u2d.model.ComplexEObject;
import com.u2d.model.AbstractListEO;
import com.u2d.model.ComplexType;
import com.u2d.element.Field;
import javax.swing.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: eitan
 * Date: Nov 16, 2006
 * Time: 1:43:39 PM
 */
public interface AssociationStrategy
{
   Title title();
   ComplexEObject get();
   AbstractListEO getAsList();
   void set(ComplexEObject value);
   void associateList(List value);
   void associate(ComplexEObject value);
   void dissociate();
   void dissociateItem(ComplexEObject eo);
   ComplexEObject parent();
   Icon iconSm();
   boolean isEmpty();
   ComplexType type();
   boolean isEditableState(); 
   String getName(); 
   Field field();
}
