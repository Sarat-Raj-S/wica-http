/*- Package Declaration ------------------------------------------------------*/
package ch.psi.wica.services.epics;

/*- Imported packages --------------------------------------------------------*/


import ch.psi.wica.model.WicaChannelMetadata;
import ch.psi.wica.model.WicaChannelName;
import ch.psi.wica.model.WicaChannelValue;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/*- Interface Declaration ----------------------------------------------------*/
/*- Class Declaration --------------------------------------------------------*/

@Ignore
@SpringBootTest
class EpicsChannelMonitorServiceTest
{

/*- Public attributes --------------------------------------------------------*/
/*- Private attributes -------------------------------------------------------*/

   private EpicsChannelMonitorService epicsChannelMonitorService = new EpicsChannelMonitorService( );

   private Consumer<Boolean> stateChangeHandler = ( bool ) -> {};
   private Consumer<WicaChannelMetadata> metadataChangeHandler = ( string ) -> {};
   private Consumer<WicaChannelValue> valueChangeHandler = ( string ) -> {};


/*- Main ---------------------------------------------------------------------*/
/*- Constructor --------------------------------------------------------------*/
/*- Class methods ------------------------------------------------------------*/
/*- Public methods -----------------------------------------------------------*/
/*- Private methods ----------------------------------------------------------*/

   @BeforeEach
   void beforeEach()
   {
      epicsChannelMonitorService = new EpicsChannelMonitorService();
   }

   @AfterEach
   void afterEach()
   {
      epicsChannelMonitorService.close();
      assertEquals( 0, EpicsChannelMonitorService.getChannelConnectionCount() );
   }


   @Test
   void testStartMonitoring_ThrowsNullPointerExceptionWhenEpicsChannelNamesIsNull()
   {
      assertThrows( NullPointerException.class, () -> epicsChannelMonitorService.startMonitoring(null, stateChangeHandler, metadataChangeHandler, valueChangeHandler ) );
   }

   @Test
   void testStartMonitoring_ThrowsNullPointerExceptionWhenStateChangeHandlerIsNull()
   {
      assertThrows( NullPointerException.class, () -> epicsChannelMonitorService.startMonitoring(new WicaChannelName("abcd" ), null, metadataChangeHandler, valueChangeHandler ) );
   }

   @Test
   void testStartMonitoring_ThrowsNullPointerExceptionWhenMetadataChangeHandlerIsNull()
   {
      assertThrows( NullPointerException.class, () -> epicsChannelMonitorService.startMonitoring(new WicaChannelName("abcd" ), stateChangeHandler, null, valueChangeHandler ) );
   }

   @Test
   void testStartMonitoring_ThrowsNullPointerExceptionWhenValueChangeHandlerIsNull()
   {
      assertThrows( NullPointerException.class, () -> epicsChannelMonitorService.startMonitoring(new WicaChannelName("abcd" ), stateChangeHandler, metadataChangeHandler, null ) );
   }

   @Test
   void testGetChannelCreationCount()
   {
      assertEquals( 0, EpicsChannelMonitorService.getChannelCreationCount() );
      epicsChannelMonitorService.startMonitoring(new WicaChannelName("non-existent-channel 1" ), stateChangeHandler, metadataChangeHandler, valueChangeHandler );
      assertEquals( 1, EpicsChannelMonitorService.getChannelCreationCount() );
      epicsChannelMonitorService.startMonitoring(new WicaChannelName("non-existent-channel 1" ), stateChangeHandler, metadataChangeHandler, valueChangeHandler );
      assertEquals( 2, EpicsChannelMonitorService.getChannelCreationCount());
      epicsChannelMonitorService.startMonitoring(new WicaChannelName("non-existent-channel 2" ), stateChangeHandler, metadataChangeHandler, valueChangeHandler );
      assertEquals( 3, EpicsChannelMonitorService.getChannelCreationCount() );
      epicsChannelMonitorService.close();
      assertEquals( 0, EpicsChannelMonitorService.getChannelCreationCount() );
   }

   @Test
   void testGetMonitorCreationCount()
   {
      assertEquals( 0, EpicsChannelMonitorService.getMonitorCreationCount() );
      epicsChannelMonitorService.startMonitoring(new WicaChannelName("non-existent-channel 1" ), stateChangeHandler, metadataChangeHandler, valueChangeHandler );
      assertEquals( 0, EpicsChannelMonitorService.getMonitorCreationCount() );
      epicsChannelMonitorService.startMonitoring(new WicaChannelName("non-existent-channel 2" ), stateChangeHandler, metadataChangeHandler, valueChangeHandler );
      assertEquals( 0, EpicsChannelMonitorService.getMonitorCreationCount() );
      epicsChannelMonitorService.close();
      assertEquals( 0, EpicsChannelMonitorService.getMonitorCreationCount());
   }

