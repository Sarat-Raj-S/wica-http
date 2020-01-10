/*- Package Declaration ------------------------------------------------------*/
package ch.psi.wica.controlsystem.epics;

/*- Imported packages --------------------------------------------------------*/

import ch.psi.wica.model.app.ControlSystemName;
import net.jcip.annotations.Immutable;
import org.apache.commons.lang3.Validate;
import org.epics.ca.Channel;
import org.epics.ca.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/*- Interface Declaration ----------------------------------------------------*/
/*- Class Declaration --------------------------------------------------------*/

/**
 * Provides the functionality to monitor the specified EPICS channel and
 * to publish updating channel value information to listening consumers.
 */
@Immutable
@Component
class EpicsChannelConnectionChangeSubscriber
{

/*- Public attributes --------------------------------------------------------*/
/*- Private attributes -------------------------------------------------------*/

   private final Logger logger = LoggerFactory.getLogger( EpicsChannelConnectionChangeSubscriber.class );
   private final EpicsChannelConnectionStateChangeNotifier epicsChannelConnectionStateChangeNotifier;

/*- Main ---------------------------------------------------------------------*/
/*- Constructor --------------------------------------------------------------*/

   public EpicsChannelConnectionChangeSubscriber( @Autowired EpicsChannelConnectionStateChangeNotifier myExecutor )
   {
      this.epicsChannelConnectionStateChangeNotifier = myExecutor;
   }

/*- Class methods ------------------------------------------------------------*/
/*- Public methods -----------------------------------------------------------*/

   /**
    * Registers a handler which will publish connection changed events.
    *
    * This method operates synchronously but does NOT involve a network
    * round trip.
    *
    * The supplied will be called back from MULTIPLE threads derived
    * from an internal thread pool.
    *
    * Precondition: the supplied channel should never previously have
    *     been connected.
    * Postcondition: the state of the channel will remain unaffected.
    *
    * @param channel the EPICS channel.
    * @param connectionChangeHandler the event consumer.
    *
    * @throws NullPointerException if the channel argument was null.
    * @throws NullPointerException if the connectionChangeHandler argument was null.
    * @throws IllegalStateException if the channel was in an unexpected state on
    *    entry or exit.
    */
   void subscribe( Channel<Object> channel, Consumer<Boolean> connectionChangeHandler )
   {
      // Validate preconditions
      Validate.notNull( channel );
      Validate.notNull( connectionChangeHandler );
      Validate.isTrue( channel.getConnectionState() == ConnectionState.NEVER_CONNECTED, "Programming Error: The channel was not in the expected state (NEVER_CONNECTED)" );

      // Obtain the control system name for logging purposes.
      final ControlSystemName controlSystemName = ControlSystemName.of(channel.getName());

      logger.info("'{}' - adding connection change handler...", controlSystemName );
      channel.addConnectionListener(( chan, isConnected ) -> {

         // Note the current (1.2.2) implementation of the PSI CA library calls back the
         // connection state changed listener only on a SINGLE thread. Therefore, delays in the
         // handler processing will be serialized and would potentially result in a performance
         // bottleneck unless steps are taken to mitigate this. The implementation below
         // makes use of Spring Boot's Async processing facility to ensure that the execution
         // is delegated to execute using a predefined thread pool.

         logger.info("'{}' - scheduling publication of new connection state: '{}'", controlSystemName, isConnected );

         // Delegate the notification to be performed asynchronously using the task executor
         // assciated with the change notifier.
         epicsChannelConnectionStateChangeNotifier.callHandler( controlSystemName, connectionChangeHandler, isConnected );

         logger.trace("'{}' - publication of new connection state has been scheduled.", controlSystemName );

      });

      logger.info("'{}' - connection change handler added.", controlSystemName );

      // Validate postconditions
      Validate.isTrue( channel.getConnectionState() == ConnectionState.NEVER_CONNECTED, "Programming Error: The channel was not in the expected state (NEVER_CONNECTED)" );
   }

/*- Private methods ----------------------------------------------------------*/
/*- Nested Classes -----------------------------------------------------------*/

   @Component
   static class EpicsChannelConnectionStateChangeNotifier
   {
      private final Logger logger = LoggerFactory.getLogger( EpicsChannelConnectionChangeSubscriber.EpicsChannelConnectionStateChangeNotifier.class );

      @Async( "EpicsChannelConnectionChangeSubscriberTaskExecutor" )
      public void callHandler( ControlSystemName controlSystemName, Consumer<Boolean> connectionChangeHandler, boolean isConnected )
      {
         logger.info("'{}' - publishing new connection state: '{}'", controlSystemName, isConnected );
         connectionChangeHandler.accept( isConnected );
         logger.trace("'{}' - published new connection state ok.", controlSystemName );
      }
   }

   @Configuration
   @EnableAsync
   static class TaskExecutorConfigurer
   {
      private final Logger logger = LoggerFactory.getLogger(EpicsChannelConnectionChangeSubscriber.TaskExecutorConfigurer.class);

      /*- Main ---------------------------------------------------------------------*/
      /*- Constructor --------------------------------------------------------------*/
      /*- Class methods ------------------------------------------------------------*/
      /*- Public methods -----------------------------------------------------------*/

      @Bean( name = "EpicsChannelConnectionChangeSubscriberTaskExecutor" )
      public Executor threadPoolTaskExecutor()
      {
         logger.info("Configuring Async Support for EpicsChannelMonitoringService...");
         final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
         executor.setThreadNamePrefix("epics-channel-connection-change-notifier");
         executor.setCorePoolSize(100);
         executor.setMaxPoolSize(100);
         executor.initialize();
         logger.info("Async Support for EpicsChannelMonitoringService configuration completed.");
         return executor;
      }
   }
}