<?xml version="1.0" encoding="utf-8"?>

<jnlp spec="1.0+"
      codebase="http://$ip_addr$:$port$/Sympster"
      href="project.jnlp">

 <information>
   <title>Project App</title> <!-- TODO: revise title appropriately -->
   <vendor>DS Allergy, Inc.</vendor> <!-- TODO: plug in vendor name here -->
   <homepage href="index.html" />
   <description kind="one-line">Sympster App</description>
   <description kind="short">Sympster App</description>
   <description kind="tooltip">Sympster App</description>
   <icon href="leaf.jpg" />
   <icon kind="splash" href="leaf.jpg" /> <!-- TODO: plug in splash image here -->
 </information>

 <security>
   <all-permissions />
 </security>

 <resources>
   <j2se version="1.5+" />
   <jar href="Sympster.jar" main="true" />
   $jars:{jar|
   <jar href="$jar$" />
   }$
 </resources>

 <!-- TODO: plug in main class name here -->
 <application-desc main-class="com.u2d.view.swing.EntryPoint">
   <argument>-Xdock:name=Sympster</argument>
   <argument>-Xdock:icon=icons.icns</argument>
 </application-desc>

</jnlp>