   @Test
   void testGetChannelConnectionCount() throws InterruptedException
   {
      assertEquals( 0, EpicsChannelMonitorService.getChannelConnectionCount());
      epicsChannelMonitorService.startMonitoring(new WicaChannelName("test:db_ok" ), stateChangeHandler, metadataChangeHandler, valueChangeHandler );
      Thread.sleep( 1_000 );
      assertEquals( 1, EpicsChannelMonitorService.getChannelConnectionCount());
      epicsChannelMonitorService.close();
      assertEquals( 0, EpicsChannelMonitorService.getChannelConnectionCount());
   }


   @Test
   void testStartMonitoring_verifyInitialConnectBehaviour_HandlersAreNotNotifiedIfChannelOffline() throws InterruptedException
   {
      assertEquals( 0, EpicsChannelMonitorService.getChannelCreationCount() );
      final Consumer<Boolean> stateChangeHandlerMock = Mockito.mock( BooleanConsumer.class );
      final Consumer<WicaChannelValue> valueChangeHandlerMock = Mockito.mock(EpicsChannelValueConsumer.class );
      epicsChannelMonitorService.startMonitoring(new WicaChannelName("non-existent-channel" ), stateChangeHandlerMock, metadataChangeHandler, valueChangeHandlerMock );
      Thread.sleep( 1_000 );
      Mockito.verify( stateChangeHandlerMock, never() ).accept( anyBoolean() );
      Mockito.verify( valueChangeHandlerMock, never() ).accept( any() );
   }

   @Test
   void testStartMonitoring_verifyInitialConnectBehaviour_NotificationSequence() throws InterruptedException
   {
      assertEquals( 0, EpicsChannelMonitorService.getChannelCreationCount() );
      final Consumer<Boolean> stateChangeHandlerMock = Mockito.mock( BooleanConsumer.class );
      final Consumer<WicaChannelValue> valueChangeHandlerMock = Mockito.mock(EpicsChannelValueConsumer.class );
      epicsChannelMonitorService.startMonitoring(new WicaChannelName("test:db_ok" ), stateChangeHandlerMock, metadataChangeHandler, valueChangeHandlerMock );
      Thread.sleep( 1_000 );
      final InOrder inOrder = inOrder( stateChangeHandlerMock, valueChangeHandlerMock );
      inOrder.verify( stateChangeHandlerMock ).accept(true );
   }

   @Test
   void testStartMonitoring_verifyMonitorDisconnectBehaviour()
   {
      // TODO: write this
   }

   @Test
   void testStartMonitoring_verifyBehaviourWithDifferentChannelTypes()
   {
      // TODO: write this
   }

   @Test
   void testStopMonitoring_verifyConnectionCountChanges() throws InterruptedException
   {
      assertEquals( 0, EpicsChannelMonitorService.getChannelConnectionCount() );
      epicsChannelMonitorService.startMonitoring( new WicaChannelName( "test:db_ok" ), stateChangeHandler, metadataChangeHandler, valueChangeHandler );
      Thread.sleep( 1_000 );
      assertEquals( 1, EpicsChannelMonitorService.getChannelConnectionCount() );
      epicsChannelMonitorService.stopMonitoring( new WicaChannelName( "test:db_ok" ) );
      assertEquals( 0, EpicsChannelMonitorService.getChannelConnectionCount() );
   }

   @Test
   void testStopMonitoring_verifyIllegalArgumentExceptionWhenChannelNotRecognised() throws InterruptedException
   {
      assertEquals( 0, EpicsChannelMonitorService.getChannelConnectionCount() );
      epicsChannelMonitorService.startMonitoring( new WicaChannelName( "test:db_ok" ), stateChangeHandler, metadataChangeHandler, valueChangeHandler );
      Thread.sleep( 1_000 );
      assertEquals( 1, EpicsChannelMonitorService.getChannelConnectionCount() );
      assertThrows( IllegalArgumentException.class, () ->  epicsChannelMonitorService.stopMonitoring( new WicaChannelName( "XXXXX" ) ) );
   }


/*- Nested Classes -----------------------------------------------------------*/

   // Note: these interfaces exist to avoid the need for an unchecked cast in
   // some of the tests above
   private interface BooleanConsumer extends Consumer<Boolean> {}
   private interface EpicsChannelValueConsumer extends Consumer<WicaChannelValue> {}

}
