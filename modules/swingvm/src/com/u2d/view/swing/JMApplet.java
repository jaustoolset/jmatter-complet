package com.u2d.view.swing;

import com.u2d.app.*;
import com.u2d.interaction.Instruction;
import com.u2d.view.swing.list.CommandsMenuView;
import com.u2d.view.swing.list.CommandsIconButtonView;
import com.u2d.view.swing.dnd.EODesktopPane;
import com.u2d.view.swing.atom.URIRenderer;
import com.u2d.pattern.Filter;
import com.u2d.pattern.Onion;
import com.u2d.element.Command;
import com.u2d.ui.LockablePanel;
import com.u2d.ui.CardPanel;
import com.u2d.ui.UIUtils;
import com.u2d.ui.Platform;
import com.u2d.ui.desktop.CloseableJInternalFrame;
import com.u2d.ui.desktop.Positioning;
import static com.u2d.pubsub.AppEventType.LOGIN;
import static com.u2d.pubsub.AppEventType.LOGOUT;
import com.u2d.pubsub.AppEventListener;
import com.u2d.css4swing.style.ComponentStyle;
import com.u2d.type.composite.Folder;
import com.u2d.type.atom.URI;
import com.u2d.model.ComplexType;
import com.u2d.model.EObject;
import com.u2d.persist.HBMSingleSession;
import com.u2d.utils.Launcher;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.beans.XMLEncoder;
import java.beans.XMLDecoder;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.swingx.JXPanel;

/**
 * Created by IntelliJ IDEA.
 * User: Eitan Suez
 * Date: Oct 28, 2008
 * Time: 4:36:32 PM
 */
public class JMApplet extends JApplet implements AppContainer
{
   public void init()
   {
   }

   public void start()
   {
      SwingViewMechanism.getInstance().launch(this);
   }

   public void destroy()
   {
      _app.onShutdown();
   }



   private AppSession _appSession;
   private Application _app;
   private JMenuBar _menuBar;
   private ImageIcon _appIcon;
   private Instruction instruction;
   private InstructionView instructionView;
   private AboutDlg aboutDlg;

   private CommandsMenuView _userMenu = new CommandsMenuView(new Filter()
   {
      String[] excludeCmds = {"Delete", "Copy", "Refresh", "Lock"};
      public boolean exclude(Object item)
      {
         Command cmd = (Command) item;
         for (String excludeCmd : excludeCmds)
         {
            if (excludeCmd.equals(cmd.name()))
               return true;
         }
         return false;
      }
   });

   private JPanel _centerPane;
   private OutlookFolderView _classBar = new OutlookFolderView();
   private JPanel _classBarPanel = new LockablePanel(_classBar);
   private ClassMenu _classMenu = new ClassMenu();
   private EODesktopPane _desktopPane, _loggedOutDesktopPane;
   private WindowButtonBar _windowButtonBar;
   private CardPanel _cardPanel, _sessionCardPanel;

   public JMApplet()
   {
      setupUI();
      setupLoginLogoutListeners();
   }
   public JMApplet(AppSession appSession)
   {
      this();
      setupApp(appSession);
   }

   public void appUnloaded()
   {
      _appSession.removeAppEventListener(LOGIN, _loginListener);
      _appSession.removeAppEventListener(LOGOUT, _logoutListener);
      _appSession = null;
      _cardPanel.show("app-off");
   }
   public void appLoaded(AppSession appSession)
   {
      setupApp(appSession);
   }

