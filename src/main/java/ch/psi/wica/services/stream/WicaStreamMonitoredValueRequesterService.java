/*- Package Declaration ------------------------------------------------------*/
package ch.psi.wica.services.stream;

/*- Imported packages --------------------------------------------------------*/

import ch.psi.wica.controlsystem.event.WicaChannelMetadataUpdateEvent;
import ch.psi.wica.controlsystem.event.WicaChannelMonitoredValueUpdateEvent;
import ch.psi.wica.controlsystem.event.WicaChannelStartMonitoringEvent;
import ch.psi.wica.controlsystem.event.WicaChannelStopMonitoringEvent;
import ch.psi.wica.model.app.WicaDataBufferStorageKey;
import ch.psi.wica.model.channel.WicaChannel;
import ch.psi.wica.model.channel.WicaChannelMetadata;
import ch.psi.wica.model.channel.WicaChannelValue;
import ch.psi.wica.model.stream.WicaStream;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/*- Interface Declaration ----------------------------------------------------*/
/*- Class Declaration --------------------------------------------------------*/

/**
 * Provides a service for starting and stopping the control system monitoring
 * of a WicaStream.
 */
@Configuration
@EnableScheduling
@Service
@ThreadSafe
public class WicaStreamMonitoredValueRequesterService
{

/*- Public attributes --------------------------------------------------------*/
/*- Private attributes -------------------------------------------------------*/

   private static final int RESOURCE_RELEASE_SCAN_INTERVAL = 1000;

   private final Logger logger = LoggerFactory.getLogger( WicaStreamMonitoredValueRequesterService.class );

   private final int wicaChannelResourceReleaseIntervalInSecs;
   private final ApplicationEventPublisher applicationEventPublisher;
   private final Map<WicaDataBufferStorageKey,Integer> monitoredChannelInterestMap;
   private final Map<WicaDataBufferStorageKey,LocalDateTime> monitoredChannelEventMap;

/*- Main ---------------------------------------------------------------------*/
/*- Constructor --------------------------------------------------------------*/

   /**
    * Constructs a new instance.
    *
    * @param wicaChannelResourceReleaseIntervalInSecs period after which the
    *    resources associated with a Wica Channel will be released if they are no
    *    longer in use.
    *
    * @param applicationEventPublisher reference to the application publisher
    *    which will be used to publish the channels that are to be monitored
    *    or which are no longer of interest.
    */
   WicaStreamMonitoredValueRequesterService( @Value( "${wica.channel-resource-release-interval-in-secs}" ) int wicaChannelResourceReleaseIntervalInSecs,
                                             @Autowired ApplicationEventPublisher applicationEventPublisher )
   {
      this.wicaChannelResourceReleaseIntervalInSecs = wicaChannelResourceReleaseIntervalInSecs;
      this.applicationEventPublisher = Validate.notNull( applicationEventPublisher );
      this.monitoredChannelInterestMap = Collections.synchronizedMap( new HashMap<>() );
      this.monitoredChannelEventMap = Collections.synchronizedMap( new HashMap<>() );
   }

/*- Class methods ------------------------------------------------------------*/
/*- Public methods -----------------------------------------------------------*/
/*- Package-level methods ----------------------------------------------------*/

   /**
    * Starts monitoring the channels in the specified stream.
    *
    * @param wicaStream the stream to monitor.
    */
   void startMonitoring( WicaStream wicaStream )
   {
      Validate.notNull( wicaStream );
      wicaStream.getWicaChannels()
            .stream()
            .filter( c -> c.getProperties().getDataAcquisitionMode().doesMonitoring() )
            .forEach( this::startMonitoringChannel);
   }

   /**
    * Stops monitoring the channels in the specified stream.
    *
    * @param wicaStream the stream that is no longer of interest.
    */
   void stopMonitoring( WicaStream wicaStream )
   {
      Validate.notNull( wicaStream );
      wicaStream.getWicaChannels()
            .stream()
            .filter( c -> c.getProperties().getDataAcquisitionMode().doesMonitoring() )
            .forEach( this::stopMonitoringChannel);
   }

   /**
    * Returns the level of interest in a WicaChannel.
    *
    * @implNote. this method is provided mainly for test purposes.
    *
    * @param wicaChannel the name of the channel to lookup.
    * @return the current interest count (or zero if the channel was
    *     not recognised).
    */
   int getInterestCountForChannel( WicaChannel wicaChannel )
   {
      final var storageKey = WicaDataBufferStorageKey.getMonitoredValueStorageKey( wicaChannel );
      return monitoredChannelInterestMap.getOrDefault( storageKey, 0 );
   }

   /**
    * Returns the timestamp of the last event
    *
    * @implNote. this method is provided mainly for test purposes.
    *
    * @param wicaChannel the name of the channel to lookup.
    * @return the current interest count (or zero if the channel was
    *     not recognised).
    */
   Optional<LocalDateTime> getLastEventForChannel( WicaChannel wicaChannel )
   {
      final var storageKey = WicaDataBufferStorageKey.getMonitoredValueStorageKey( wicaChannel );
      return Optional.ofNullable( monitoredChannelEventMap.get( storageKey ) );
   }

/*- Private methods ----------------------------------------------------------*/

