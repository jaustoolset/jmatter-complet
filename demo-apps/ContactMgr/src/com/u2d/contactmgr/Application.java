package com.u2d.contactmgr;

/**
 * Created by IntelliJ IDEA.
 * User: eitan
 * Date: Feb 14, 2008
 * Time: 9:49:45 AM
 */
public class Application extends com.u2d.app.Application
{
   public void postInitialize()
   {
      super.postInitialize();
      contributeToIndex(PersonContact.class);
   }
}
