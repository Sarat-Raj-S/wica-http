/*- Package Declaration ------------------------------------------------------*/
package ch.psi.wica.services;

/*- Imported packages --------------------------------------------------------*/

import ch.psi.wica.model.EpicsChannelMetadata;
import ch.psi.wica.model.EpicsChannelName;
import ch.psi.wica.model.EpicsChannelValue;
import ch.psi.wica.model.EpicsChannelDataStream;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/*- Interface Declaration ----------------------------------------------------*/
/*- Class Declaration --------------------------------------------------------*/

@ThreadSafe
@Service
public class EpicsChannelDataService
{

/*- Public attributes --------------------------------------------------------*/
/*- Private attributes -------------------------------------------------------*/

   private final Logger logger = LoggerFactory.getLogger (EpicsChannelDataService.class );

   private static final Map<EpicsChannelName,Integer> channelInterestMap = Collections.synchronizedMap(new HashMap<>() );
   private static final Map<EpicsChannelName, EpicsChannelValue> channelValueStash = Collections.synchronizedMap(new HashMap<>() );
   private static final Map<EpicsChannelName, EpicsChannelMetadata> channelMetadataStash = Collections.synchronizedMap(new HashMap<>() );

   private EpicsChannelMonitorService epicsChannelMonitorService;


/*- Main ---------------------------------------------------------------------*/
/*- Constructor --------------------------------------------------------------*/

   private EpicsChannelDataService( EpicsChannelMonitorService epicsChannelMonitorService )
   {
      Validate.notNull ( epicsChannelMonitorService );
      this.epicsChannelMonitorService = new EpicsChannelMonitorService();
   }

/*- Class methods ------------------------------------------------------------*/
/*- Public methods -----------------------------------------------------------*/

   public void startMonitoring( EpicsChannelDataStream epicsChannelDataStream )
   {
      Validate.notNull(epicsChannelDataStream);

      for ( EpicsChannelName channelName : epicsChannelDataStream.getChannels() )
      {
         // If the channel is already being monitored increment the interest count.
         if ( channelInterestMap.containsKey( channelName ) )
         {
            channelInterestMap.put( channelName, channelInterestMap.get( channelName ) + 1 );
         }
         // If the channel is NOT already being monitored start monitoring it.
         else
         {
            logger.info("subscribing to new channel: '{}'", channelName ) ;
            // Now startMonitoring
            final Consumer<Boolean> stateChangedHandler = b -> stateChanged( channelName, b );
            final Consumer<EpicsChannelValue> valueChangedHandler =  v -> valueChanged( channelName, v );
            final Consumer<EpicsChannelMetadata> metadataChangedHandler =  v -> metadataChanged( channelName, v );
            epicsChannelMonitorService.startMonitoring( channelName, stateChangedHandler, metadataChangedHandler, valueChangedHandler );
            channelInterestMap.put( channelName, 1 );
         }
      }
   }

   public void stopMonitoring( EpicsChannelDataStream epicsChannelDataStream )
   {
      Validate.notNull(epicsChannelDataStream);

      for ( EpicsChannelName channelName : epicsChannelDataStream.getChannels() )
      {
         // Validate the precondition that
         Validate.isTrue( channelInterestMap.containsKey( channelName ) );

         channelInterestMap.put( channelName, channelInterestMap.get( channelName ) - 1 );

         if ( channelInterestMap.get( channelName ) == 0 )
         {
            logger.info( "unsubscribing to channel: '{}'", channelName ) ;
            epicsChannelMonitorService.stopMonitoring( channelName );
         }
      }
   }


   public Map<EpicsChannelName,EpicsChannelValue> getChannelValues( EpicsChannelDataStream epicsChannelDataStream )
   {
      Validate.notNull(epicsChannelDataStream);

      return epicsChannelDataStream.getChannels().stream()
                                                  .filter( channelValueStash::containsKey )
                                                  .collect( Collectors.toMap( Function.identity(), channelValueStash::get ) );
   }


   public Map<EpicsChannelName,EpicsChannelValue> getChannelValuesUpdatedSince( EpicsChannelDataStream epicsChannelDataStream, LocalDateTime sinceDateTime )
   {
      Validate.notNull(epicsChannelDataStream);

      return epicsChannelDataStream.getChannels().stream()
                                                  .filter( channelValueStash::containsKey )
                                                  .filter( c -> channelValueStash.get( c ).getTimestamp().isAfter( sinceDateTime ) )
                                                  .collect( Collectors.toMap( Function.identity(), channelValueStash::get ) );
   }

   public Map<EpicsChannelName,EpicsChannelMetadata> getChannelMetadata( EpicsChannelDataStream epicsChannelDataStream )
   {
      Validate.notNull(epicsChannelDataStream);

      return epicsChannelDataStream.getChannels().stream()
            .filter( channelMetadataStash::containsKey )
            .collect( Collectors.toMap( Function.identity(), channelMetadataStash::get ) );
   }


   /*- Private methods ----------------------------------------------------------*/

   /**
    * Handles a connection state change on the underlying EPICS channel monitor.
    *
    * @param epicsChannelName the name of the channel whose connection state changed.
    * @param isConnected the new connection state.
    */
   private void stateChanged( EpicsChannelName epicsChannelName, Boolean isConnected )
   {
      Validate.notNull( epicsChannelName, "The 'epicsChannelName' argument was null" );
      Validate.notNull( isConnected, "The 'isConnected' argument was null"  );

      logger.info("'{}' - connection state changed to '{}'.", epicsChannelName, isConnected);

      if ( ! isConnected )
      {
         logger.debug("'{}' - value changed to NULL to indicate the connection was lost.", epicsChannelName);
         channelValueStash.put( epicsChannelName, null );
      }
   }

   /**
    * Handles a value change on the underlying EPICS channel monitor.
    *
    * @param epicsChannelName the name of the channel whose monitor changed.
    * @param epicsChannelValue the new value.
    */
   private void valueChanged( EpicsChannelName epicsChannelName, EpicsChannelValue epicsChannelValue )
   {
      Validate.notNull( epicsChannelName, "The 'epicsChannelName' argument was null");
      Validate.notNull( epicsChannelValue,"The 'newValue' argument was null");

      logger.trace("'{}' - value changed to: '{}'", epicsChannelName, epicsChannelValue );
      channelValueStash.put( epicsChannelName,  epicsChannelValue );
   }

   /**
    * Handles a value change on the metadata associated with an EPICS channel.
    *
    * @param epicsChannelName the name of the channel for whom metadata is now available
    * @param epicsChannelMetadata the metadata
    */
   private void metadataChanged( EpicsChannelName epicsChannelName, EpicsChannelMetadata epicsChannelMetadata )
   {
      Validate.notNull( epicsChannelName, "The 'epicsChannelName' argument was null");
      Validate.notNull( epicsChannelMetadata,"The 'epicsChannelMetadata' argument was null");

      logger.trace("'{}' - metadata changed to: '{}'", epicsChannelName, epicsChannelMetadata );
      channelMetadataStash.put( epicsChannelName,  epicsChannelMetadata );
   }


   /*- Nested Classes -----------------------------------------------------------*/

}