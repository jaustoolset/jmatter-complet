/*
 * Created on Feb 9, 2004
 */
package com.u2d.view.swing;

import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.lang.reflect.*;
import com.u2d.ui.desktop.CloseableJInternalFrame;
import com.u2d.css4swing.style.ComponentStyle;

/**
 * @author Eitan Suez
 */
public class ExceptionFrame extends CloseableJInternalFrame
{
   public ExceptionFrame(Throwable ex)
   {
      super("Exception", true, true, false, true);
      JPanel contentPane = (JPanel) getContentPane();
      contentPane.setLayout(new BorderLayout());
      
      if (ex instanceof InvocationTargetException)
      {
         InvocationTargetException indirect = (InvocationTargetException) ex;
         ex = indirect.getCause();
      }

      JLabel msgLabel = new JLabel("Exception:  " + ex.getMessage());
      Font font = msgLabel.getFont().deriveFont(Font.BOLD, 14.0f);
      msgLabel.setFont(font);

      StringWriter sw = new StringWriter();
      ex.printStackTrace(new PrintWriter(sw));
      JTextArea details = new CustomTextArea(sw.toString());
      JScrollPane detailsPane = new JScrollPane(details);
      
      contentPane.add(new GenericExpandableView(msgLabel, detailsPane, new Color(0xFF3F0A)), BorderLayout.CENTER);
      ComponentStyle.addClass(contentPane, "exception");
      ComponentStyle.addClass(details, "exception-detail");
      ComponentStyle.addClass(msgLabel, "exception-summary");

      pack();
   }
   
   class CustomTextArea extends JTextArea
   {
      CustomTextArea(String value)
      { 
         super(value);
         
         setOpaque(false);
         setEditable(false);
         setBorder(BorderFactory.createTitledBorder("Exception Details"));
      }

      private Dimension MAXSIZE = new Dimension(700,450);
   
      public Dimension getPreferredScrollableViewportSize()
      {
         Dimension p = getPreferredSize();
         p.height = Math.min(p.height, MAXSIZE.height);
         p.width = Math.min(p.width, MAXSIZE.width);
         return p;
      }
      public boolean getScrollableTracksViewportHeight()
      {
         if (getParent() instanceof JViewport)
         {
            JViewport viewport = (JViewport) getParent();
            int vpheight = viewport.getHeight();
            return (vpheight > getPreferredSize().height || vpheight == 0);
         }
         return false;
      }
   }

}
