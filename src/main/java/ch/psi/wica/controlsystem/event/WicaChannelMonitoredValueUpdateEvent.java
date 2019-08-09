/*- Package Declaration ------------------------------------------------------*/
package ch.psi.wica.controlsystem.event;

/*- Imported packages --------------------------------------------------------*/

import ch.psi.wica.model.app.ControlSystemName;
import ch.psi.wica.model.channel.WicaChannelValue;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*- Interface Declaration ----------------------------------------------------*/
/*- Class Declaration --------------------------------------------------------*/

public class WicaChannelMonitoredValueUpdateEvent
{

/*- Public attributes --------------------------------------------------------*/
/*- Private attributes -------------------------------------------------------*/

   private final ControlSystemName controlSystemName;
   private final WicaChannelValue wicaChannelValue;

/*- Main ---------------------------------------------------------------------*/
/*- Constructor --------------------------------------------------------------*/

   public WicaChannelMonitoredValueUpdateEvent( ControlSystemName controlSystemName, WicaChannelValue wicaChannelValue )
   {
      final Logger logger = LoggerFactory.getLogger( WicaChannelMonitoredValueUpdateEvent.class);
      this.controlSystemName = Validate.notNull( controlSystemName );
      this.wicaChannelValue = Validate.notNull( wicaChannelValue );
      logger.trace("Event created: '{}'.", this );
   }

   /*- Class methods ------------------------------------------------------------*/
   /*- Public methods -----------------------------------------------------------*/

   public ControlSystemName getControlSystemName()
   {
      return controlSystemName;
   }

   public WicaChannelValue getWicaChannelValue()
   {
      return wicaChannelValue;
   }

   @Override
   public String toString()
   {
      return "WicaChannelMonitoredValueUpdateEvent{" +
         "controlSystemName=" + controlSystemName +
         ", wicaChannelValue=" + wicaChannelValue +
         '}';
   }

/*- Private methods ----------------------------------------------------------*/
/*- Nested Classes -----------------------------------------------------------*/

}