   /**
    * Starts monitoring the Wica channel with the specified name and/or
    * increments the interest count for this channel.
    *
    * Immediately thereafter the channel's connection state, metadata and
    * value will become observable via the other methods in this class.
    *
    * Until the wica server receives its first value from the channel's
    * underlying data source the metadata will be set to type UNKNOWN and
    * the value set to show that the channel is disconnected.
    *
    * @param wicaChannel the name of the channel to monitor.
    */
   private void startMonitoringChannel( WicaChannel wicaChannel )
   {
      Validate.notNull( wicaChannel );
      logger.info( "Request to start monitoring wica channel: '{}'", wicaChannel);

      final var storageKey = WicaDataBufferStorageKey.getMonitoredValueStorageKey( wicaChannel );
      final var controlSystemName = wicaChannel.getName().getControlSystemName();

      // Update the timestamp of the event that was most recently associated with the storage key.
      monitoredChannelEventMap.put( storageKey, LocalDateTime.now() );

      // If a channel with the same storage key was not previously being monitored
      // then start monitoring it. Otherwise simply increase the interest count.
      if ( monitoredChannelInterestMap.containsKey( storageKey ) )
      {
         final int newInterestCount = monitoredChannelInterestMap.get( storageKey ) + 1;
         logger.info( "Increasing interest level in monitored control system channel: '{}' to {}", controlSystemName, newInterestCount );
         monitoredChannelInterestMap.put( storageKey, newInterestCount );
      }
      else
      {
         logger.info( "Starting monitoring on control system channel: '{}'", controlSystemName );

         // Set the initial state for the value and metadata stashes and publish an event
         // instructing the underlying control system to start monitoring.
         applicationEventPublisher.publishEvent( new WicaChannelMetadataUpdateEvent( wicaChannel, WicaChannelMetadata.createUnknownInstance() ) );
         applicationEventPublisher.publishEvent( new WicaChannelMonitoredValueUpdateEvent(wicaChannel, WicaChannelValue.createChannelValueDisconnected() ) );
         applicationEventPublisher.publishEvent( new WicaChannelStartMonitoringEvent( wicaChannel ) );
         monitoredChannelInterestMap.put( storageKey, 1 );
      }
   }

   /**
    * Stops monitoring the Wica channel with the specified name (which
    * should previously have been monitored) and/or reduces the interest
    * count for this channel.
    *
    * When/if the interest in the channel is reduced to zero then any
    * attempts to subsequently observe it's state will result in an
    * exception.
    *
    * @param wicaChannel the name of the channel which is no longer of interest.
    *
    * @throws IllegalStateException if the channel was never previously monitored.
    */
   private void stopMonitoringChannel( WicaChannel wicaChannel )
   {
      Validate.notNull( wicaChannel );
      logger.info( "Request to stop monitoring wica channel: '{}'", wicaChannel );

      final var storageKey = WicaDataBufferStorageKey.getMonitoredValueStorageKey( wicaChannel );
      final var controlSystemName = wicaChannel.getName().getControlSystemName();
      Validate.validState( monitoredChannelInterestMap.containsKey( storageKey ) );
      Validate.validState(monitoredChannelInterestMap.get( storageKey ) > 0 );

      // Update the timestamp of the event that was most recently associated with the storage key.
      monitoredChannelEventMap.put( storageKey, LocalDateTime.now() );

      // Reduce the level of interest in the channel.
      final int currentInterestCount = monitoredChannelInterestMap.get( storageKey );
      final int newInterestCount = currentInterestCount - 1;
      logger.info( "Reducing interest level in monitored control system channel: '{}' to {}" , controlSystemName.asString(), newInterestCount );
      monitoredChannelInterestMap.put( storageKey, newInterestCount );

      if ( currentInterestCount == 0 )
      {
         logger.info( "No more interest in control system channel: '{}'", controlSystemName.asString() );
         logger.info( "The resources for the channel will be discarded in {} seconds.", wicaChannelResourceReleaseIntervalInSecs );
      }
   }

   /**
    * This method runs periodically to scan the discard list for entries corresponding to
    * monitored channels that are no longer of interest
    */
   @Scheduled( fixedRate=RESOURCE_RELEASE_SCAN_INTERVAL )
   public void discardMonitorsThatHaveReachedEndOfLife()
   {
      final var timeNow = LocalDateTime.now();
      monitoredChannelInterestMap.keySet()
         .stream()
         .filter( key -> monitoredChannelInterestMap.get( key ) == 0 )
         .filter( key -> timeNow.isAfter( monitoredChannelEventMap.get( key ).plusSeconds( wicaChannelResourceReleaseIntervalInSecs ) ) )
         .collect( Collectors.toList() )
         .forEach( this::discardMonitoredChannel );
   }

   private void discardMonitoredChannel( WicaDataBufferStorageKey storageKey )
   {
      Validate.isTrue( monitoredChannelInterestMap.containsKey( storageKey ) );
      Validate.isTrue( monitoredChannelEventMap.containsKey( storageKey ) );
      Validate.isTrue( monitoredChannelInterestMap.get(storageKey ) == 0 );

      logger.info( "Releasing resources for monitored control system channel associated with storage key: '{}'." , storageKey.toString() );
      applicationEventPublisher.publishEvent( new WicaChannelStopMonitoringEvent( storageKey.getWicaChannel() ) );

      monitoredChannelInterestMap.remove( storageKey );
      monitoredChannelEventMap.remove( storageKey );
   }

/*- Nested Classes -----------------------------------------------------------*/

}