   private void setupUI()
   {
      setupAppIcon();

      JPanel contentPane = (JPanel) getContentPane();

      _desktopPane = new EODesktopPane();
      _desktopPane.getContextMenu().addSeparator();
      setupInstructionView();

      _windowButtonBar = new WindowButtonBar();

      _centerPane = new JPanel(new BorderLayout());
      _centerPane.add(_classBarPanel, BorderLayout.LINE_START);
      JPanel innerPane = new JPanel(new BorderLayout());
      innerPane.add(_desktopPane, BorderLayout.CENTER);
      innerPane.add(new HorizScrollpane(_windowButtonBar), BorderLayout.PAGE_END);
      _centerPane.add(innerPane, BorderLayout.CENTER); // this is so that classbar drops all the way
        // to the bottom and status pane stops at right edge of classbar

      _loggedOutDesktopPane = new EODesktopPane();
      _loggedOutDesktopPane.setEnabled(false);

      _sessionCardPanel = new CardPanel();
      _sessionCardPanel.add(_loggedOutDesktopPane, "loggedout");
      _sessionCardPanel.add(_centerPane, "loggedin");

      _cardPanel = new CardPanel();
      _cardPanel.add(new AppLoaderPanel(), "app-off");
      _cardPanel.add(_sessionCardPanel, "app-on");
      _cardPanel.show("app-off");

      contentPane.add(_cardPanel, BorderLayout.CENTER);

      UIUtils.centerOnScreen(this);
   }

