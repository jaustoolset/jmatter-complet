package com.u2d.wizard.details;

import javax.swing.*;
import java.awt.Dimension;

/**
 * Created by IntelliJ IDEA.
 * User: eitan
 * Date: Aug 26, 2005
 * Time: 11:56:32 AM
 *
 * Obviously needs refinement..
 */
public class Wizard extends CompositeStep
{
   protected CompositeStep _innerStep;

   public static final Dimension PREFERRED_SIZE = new Dimension(350,400);
   private Dimension _contentPaneSize = PREFERRED_SIZE;

   public Wizard(CompositeStep step)
   {
      _innerStep = step;

      addStep(new StartStep());
      addStep(step);
      ready();
   }

   public Wizard(CompositeStep step, Dimension contentPaneSize)
   {
      this(step);
      _contentPaneSize = contentPaneSize;
   }

   public Dimension getContentPaneSize() { return _contentPaneSize; }

   public String compositeTitle()
   {
      if (_innerStep instanceof CompositeStep)
      {
         return ((CompositeStep) _innerStep).compositeTitle();
      }
      return _innerStep.title();
   }

   public int stepNumber()
   {
      if (!hasNextStep()) // we're done
         return 0;
      
      return super.stepNumber() - 1;  // for wizards don't
       // count first step, since it's an introductory step
       // technically you haven't started yet
   }

   public CompositeStep innerStep() { return _innerStep; }

   class StartStep
         extends BasicStep
   {
      public JComponent getView()
      {
         return new SimpleStepPanel("About to begin " + title());
      }

      public String title()
      {
         if (_innerStep instanceof CompositeStep)
            return ((CompositeStep) _innerStep).compositeTitle();
         return _innerStep.title();
      }

      public String description()
      {
         return "Click on the 'Next' button below to begin wizard";
      }
   }

}
