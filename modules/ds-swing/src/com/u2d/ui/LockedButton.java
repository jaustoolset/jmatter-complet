/*
 * Created on Nov 2, 2004
 */
package com.u2d.ui;

import java.awt.event.*;
import javax.swing.*;
import java.awt.*;

/**
 * @author Eitan Suez
 */
public class LockedButton extends NormalButton implements ActionListener
{
   private LockToggle _lock;

   public LockedButton(String caption)
   {
      super(caption);
      initialize();
   }
   
   public LockedButton(Action action)
   {
      super(action);
      initialize();
   }
   
   private void initialize()
   {
      setHorizontalAlignment(SwingConstants.CENTER);
      _lock = new LockToggle(this);
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
      Dimension min = new Dimension(0,0);
      Dimension preferred = getPreferredSize();
      Dimension max = new Dimension(500,3);
      Box.Filler elastic = new Box.Filler(min, preferred, max);
      add(elastic);
      add(_lock);
      setOpaque(false);
      super.setEnabled(false);
   }

   /*
    * overridden because otherwise getMinimumSize() returns null, causing
    *  a nullpointerexception, when the button text contains html
    * see basicbuttonui line 344.  d.width is referenced when d == null
    * because BasicGraphicUtils.getPreferredButtonSize returns null
    */
   @Override
   public Dimension getMinimumSize()
   {
      return new Dimension(10,10);
   }

   public void setEnabled(boolean enabled)
   {
      if (_lock == null)
      {
         super.setEnabled(enabled);
         return;
      }
      _lock.setEnabled(enabled);
      super.setEnabled(!_lock.isLocked() && enabled);
   }
   
   public void actionPerformed(ActionEvent evt)
   {
      super.setEnabled(!_lock.isLocked());
   }
   
   public Insets getInsets()
   {
      return new Insets(2,8,2,2);
   }
   
}
