import com.google.inject.AbstractModule
import com.google.inject.name.Names

import play.api.{Configuration, Environment}

import services.{ClevercloudApi, AkkaCluster}

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.
 *
 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  def configure() = {
    println(s">>>> configure module ")

    // Use the system clock as the default implementation of Clock
    //bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    // Ask Guice to create an instance of ApplicationTimer when the
    // application starts.
    //bind(classOf[ApplicationTimer]).asEagerSingleton()
    bind(classOf[ClevercloudApi]).asEagerSingleton()
    //bind(classOf[AkkaCluster]).asEagerSingleton()
  }

}
