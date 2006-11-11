package com.u2d.restrict;

import com.u2d.model.ComplexType;

/**
 * Created by IntelliJ IDEA.
 * User: eitan
 * Date: Nov 9, 2006
 * Time: 11:39:56 AM
 */
public class CreationRestriction extends CommandRestriction
{
   private ComplexType _type;
   
   public CreationRestriction() {}

   public CreationRestriction(ComplexType type)
   {
      _type = type;
      _member = _type.command("New");
      // TODO: this is not enough.  List.command("New") must also be
      //  restricted
   }
}