   private void setupApp(AppSession appSession)
   {
      _appSession = appSession;
      _app = _appSession.getApp();

      _appSession.addAppEventListener(LOGIN, _loginListener);
      _appSession.addAppEventListener(LOGOUT, _logoutListener);

      setupAppIcon();

      if (aboutDlg != null) aboutDlg.rebind();
      setupCommandBar();
      setupMenu();

      instruction = new Instruction();
      instructionView.bind(instruction);

      _cardPanel.show("app-on");
      applyComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));
   }

   private void setupAppIcon()
   {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      java.net.URL imgURL = loader.getResource("images/App32.png");
      if (imgURL != null)
      {
         _appIcon = new ImageIcon(imgURL);
      }
   }

   private void setupInstructionView()
   {
      instructionView = new InstructionView();
      ComponentStyle.setIdent(instructionView, "command-panel");
      _desktopPane.add(instructionView, JLayeredPane.POPUP_LAYER);
      UIUtils.center(_desktopPane, instructionView);
   }

   private void setupKeyboardShorcuts()
   {
      bindKeyStroke("alt SLASH", "focus-classbar", new AbstractAction()
         {
            public void actionPerformed(ActionEvent e)
            {
               _classBar.focusItem();
            }
         });
      bindKeyStroke(KeyStroke.getKeyStroke('W', Platform.mask()), "close-window", new AbstractAction() {
         public void actionPerformed(ActionEvent e)
         {
            JInternalFrame jif = _desktopPane.getSelectedFrame();
            if (jif != null && jif instanceof CloseableJInternalFrame)
            {
               ((CloseableJInternalFrame) jif).close();
            }
         }
      });
      bindKeyStroke(KeyStroke.getKeyStroke('I', Platform.mask()), "invoke-instruction", new AbstractAction() {
         public void actionPerformed(ActionEvent e)
         {
            instruction.activate();
            instruction.getActive().fireStateChanged();
         }
      });
   }

   private void bindKeyStroke(String shortcut, String key, Action action)
   {
      bindKeyStroke(KeyStroke.getKeyStroke(shortcut), key, action);
   }
   private void bindKeyStroke(KeyStroke kstroke, String key, Action action)
   {
      JPanel contentPane = (JPanel) getContentPane();
      contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(kstroke, key);
      contentPane.getActionMap().put(key, action);
   }
   private void detachKeyStroke(String key)
   {
      JPanel contentPane = (JPanel) getContentPane();
      contentPane.getActionMap().remove(key);
   }
   private void detachKeystrokes()
   {
      detachKeyStroke("focus-classbar");
      detachKeyStroke("close-window");
      detachKeyStroke("invoke-instruction");
   }

   private Set<String> keybindings = new HashSet<String>();

   private void bindTypeKeyboardShortcuts(Folder userClassBar)
   {
      keybindings = new HashSet<String>();
      for (int i=0; i<userClassBar.size(); i++)
      {
         Object item = userClassBar.get(i);
         if (item instanceof Folder)
         {
            Folder folder = (Folder) item;
            for (int j=0; j<folder.size(); j++)
            {
               Object subItem = folder.get(j);
               if (subItem instanceof ComplexType)
               {
                  ComplexType type = (ComplexType) subItem;
                  bindTypeCommands(type);
               }
            }
         }
      }
   }

   private void bindTypeCommands(ComplexType type)
   {
      Onion typeCommands = type.commands();
      for (Iterator itr = typeCommands.deepIterator(); itr.hasNext(); )
      {
         Command cmd = (Command) itr.next();
         if (cmd.hasShortcut())
         {
            String key = keyFor(type, cmd);
            bindKeyStroke(cmd.shortcut(), key, new CommandAdapter(cmd, null));
            keybindings.add(key);
         }
      }
   }

   private void detachTypeKeyboardShortcuts()
   {
      for (String key : keybindings)
      {
         detachKeyStroke(key);
      }
   }

   private String keyFor(ComplexType type, Command cmd)
   {
      return String.format("%s-%s", type.name(), cmd.name());
   }

   AppEventListener _loginListener, _logoutListener;

   private void setupLoginLogoutListeners()
   {
      _loginListener = new AppEventListener()
      {
         public void onEvent(com.u2d.pubsub.AppEvent evt)
         {
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  _sessionCardPanel.show("loggedin");
                  showUserMenu();
                  showCommandBar();
                  showClassBar();
                  _menuBar.revalidate(); _menuBar.repaint();
                  setupKeyboardShorcuts();
                  restoreUserDesktop();
               }
            });
         }
      };
      _logoutListener = new AppEventListener()
      {
         public void onEvent(com.u2d.pubsub.AppEvent evt)
         {
            Thread thread = AppLoader.getInstance().newThread(new Runnable() { public void run()
               {
                  saveUserDesktop();

                  SwingUtilities.invokeLater(new Runnable()
                  {
                     public void run()
                     {
                        _desktopPane.closeAllChildren();
                        _windowButtonBar.cleanup();
                        hideClassBar();
                        hideUserMenu();
                        hideCommandBar();
                        _menuBar.revalidate(); _menuBar.repaint();
                        detachKeystrokes();
                        _sessionCardPanel.show("loggedout");
                     }
                  });

                  PersistenceMechanism pmech = _app.getPersistenceMechanism();
                  if (pmech instanceof HBMSingleSession)
                  {
                     ((HBMSingleSession) pmech).newSession();
                  }
               }
            });
            thread.start();

            // must wait for thread to finish its job.  otherwise appSession.setUser(null)
            // may occur prematurely
            try
            {
               thread.join();
            }
            catch (InterruptedException e)
            {
               e.printStackTrace();
            }
         }
      };
   }

   EObject _serviceObject = null;
   CommandsIconButtonView _commandsPnl = null;
   JPanel northPanel = null;
   CommandsMenuView _commandsMenu = null;
   private void setupCommandBar()
   {
      _serviceObject = _app.serviceObject();
      if (_serviceObject != null)
      {
         _commandsMenu = new CommandsMenuView();

         MigLayout layout = new MigLayout("alignx leading, insets 0", "", "[fill, align center]");
         northPanel = new JPanel(layout);

         _commandsPnl = new CommandsIconButtonView();
         northPanel.add(_commandsPnl);

         ComponentStyle.addClass(northPanel, "command-bar");
         _centerPane.add(northPanel, BorderLayout.PAGE_START);
        for (Runnable r : deferredActions)
        {
           r.run();
        }
      }
   }
   private java.util.List<Runnable> deferredActions = new ArrayList<Runnable>();
   public void contributeToHeader(final JComponent component)
   {
      deferredActions.add(new Runnable()
      {
         public void run()
         {
            northPanel.add(component);
         }
      });
   }

   private void setupMenu()
   {
      _menuBar = new JMenuBar();
      _menuBar.add(helpMenu());
      setJMenuBar(_menuBar);
   }
   private JMenu helpMenu()
   {
      JMenu helpMenu = configMenu("menubar.help");
      helpMenu.add(new AboutAction());
      helpMenu.add(new HelpContentsAction());
      return helpMenu;
   }
   private JMenu configMenu(String key)
   {
      TextWithMnemonic twm = TextWithMnemonic.lookup(key);
      JMenu menu = new JMenu(twm.text());
      if (twm.hasMnemonic())
      {
         menu.setMnemonic(twm.mnemonic());
      }
      return menu;
   }

   public void addLoginDialog(LoginDialog loginDialog)
   {
      _loggedOutDesktopPane.add(loginDialog, JLayeredPane.MODAL_LAYER);
   }

   public JInternalFrame addFrame(JInternalFrame frame)
   {
      _windowButtonBar.frameAdded(frame);
      _desktopPane.addFrame(frame);
      return frame;
   }
   public JInternalFrame addFrame(JInternalFrame frame, Positioning positioning)
   {
      _desktopPane.addFrame(frame, positioning);
      return frame;
   }
   public void centerFrame(JInternalFrame frame) { _desktopPane.positionFrame(frame, Positioning.CENTERED); }
   public JInternalFrame getSelectedFrame() { return _desktopPane.getSelectedFrame(); }


   public void popup(JPopupMenu menu)
   {
      _desktopPane.popup(menu);
   }

   //===
   private void showCommandBar()
   {
      if (_commandsPnl != null)
      {
         _commandsPnl.bind(_serviceObject, null);
         _commandsMenu.bind(_serviceObject, _menuBar, null);
         _menuBar.add(_commandsMenu, 1);
      }
   }
   private void hideCommandBar()
   {
      if (_commandsPnl != null)
      {
         _commandsPnl.detach();
         _commandsMenu.detach();
      }
   }
   private void showClassBar()
   {
      User currentUser = _appSession.getUser();
      Folder userClassBar = currentUser.getClassBar();
      _classBar.bind(userClassBar);
      _classMenu.bind(userClassBar, _menuBar, 1);

      bindTypeKeyboardShortcuts(userClassBar);

      _classBar.focusItem();
      SwingUtilities.invokeLater(new Runnable() {
         public void run()
         {
            _centerPane.revalidate(); _centerPane.repaint();
         }
      });
   }
   private void hideClassBar()
   {
      _classBar.detach();
      _classMenu.detach();
      detachTypeKeyboardShortcuts();
   }
   //===

      private void showUserMenu()
      {
         User currentUser = _appSession.getUser();
         _userMenu.bind(currentUser, _menuBar, null);
         _menuBar.add(_userMenu, 1);
      }

      private void hideUserMenu()
      {
         _userMenu.detach();
      }

   //===

   private void configAction(Action action, String key)
   {
      TextWithMnemonic twm = TextWithMnemonic.lookup(key);
      action.putValue(javax.swing.Action.NAME, twm.text());
      if (twm.hasMnemonic())
      {
         action.putValue(Action.MNEMONIC_KEY, (int) twm.mnemonic());
      }
   }


   class AboutAction extends AbstractAction
   {
      public AboutAction()
      {
         configAction(this, "menubar.help.about");
      }
      public void actionPerformed(ActionEvent e)
      {
         if (aboutDlg == null)
         {
            aboutDlg = new AboutDlg();
         }
         aboutDlg.setVisible(true);
      }
   }
   class AboutDlg extends JDialog implements ActionListener
   {
      JButton closeBtn;
      private JLabel titleView;
      private JTextArea descriptionArea;
      private URIRenderer link;

      AboutDlg()
      {
         super((JFrame) null, "About "+_app.getName(), true);
         setResizable(false);

         laymeout();

         pack();
         UIUtils.center(JMApplet.this, AboutDlg.this, true);
         addComponentListener(new ComponentAdapter()
         {
            public void componentShown(ComponentEvent e)
            {
               SwingUtilities.invokeLater(new Runnable()
               {
                  public void run()
                  {
                     closeBtn.requestFocus();
                  }
               });
            }
         });
      }
      private void laymeout()
      {
         JPanel contentPane = (JPanel) getContentPane();
         MigLayout layout = new MigLayout("fill, flowy");
         contentPane.setLayout(layout);

         titleView = new JLabel(_app.title(), _appIcon, JLabel.LEADING);
         ComponentStyle.addClass(titleView, "title");
         add(titleView);

         descriptionArea = new JTextArea(_app.getDescription(), 5, 40);
         descriptionArea.setEditable(false);
         descriptionArea.setOpaque(false);
         add(new JScrollPane(descriptionArea));

         link = new URIRenderer();
         link.render(new URI(_app.getHelpContentsUrl()));
         add(link);

         closeBtn = new JButton("OK");
         closeBtn.addActionListener(AboutDlg.this);
         add(closeBtn, "tag ok");

         ComponentStyle.setIdent(contentPane, "aboutPnl");
      }

      private String title() { return String.format("About %s", _app.getName()); }
      public void rebind()
      {
         setTitle(title());
         titleView.setText(_app.title());
         titleView.setIcon(_appIcon);
         descriptionArea.setText(_app.getDescription());
         link.render(new URI(_app.getHelpContentsUrl()));
      }

      public void actionPerformed(ActionEvent e)
      {
         AboutDlg.this.setVisible(false);
      }
   }

   class HelpContentsAction extends AbstractAction
   {
      public HelpContentsAction()
      {
         configAction(this, "menubar.help.contents");
      }
      public void actionPerformed(ActionEvent e)
      {
         Launcher.openInBrowser(_app.getHelpContentsUrl());
      }
   }

   public void onMessage(String msg)
   {
      _desktopPane.message(msg);
   }


   private static final Cursor WAITCURSOR = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
   private static final Cursor DEFAULTCURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

   public void setWaitCursor()
   {
      getGlassPane().setCursor(WAITCURSOR);
      getGlassPane().setVisible(true);
   }
   public void setDefaultCursor()
   {
      getGlassPane().setCursor(DEFAULTCURSOR);
      getGlassPane().setVisible(false);
   }

   /* ======
    *
    * methods related to saving and restoring the user desktop follow..
    *
    * ------
    */

   private void saveUserDesktop()
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
      XMLEncoder enc = new XMLEncoder(baos);
      serialize(enc);
      enc.close();
      User currentUser = _appSession.getUser();
      currentUser.getDesktop().setValue(baos.toString());

      // currentuser lives for entire session.  if obtain a new session, must attach
      // object to new session;  this is not always the cause but a good precaution.
      // actually avoids an exception in certain circumstances.
      HBMPersistenceMechanism hbm = (HBMPersistenceMechanism) _app.getPersistenceMechanism();
      hbm.getSession().update(currentUser);
      currentUser.save();
   }

   private void restoreUserDesktop()
   {
      String desktop = _appSession.getUser().getDesktop().stringValue();
      if (desktop == null || desktop.length() <= 0) return;  // nothing saved
      XMLDecoder dec = new XMLDecoder(new ByteArrayInputStream(desktop.getBytes()));
      deserialize(dec);
   }

   private void serialize(XMLEncoder enc)
   {
      JInternalFrame[] frames = _desktopPane.getAllFrames();
      java.util.List<CloseableJInternalFrame> framesToSave = new ArrayList<CloseableJInternalFrame>();

      for (JInternalFrame f : frames)
      {
         if (!f.isVisible() || f.isIcon()) continue;
         if (!(f instanceof CloseableJInternalFrame)) continue;
         CloseableJInternalFrame cjif = (CloseableJInternalFrame) f;
         framesToSave.add(cjif);
      }

      enc.writeObject(framesToSave.size());
      for (CloseableJInternalFrame f : framesToSave)
      {
         f.serialize(enc);
      }
   }
   private void deserialize(XMLDecoder dec)
   {
      int numFrames = (Integer) dec.readObject();
      for (int i=0; i<numFrames; i++)
      {
         Class viewType = (Class) dec.readObject();
         try
         {
            CloseableJInternalFrame f = (CloseableJInternalFrame) viewType.newInstance();
            f.deserialize(dec);
            if (!(f instanceof FlexiFrame) || !((FlexiFrame) f).isEmpty())
            {
               addFrame(f, Positioning.NONE);
            }
         }
         catch (InstantiationException ex) { ex.printStackTrace(); }
         catch (IllegalAccessException ex) { ex.printStackTrace(); }
      }
   }


   class AppLoaderPanel extends JXPanel
   {
      public AppLoaderPanel()
      {
         ComponentStyle.setIdent(this, "appload-pnl");
      }
   }

   class WindowButtonBar extends JPanel implements Scrollable
   {
      ButtonGroup buttonGroup = new ButtonGroup();
      private InternalFrameListener iframeListener;
      public static final String FRAMEBUTTON_CLIENTPROPERTYNAME = "sibling-button";

      public WindowButtonBar()
      {
         setLayout(new FlowLayout(FlowLayout.LEADING, 1, 1));
         setBorder(new BevelBorder(BevelBorder.LOWERED));

         iframeListener = new InternalFrameListener()
         {
            public void internalFrameClosing(InternalFrameEvent e)
            {
               JInternalFrame frame = (JInternalFrame) e.getSource();
               cleanup(frame, buttonFor(frame));
               WindowButtonBar.this.revalidate();
               WindowButtonBar.this.repaint();
            }

            public void internalFrameActivated(InternalFrameEvent e)
            {
               JInternalFrame frame = (JInternalFrame) e.getSource();
               buttonFor(frame).setSelected(true);
            }

            public void internalFrameDeactivated(InternalFrameEvent e)
            {
               JInternalFrame frame = (JInternalFrame) e.getSource();
               buttonFor(frame).setSelected(false);
            }

            public void internalFrameOpened(InternalFrameEvent e) { }
            public void internalFrameClosed(InternalFrameEvent e) { }
            public void internalFrameIconified(InternalFrameEvent e) { }
            public void internalFrameDeiconified(InternalFrameEvent e) { }

         };
      }

      private void cleanup(JInternalFrame frame, FrameButton button)
      {
         button.cleanup();
         buttonGroup.remove(button);
         remove(button);

         frame.putClientProperty(FRAMEBUTTON_CLIENTPROPERTYNAME, null); // no remove method.  done by setting to null.
         frame.removeInternalFrameListener(iframeListener);
      }

      public void cleanup()
      {
         Component[] components = getComponents();
         for (Component comp : components)
         {
            FrameButton button = (FrameButton) comp;
            JInternalFrame frame = button.getFrame();
            cleanup(frame, button);
         }
      }

      private FrameButton buttonFor(JInternalFrame frame)
      {
         return (FrameButton) frame.getClientProperty(FRAMEBUTTON_CLIENTPROPERTYNAME);
      }

      void frameAdded(JInternalFrame frame)
      {
         FrameButton button = new FrameButton(frame, _desktopPane);
         buttonGroup.add(button);
         add(button);
         frame.putClientProperty(FRAMEBUTTON_CLIENTPROPERTYNAME, button);
         frame.addInternalFrameListener(iframeListener);
      }

      public Dimension getPreferredSize()
      {
         Dimension pref = super.getPreferredSize();
         pref.height = 28;
         return pref;
      }

      public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
      public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 150; }
      public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 150; }

      public boolean getScrollableTracksViewportWidth()
      {
         if (getParent() instanceof JViewport)
         {
            return (getParent().getWidth() > getPreferredSize().width);
         }
         return false;
      }

      public boolean getScrollableTracksViewportHeight() { return false; }
   }

}