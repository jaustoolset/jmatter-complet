/*
 * Created on Feb 4, 2004
 */
package com.u2d.view.swing.atom;

import com.u2d.ui.IconButton;
import com.u2d.model.AtomicEObject;
import com.u2d.model.AtomicEditor;
import com.u2d.type.atom.DateEO;
import com.u2d.type.atom.TimeInterval;
import com.u2d.view.ActionNotifier;
import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.Date;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import com.holub.ui.*;
import net.miginfocom.swing.MigLayout;

/**
 * Original date picker, using a customized/tweaked implementation of holub's Date_selector_panel.
 * SwingX's JXMonthView now preferred.
 * 
 * @author Eitan Suez
 */
public class DateEditor extends JPanel
                        implements AtomicEditor, ActionNotifier, CompositeEditor
{
   private JTextField _tf;
   private JButton _calendarBtn;

   private Date_selector_dialog _chooser;
   private Date_selector_panel _date_selector_panel;

   private static Icon CAL_ICON, CAL_ROLLOVER_ICON;
   static
   {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      URL calIconURL = loader.getResource("images/calendar.png");
      CAL_ICON = new ImageIcon(calIconURL);
      calIconURL = loader.getResource("images/calendar_rollover.png");
      CAL_ROLLOVER_ICON = new ImageIcon(calIconURL);
   }

   public DateEditor()
   {
      _tf = new JTextField(9);
      _tf.setHorizontalAlignment(JTextField.RIGHT);

      _calendarBtn = new IconButton(CAL_ICON, CAL_ROLLOVER_ICON);
      _calendarBtn.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent evt)
         {
            if (_chooser == null) setupChooser();

            positionChooser();

            if (_tf.getText().trim().length() > 0)
            {
               try
               {
                  DateEO eo = new DateEO();
                  eo.parseValue(_tf.getText());
                  _date_selector_panel.setTime(eo.dateValue());
               }
               catch (java.text.ParseException ex) {}
            }

            Date date = _chooser.select();
            if (date != null)
            {
               DateEO eo = new DateEO();
               eo.setValue(date);
               _tf.setText(_format.format(eo.dateValue()));
            }
         }
      });

      _date_selector_panel = new Date_selector_panel();

      MigLayout layout = new MigLayout("insets 0, alignx right");  // not trailing.
      setLayout(layout);

      KeyListener keyListener  = new KeyAdapter()
      {
         public void keyTyped(KeyEvent e)
         {
            DateEO deo = new DateEO();
            bind(deo);
            if (deo.isEmpty()) return;
            if (e.getKeyChar() == '+' || e.getKeyChar() == '=')
            {
               deo.add(TimeInterval.ONEDAY);
               render(deo);
               e.consume();
            }
            else if (e.getKeyChar() == '-')
            {
               deo.subtract(TimeInterval.ONEDAY);
               render(deo);
               e.consume();
            }
         }
      };
      _tf.addKeyListener(keyListener);

      add(_tf);
      add(_calendarBtn);

   }


   private void setupChooser()
   {
      Container container = getTopLevelAncestor();
      if (container instanceof Frame)
      {
         _chooser = new Date_selector_dialog((Frame) container, 
                     new Navigable_date_selector(_date_selector_panel));
      }
      else if (container instanceof Dialog)
      {
         _chooser = new Date_selector_dialog((Dialog) container, 
                  new Navigable_date_selector(_date_selector_panel));
      }
      else 
      {
         System.err.println("What's DateField's top-level container?:\n\t"+container);
      }
      _chooser.setDragable(false);
   }
   
   private void positionChooser()
   {
      // need btn location on screen, not relative,
      // because dialog positioning is screen-relative..
      Point iconLoc = _calendarBtn.getLocationOnScreen();

      int pad = 5;
      int xright = iconLoc.x + _calendarBtn.getWidth() + pad;
      int xleft = iconLoc.x - pad - _chooser.getWidth();
      int ydown = iconLoc.y;
      int yup = iconLoc.y + _calendarBtn.getHeight() - _chooser.getHeight();

      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      boolean right = _chooser.getWidth() < (screen.width - xright);
      boolean down = _chooser.getHeight() < (screen.height - ydown);

      int x = (right) ? xright : xleft;
      int y = (down) ? ydown : yup;

      _chooser.setLocation(new Point(x, y));
   }


   private SimpleDateFormat _format = null;
   public void render(AtomicEObject value)
   {
      if (value.isEmpty())
      {
         _tf.setText("");
      }
      else
      {
         _tf.setText(value.marshal());
      }
      if (_format == null)
      {
         DateEO eo = (DateEO) value;
         _format = eo.formatter();
         String tooltip = "[" + _format.toPattern() + "]";
         _tf.setToolTipText(tooltip);
      }
   }

   public int bind(AtomicEObject value)
   {
      try
      {
         value.parseValue(_tf.getText());
         return 0;
      }
      catch (java.text.ParseException ex)
      {
         value.fireValidationException(ex.getMessage());
         return 1;
      }
   }

   // ===

   public JComponent getEditorComponent() { return _tf; }

   public void passivate() { }

   
   // added specifically for integration with table cell editing
   public void addActionListener(ActionListener al)
   {
      _tf.addActionListener(al);
   }
   public void removeActionListener(ActionListener al)
   {
      _tf.removeActionListener(al);
   }


}
