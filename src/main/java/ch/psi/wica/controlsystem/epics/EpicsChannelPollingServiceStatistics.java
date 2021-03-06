/*- Package Declaration ------------------------------------------------------*/

package ch.psi.wica.controlsystem.epics;

/*- Imported packages --------------------------------------------------------*/

import ch.psi.wica.model.app.StatisticsCollectable;
import ch.psi.wica.model.channel.WicaChannelName;
import net.jcip.annotations.ThreadSafe;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/*- Interface Declaration ----------------------------------------------------*/
/*- Class Declaration --------------------------------------------------------*/

@ThreadSafe
public class EpicsChannelPollingServiceStatistics implements StatisticsCollectable
{

/*- Public attributes --------------------------------------------------------*/
/*- Private attributes -------------------------------------------------------*/

   private final AtomicInteger startRequests = new AtomicInteger(0);
   private final AtomicInteger stopRequests = new AtomicInteger(0);
   private final AtomicInteger pollCycleCount = new AtomicInteger(0);
   private final AtomicInteger pollSuccessCount = new AtomicInteger(0);
   private final AtomicInteger pollFailureCount = new AtomicInteger(0);

   private final Map<WicaChannelName, ScheduledFuture<?>> executorMap;

/*- Main ---------------------------------------------------------------------*/
/*- Constructor --------------------------------------------------------------*/

   public EpicsChannelPollingServiceStatistics( Map<WicaChannelName, ScheduledFuture<?>> executorMap )
   {
      this.executorMap = executorMap;
   }

/*- Class methods ------------------------------------------------------------*/
/*- Public methods -----------------------------------------------------------*/

   @Override
   public Statistics get()
   {
      return new Statistics( "EPICS POLLING SERVICE",
                             List.of( new StatisticsItem("- Start Polling Requests", getStartRequests() ),
                                      new StatisticsItem("- Stop Polling Requests", getStopRequests() ),
                                      new StatisticsItem("- EPICS Pollers: Total", getTotalPollerCount() ),
                                      new StatisticsItem("- EPICS Pollers: Cancelled", getCancelledPollerCount() ),
                                      new StatisticsItem("- EPICS Pollers: Completed", getCompletedPollerCount() ),
                                      new StatisticsItem("- EPICS Pollers: Polling Cycle: Total Count", getPollCycleCount() ),
                                      new StatisticsItem("- EPICS Pollers: Polling Cycle: Success Count", getPollSuccessCount() ),
                                      new StatisticsItem("- EPICS Pollers: Polling Cycle: Failure Count", getPollFailureCount() ) )
                             );
   }

   @Override
   public void reset()
   {
      startRequests.set( 0 );
      stopRequests.set( 0 );
      pollCycleCount.set( 0 );
      pollSuccessCount.set( 0 );
      pollFailureCount.set( 0 );
   }

   public List<String> getChannelNames()
   {
      return executorMap
            .keySet()
            .stream()
            .map(WicaChannelName::asString)
            .collect(Collectors.toUnmodifiableList() );
   }

/*- Package-access methods ---------------------------------------------------*/

   void incrementStartRequests()
   {
      startRequests.incrementAndGet();
   }
   void incrementStopRequests()
   {
      stopRequests.incrementAndGet();
   }

   void incrementPollCycleCount()
   {
      pollCycleCount.incrementAndGet();
   }

   void updatePollingResult( boolean success )
   {
      if ( success )
      {
         pollSuccessCount.incrementAndGet();
      }
      else
      {
         pollFailureCount.incrementAndGet();
      }
   }

/*- Private methods ----------------------------------------------------------*/

   private String getStartRequests()
{
   return String.valueOf( startRequests.get());
}
   private String getStopRequests()
   {
      return String.valueOf( stopRequests.get());
   }

   private String getTotalPollerCount()
   {
      return String.valueOf( executorMap.keySet().size() );
   }

   private String getCompletedPollerCount()
   {
      return String.valueOf( executorMap.values().stream().filter( Future::isDone).count() );
   }

   private String getCancelledPollerCount()
   {
      return String.valueOf( executorMap.values().stream().filter( Future::isCancelled).count() );
   }

   private String getPollCycleCount()
   {
      return String.valueOf( pollCycleCount );
   }

   private String getPollSuccessCount()
   {
      return String.valueOf( pollSuccessCount );
   }

   private String getPollFailureCount()
   {
      return String.valueOf( pollFailureCount );
   }


   /*- Nested Classes -----------------------------------------------------------*/

}