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

public class WicaChannelPolledValueUpdateEvent
{

   /*- Public attributes --------------------------------------------------------*/
   /*- Private attributes -------------------------------------------------------*/

   private final ControlSystemName controlSystemName;
   private final WicaChannelValue wicaChannelValue;

   /*- Main ---------------------------------------------------------------------*/
   /*- Constructor --------------------------------------------------------------*/

   public WicaChannelPolledValueUpdateEvent( ControlSystemName controlSystemName, WicaChannelValue wicaChannelValue )
   {
      final Logger logger = LoggerFactory.getLogger( WicaChannelPolledValueUpdateEvent.class);
      logger.trace("Creating event: WicaChannelPolledValueUpdateEvent");
      this.controlSystemName = Validate.notNull( controlSystemName );
      this.wicaChannelValue = Validate.notNull( wicaChannelValue );
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


/*- Private methods ----------------------------------------------------------*/
/*- Nested Classes -----------------------------------------------------------*/

}