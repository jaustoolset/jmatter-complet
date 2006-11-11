/*
 * Created on Jan 19, 2004
 */
package com.u2d.view.swing;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.Cursor;
import com.u2d.element.Command;
import com.u2d.view.*;
import com.u2d.app.Context;

/**
 * Adapter for javax.swing.Action
 * 
 * @author Eitan Suez
 */
public class CommandAdapter extends AbstractAction
{
   private Command _command;
   private Object _value;
   private EView _source;  // the view from which the command was invoked

   public CommandAdapter(Command command, EView source)
   {
      if (command == null)
         throw new IllegalArgumentException("CommandAdapter cannot be constructed for a Null command");

      _command = command;
      _source = source;

      putValue(Action.NAME, _command.label());
      putValue(Action.ACTION_COMMAND_KEY, _command.name());
      putValue(Action.MNEMONIC_KEY, new Integer(_command.mnemonic()));
   }

   public CommandAdapter(Command command, Object value, EView source)
   {
      this(command, source);
      attach(value);
   }

   public void attach(Object value) { _value = value; }
   public void detach() { _value = null; }

   private static final Cursor WAITCURSOR = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);

   public void actionPerformed(ActionEvent evt)
   {
      Context.getInstance().swingvmech().setCursor(WAITCURSOR);

      new Thread()
      {
         public void run()
         {
            try
            {
               //System.out.println("cmdAdapter:: executing command, passing source: "+_source);
               _command.execute(_value, _source);
            }
            catch (java.lang.reflect.InvocationTargetException ex)
            {
               Context.getInstance().swingvmech().displayFrame(new ExceptionFrame(ex));
            }
            finally
            {
               SwingUtilities.invokeLater(new Runnable()
               {
                  public void run()
                  {
                     Context.getInstance().swingvmech().setCursor(Cursor.getDefaultCursor());
                  }
               });
            }
         }
      }.start();
   }

}
