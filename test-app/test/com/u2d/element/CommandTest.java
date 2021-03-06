package com.u2d.element;
/**
 * Created by IntelliJ IDEA.
 * User: eitan
 * Date: Nov 8, 2006
 * Time: 4:27:52 PM
 */

import junit.framework.TestCase;
import com.u2d.model.ComplexType;
import com.u2d.model.AbstractComplexEObject;
import com.u2d.type.composite.Folder;
import com.u2d.type.composite.Person;
import com.u2d.pattern.Onion;
import com.u2d.list.RelationalList;
import com.u2d.app.User;
import java.util.Iterator;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CommandTest extends TestCase
{
   ComplexType folderType;
   EOCommand editCmd;

   protected void setUp()
         throws Exception
   {
      new ClassPathXmlApplicationContext("applicationContext.xml");

      folderType = ComplexType.forClass(Folder.class);
      editCmd = (EOCommand) folderType.instanceCommand("Edit");
   }
   
   public void testQualifiedName()
   {
      assertNotNull(editCmd);
      assertEquals("Edit", editCmd.name());
      assertEquals("Folder.Edit", editCmd.qualifiedName());
      String fullPath = "com.u2d.type.composite.Folder#Edit";
      assertEquals(fullPath, editCmd.fullPath());
      
      Command cmd = Command.forPath(fullPath);
      assertEquals(cmd, editCmd);
   }
   
   public void testCopy()
   {
      Command copy = (Command) editCmd.makeCopy();
      assertTrue(copy instanceof EOCommand);
      assertEquals(editCmd.name(), copy.name());
   }
   
   public void testCommands()
   {
      System.out.println("type commands:");
      Onion commands = folderType.commands();
      print(commands);
      
      Folder instance = (Folder) folderType.instance();
      Onion instanceCommandsInTransientState = instance.commands();
      System.out.println("instance commands in current (transient) state:");
      print(instanceCommandsInTransientState);
      
      Onion instanceCommandsInReadState = folderType.commands(AbstractComplexEObject.ReadState.class);
      System.out.println("instance commands in read state:");
      print(instanceCommandsInReadState);

      Onion instanceCommandsInEditState = folderType.commands(AbstractComplexEObject.EditState.class);
      System.out.println("instance commands in edit state:");
      print(instanceCommandsInEditState);

      System.out.println("all commands: ");
      RelationalList commandsList = folderType.getCommandsList();
      for (Iterator itr = commandsList.iterator(); itr.hasNext(); )
      {
         Command cmd = (Command) itr.next();
         System.out.println("cmd: "+cmd.getFullPath());
      }
      System.out.println("-----");
      
      Command editCmd2 = instance.command("Edit");
      assertSame(editCmd, editCmd2);
      ComplexType folderType2 = ComplexType.forClass(Folder.class);
      assertSame(folderType, folderType2);
      
   }
   
   private void print(Onion commands)
   {
      for (Iterator itr = commands.deepIterator(); itr.hasNext(); )
      {
         Command cmd = (Command) itr.next();
         System.out.println("cmd: "+cmd.getFullPath());
      }
      System.out.println("-----");
   }
   
   public void testUserEditCmd()
   {
      ComplexType userType = ComplexType.forClass(User.class);
      Command editCmd1 = userType.command("Edit", User.LockedState.class);
      Command editCmd2 = userType.command("Edit", User.UnlockedState.class);
      Command editCmd3 = userType.command("Edit", User.ReadState.class);
      
      assertEquals(editCmd1, editCmd2);
      assertEquals(editCmd1, editCmd3);
      assertEquals(editCmd2, editCmd3);
      
      assertSame(editCmd1, editCmd2);
      assertSame(editCmd2, editCmd3);
      assertSame(editCmd3, editCmd1);
   }
   
   public void testOpenCmdExists()
   {
      ComplexType userType = ComplexType.forClass(User.class);
      Command openCmd = userType.defaultCommand();
      assertNotNull(openCmd);
   }
   
   public void testHasDefaultCmd()
   {
      ComplexType userType = ComplexType.forClass(User.class);
      User user = (User) userType.instance();
      Command defaultCmd = user.defaultCommand();
      assertNotNull(defaultCmd);
   }
   
   public void testOverloadedCmd()
   {
      Command newCmd = folderType.command("New");
      assertNotNull(newCmd);
      assertTrue(newCmd instanceof OverloadedEOCmd);
   }
   
   public void testCommandUniqueness()
   {
      ComplexType personType = ComplexType.forClass(Person.class);
      ComplexType userType = ComplexType.forClass(User.class);
      Command personNew = personType.command("New");
      Command userNew = userType.command("New");
      assertNotSame(personNew, userNew);
      
      Person p1 = (Person) personType.instance();
      Person p2 = (Person) personType.instance();
      assertSame(p1.command("New"), p2.command("New"));
   }


}