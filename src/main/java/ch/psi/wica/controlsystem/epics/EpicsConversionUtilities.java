/*- Package Declaration ------------------------------------------------------*/
package ch.psi.wica.controlsystem.epics;

/*- Imported packages --------------------------------------------------------*/

import ch.psi.wica.model.*;
import org.epics.ca.data.AlarmSeverity;
import org.epics.ca.data.AlarmStatus;
import org.epics.ca.data.Control;
import org.epics.ca.data.Timestamped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;


/*- Interface Declaration ----------------------------------------------------*/
/*- Class Declaration --------------------------------------------------------*/

/**
 * Utilities for converting EPICS data entities to data entities in the
 * more general Wica data abstraction.
 */
class EpicsConversionUtilities
{

/*- Public attributes --------------------------------------------------------*/
/*- Private attributes -------------------------------------------------------*/

   private final static Logger logger = LoggerFactory.getLogger( EpicsConversionUtilities.class );

/*- Main ---------------------------------------------------------------------*/
/*- Constructor --------------------------------------------------------------*/
/*- Class methods ------------------------------------------------------------*/

   static WicaChannelAlarmSeverity fromEpics( AlarmSeverity caAlarmSeverity )
   {
      return WicaChannelAlarmSeverity.valueOf( caAlarmSeverity.toString() );
   }

   static WicaChannelAlarmStatus fromEpics( AlarmStatus caAlarmStatus )
   {
      return WicaChannelAlarmStatus.of( caAlarmStatus.ordinal() );
   }

   // TODO: this ties the current location to PSI's site. Should be made configurable.
   static LocalDateTime getEpicsTimestamp( long secondsPastEpicsEpoch, int nanoseconds )
   {
      final Instant instant = Instant.ofEpochSecond(secondsPastEpicsEpoch, nanoseconds );
      final ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of("Europe/Zurich") );
      return zonedDateTime.toLocalDateTime();
   }


/*- Public methods -----------------------------------------------------------*/
/*- Private methods ----------------------------------------------------------*/
/*- Nested Classes -----------------------------------------------------------*/

}