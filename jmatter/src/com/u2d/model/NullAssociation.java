/*
 * Created on May 13, 2004
 */
package com.u2d.model;

import com.u2d.element.CommandInfo;
import com.u2d.element.Field;
import com.u2d.element.Command;
import com.u2d.field.Association;
import com.u2d.pattern.Onion;
import com.u2d.view.View;
import com.u2d.list.RelationalList;
import com.u2d.reflection.Cmd;

import java.util.Iterator;

/**
 * @author Eitan Suez
 */
public class NullAssociation extends NullComplexEObject
{
   private Association _association;

   public NullAssociation(RelationalList leo)
   {
      this(leo.field(), leo.parentObject());
   }

   public NullAssociation(Field field, ComplexEObject parent)
   {
      super(field.fieldtype());
      setField(field, parent);
      _association = parent.association(field.name());
      propagateCmdRestrictions();
   }

   public NullAssociation(Association association)
   {
      super(association.type());
      setField(association.field(), association.parent());
      _association = association;
      propagateCmdRestrictions();
   }

   private void propagateCmdRestrictions()
   {
      for (Iterator itr = cmds2.deepIterator(); itr.hasNext(); )
      {
         Command command = (Command) itr.next();
         Command typeCommand = _type.command(command.name());
         if (typeCommand != null)  // TODO:  revisit  // related to removing typecommand New from ComplexType.
         {
            command.applyRestriction(typeCommand.restriction());
         }
      }
   }

   public boolean isIndexedAssociation() { return _association.field().isIndexed(); }
   public AbstractListEO getAsList() { return _association.getAsList(); }

//   public Title title() { return _association.title(); }
//   public boolean isEmpty() { return _association.isEmpty(); }

   // override @New to also do the binding/association
   @Cmd
   public ComplexEObject New(CommandInfo cmdInfo)
   {
      return New(cmdInfo, _type);
   }

   @Cmd
   public ComplexEObject New(CommandInfo cmdInfo, ComplexType type)
   {
      final ComplexEObject ceo = type.New(cmdInfo);
      _association.set(ceo);
      return ceo;
   }

   // if you have any 1+ parameter actions defined where at least
   // one parameter is of type complextype, then you must provide
   // this accompanying method, which is invoked reflectively by
   // paramslistview
   public ComplexType baseType()
   {
      return field().fieldtype().baseType();
   }
   public boolean isAbstract() { return field().isAbstract(); }



   @Cmd
   public AbstractListEO Browse(CommandInfo cmdInfo)
   {
      AbstractListEO leo = _type.Browse(cmdInfo);
      leo.setPickState(_association);
      return leo;
   }
   @Cmd
   public View Find(CommandInfo cmdInfo)
   {
      return vmech().getFindView2(_type, _association);
   }

   //    @Cmd
   //   public void Association(Cmd cmdInfo)
//   {
//      type().Paste(cmdInfo);
//   }

   public void set(ComplexEObject ceo)
   {
      _association.set(ceo);
   }
   public void associate(ComplexEObject ceo)
   {
      _association.associate(ceo);
   }

   public ComplexType type() { return _association.type(); }

   public Onion commands() { return cmds2; }

   public Onion filteredCommands()
   {
      return commands().filter(Command.commandFilter(this));
   }

   static Onion cmds2;
   static
   {
      cmds2 = Harvester.simpleHarvestCommands(NullAssociation.class,
                                              new Onion(), false, null, true /* shallow */);
   }

}
