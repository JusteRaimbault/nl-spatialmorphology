/*{
  val pluginVersion = System.getProperty("plugin.version")
  if(pluginVersion == null)
    throw new RuntimeException("""|The system property 'plugin.version' is not defined.
      |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
    else addSbtPlugin("org.nlogo" % "netlogo-extension-plugin" % pluginVersion)
}*/

resolvers += Resolver.url(
  "NetLogo-JVM",
    url("http://dl.bintray.com/content/netlogo/NetLogo-JVM"))(
        Resolver.ivyStylePatterns)

addSbtPlugin("org.nlogo" % "netlogo-extension-plugin" % "3.2")
