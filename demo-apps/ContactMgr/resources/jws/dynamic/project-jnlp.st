<?xml version="1.0" encoding="utf-8"?>

<jnlp spec="1.0+"
      codebase="http://$ip_addr$:$port$/ContactMgr"
      href="project.jnlp">

 <information>
   <title>Contact Mgr</title> <!-- TODO: revise title appropriately -->
   <vendor>UptoData, Inc.</vendor> <!-- TODO: plug in vendor name here -->
   <homepage href="index.html" />
   <description kind="one-line">ContactMgr App</description>
   <description kind="short">ContactMgr App</description>
   <description kind="tooltip">ContactMgr App</description>
   <icon href="leaf.jpg" />
   <icon kind="splash" href="leaf.jpg" /> <!-- TODO: plug in splash image here -->
 </information>

 <security>
   <all-permissions />
 </security>

 <resources>
   <j2se version="1.5+" />
   <jar href="ContactMgr.jar" main="true" />
   $jars:{jar|
   <jar href="$jar$" />
   }$
 </resources>

 <application-desc main-class="com.u2d.view.swing.EntryPoint">
   <argument>-Xdock:name=ContactMgr</argument>
   <argument>-Xdock:icon=icons.icns</argument>
 </application-desc>

</jnlp>
