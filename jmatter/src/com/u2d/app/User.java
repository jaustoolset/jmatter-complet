/*
 * Created on May 5, 2004
 */
package com.u2d.app;

import com.u2d.element.CommandInfo;
import com.u2d.model.AbstractComplexEObject;
import com.u2d.model.ComplexEObject;
import com.u2d.model.Title;
import com.u2d.pattern.State;
import com.u2d.reflection.Arg;
import com.u2d.reflection.Cmd;
import com.u2d.reflection.Fld;
import com.u2d.type.atom.*;
import com.u2d.type.composite.Folder;
import com.u2d.type.composite.LoggedEvent;
import com.u2d.type.composite.Name;
import org.hibernate.Session;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Eitan Suez
 */
public class User extends AbstractComplexEObject implements Authorizer
{
   protected final StringEO _username = new StringEO();
   protected final Password _password = new Password();
   protected final BooleanEO _locked = new BooleanEO(false);
   protected final Name _name = new Name();
   protected final TextEO _desktop = new TextEO();
   protected final Photo _photo = new Photo();
   protected final UserPreferences _preferences = new UserPreferences();
   protected final Folder _classBar = new Folder();

   private Role _role;
   public static String roleInverseFieldName = "users";

   public static String[] fieldOrder = {"username", "password", "locked", "name",
         "photo", "role", "preferences", "desktop"};
   public static String[] identities = {"username"};
     // design decision to not make password field editable after first creation.
     // must go through changepwd cmd 
   public static String[] readOnly = {"locked", "desktop"};

   public User() {}

   public User(String username)
   {
      this();
      setTransientState();
      _username.setValue(username);
   }

   public User(String username, String password)
   {
      this(username);
      _password.setValue(password);
   }

   public User(String username, String password, Role role)
   {
      this(username, password);
      _role = role;
      _role.getUsers().add(this);
   }

   public void onBeforeCreate()
   {
      copyClassBarFromAppTemplate();
   }

   protected void copyClassBarFromAppTemplate()
   {
      Folder templateClassBar = app().getClassBar();
      Session session = hbmPersistor().getSession();
      session.update(templateClassBar);  // a precaution in case a new session was obtained
        // since templateClassBar was loaded
      _classBar.getName().setValue("Class Bar");
      for (Iterator itr = templateClassBar.getItems().iterator(); itr.hasNext(); )
      {
         Folder subFolder = (Folder) itr.next();
         final Folder mySubfolder = new Folder();
         mySubfolder.getName().setValue(subFolder.getName());
         session.saveOrUpdate(mySubfolder);
         _classBar.addItem(mySubfolder);
         for (Iterator subItr = subFolder.getItems().iterator(); subItr.hasNext(); )
         {
            ComplexEObject ceo = (ComplexEObject) subItr.next();
            mySubfolder.addItem(ceo);
         }
      }
   }
   private void deleteClassBarFolders()
   {
      final Set itemsToDelete = new HashSet();
      for (Iterator itr = _classBar.getItems().iterator(); itr.hasNext(); )
      {
         Folder subFolder = (Folder) itr.next();
         subFolder.clearItems();
         itemsToDelete.add(subFolder);
      }
      hbmPersistor().deleteMany(itemsToDelete);
   }

   @Fld(mnemonic='u')
   public StringEO getUsername() { return _username; }
   @Fld(description = "Password.  Minimum 5 characters.")
   public Password getPassword() { return _password; }
   public BooleanEO getLocked() { return _locked; }
   public Name getName() { return _name; }

   public Photo getPhoto() { return _photo; }
   
   public UserPreferences getPreferences() { return _preferences; }
   public Folder getClassBar() { return _classBar; }

   @Fld(hidden=true)
   public TextEO getDesktop() { return _desktop; }

   public Role getRole() { return _role; }
   public void setRole(Role role)
   {
      Role oldRole = _role;
      _role = role;
      firePropertyChange("role", oldRole, _role);
   }

   public Title title()
   {
      if (_name.isEmpty())
         return _username.title();
      return _name.title().appendParens(_username);
   }

   @Cmd(mnemonic='p')
   public String ChangePassword(CommandInfo cmdInfo,
                                @Arg("New Password") Password password)
   {
      _password.setValue(password);
      save();
      log(LoggedEvent.INFO, cmdInfo.getCommand(), "User changed password");
      return "Password has been changed";
   }
   
   @Cmd
   public void EditPreferences(CommandInfo info)
   {
      try
      {
         _preferences.command("Edit").execute(_preferences, info.getSource());
      }
      catch (InvocationTargetException e)
      {
         e.printStackTrace();
      }
   }
   
//   @Cmd(sensitive = true)
//   public void ResetClassBar(CommandInfo info)
//   {
//      deleteClassBarFolders();
//      copyClassBarFromAppTemplate();
//      save();
//   }
   
   @Cmd(mnemonic='g',iconref="logout")
   public void LogOut(CommandInfo cmdInfo)
   {
      appSession().onLogout();
   }

   // ===

   public class LockedState extends ReadState
   {
      public String getName() { return LOCKED; }
      @Cmd
      public void Unlock(CommandInfo cmdInfo)
      {
         switchState(_unlockedState);
      }
   }

   public class UnlockedState extends ReadState
   {
      public String getName() { return UNLOCKED; }
      @Cmd
      public void Lock(CommandInfo cmdInfo)
      {
         switchState(_lockedState);
      }
   }

   private transient final State _lockedState, _unlockedState;
   {
      _lockedState = new LockedState();
      _unlockedState = new UnlockedState();
      _stateMap.put(_lockedState.getName(), _lockedState);
      _stateMap.put(_unlockedState.getName(), _unlockedState);
   }
   private static final String LOCKED = "Locked";
   private static final String UNLOCKED = "Unlocked";

   public State startState() { return _unlockedState; }

   public State restoredState()
   {
      String statename = _locked.booleanValue() ? LOCKED : UNLOCKED;
      return (State) _stateMap.get(statename);
   }

   private void switchState(State state)
   {
      if (_currentState.equals(state))
         return;  // no need to switch.

      setState(state, true /* shallow */);
      _locked.setValue(LOCKED.equals(state.getName()));
      firePropertyChange("icon", null, null);

      save();
   }

   /* *** custom icon code:  use photo as icon if possible  *** */
   private transient PhotoIconAssistant _assistant =
         new PhotoIconAssistant(this, _photo);
   public Icon iconLg() { return _assistant.iconLg(); }
   public Icon iconSm() { return _assistant.iconSm(); }

   public boolean equals(Object obj)
   {
      if (obj == null) return false;
      if (!(obj instanceof User)) return false;
      User user = (User) obj;
      return _username.equals(user.getUsername());
   }

   public int hashCode()
   {
      return _username.hashCode();
   }
   
   public void applyRestrictions() { getRole().applyRestrictions(); }
   public void liftRestrictions() { getRole().liftRestrictions(); }

   public boolean authorizes(User user) { return this.equals(user); }
}
