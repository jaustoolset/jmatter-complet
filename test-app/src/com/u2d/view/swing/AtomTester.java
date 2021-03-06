/*
 * Created on Jan 27, 2004
 */
package com.u2d.view.swing;

import com.u2d.domain.*;
import com.u2d.app.*;
import com.u2d.type.atom.*;

/**
 * @author Eitan Suez
 */
public class AtomTester
{
   ViewMechanism _vmech;

   public AtomTester()
   {
      SwingViewMechanism.getInstance().launch();
      _vmech = SwingViewMechanism.getInstance();

      AtomicMedley medley = new AtomicMedley(23, "Joey", new Email("eitan@uptodata.com"), true, 23.3);
      _vmech.displayView(medley.getFormView(), null);
   }

   public static void main(String[] args)
   {
      new AtomTester();
   }

}
