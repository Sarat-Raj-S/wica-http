/*- Package Declaration ------------------------------------------------------*/

package ch.psi.wica.config;

/*- Imported packages --------------------------------------------------------*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/*- Interface Declaration ----------------------------------------------------*/
/*- Class Declaration --------------------------------------------------------*/

@Configuration
class WicaCorsConfigurer implements WebMvcConfigurer
{

/*- Public attributes --------------------------------------------------------*/
/*- Private attributes -------------------------------------------------------*/

   private final Logger logger = LoggerFactory.getLogger(WicaCorsConfigurer.class );

/*- Main ---------------------------------------------------------------------*/
/*- Constructor --------------------------------------------------------------*/
/*- Class methods ------------------------------------------------------------*/
/*- Public methods -----------------------------------------------------------*/

   @Override
   public void addCorsMappings( CorsRegistry registry )
   {
      logger.info( "Configuring CORS...");
      registry.addMapping("/**")
               // The definition below means that when Safari and Chrome load a web
               // page from ANY webserver they will be able to make XHR and SSE
               // requests on this server.
               .allowedOrigins( "*" )
               // Firefox is pickier: as '*' is not an allowed origin the
               // origins must be enabled by hand.
               //.allowedOrigins( "https://mpc2330.psi.ch" )
               .allowCredentials( true );
      logger.info( "CORS configuration completed.");
   }

/*- Private methods ----------------------------------------------------------*/
/*- Nested Classes -----------------------------------------------------------*/

}
