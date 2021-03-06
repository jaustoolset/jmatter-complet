/*
 * Created on Nov 2, 2004
 */
package com.u2d.ui;

import java.awt.Insets;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * @author Eitan Suez
 */
public class LockToggle extends JToggleButton
{
   private static Icon ICON_LOCKED, ICON_UNLOCKED, ICON_LOCKED_ROLLOVER,
      ICON_UNLOCKED_ROLLOVER;
   static
   {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      java.net.URL imgurl = loader.getResource("images/locked-16.png");
      ICON_LOCKED = new ImageIcon(imgurl);
      imgurl = loader.getResource("images/unlocked-16.png");
      ICON_UNLOCKED = new ImageIcon(imgurl);
      imgurl = loader.getResource("images/locked-16_rollover.png");
      ICON_LOCKED_ROLLOVER = new ImageIcon(imgurl);
      imgurl = loader.getResource("images/unlocked-16_rollover.png");
      ICON_UNLOCKED_ROLLOVER = new ImageIcon(imgurl);
   }

   public LockToggle(Action action)
   {
      super(action);
      init();
   }
   public LockToggle(ActionListener listener)
   {
      super();
      init();
      addActionListener(listener);
   }

   private void init()
   {
      setIcon(ICON_LOCKED);
      setSelectedIcon(ICON_UNLOCKED);
      setBorderPainted(false);
      setMargin(new Insets(0,0,0,0));
      setContentAreaFilled(false);
      setFocusPainted(true);
      setRolloverEnabled(true);
      setRolloverIcon(ICON_LOCKED_ROLLOVER);
      setRolloverSelectedIcon(ICON_UNLOCKED_ROLLOVER);
   }
   
   public boolean isLocked() { return !isSelected(); }
}

