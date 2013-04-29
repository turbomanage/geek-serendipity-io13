package com.geekfinder;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import java.util.logging.Logger;

import javax.inject.Named;

@Api(name = "clientlogging", clientIds = "", audiences = "")
public class ClientLoggingEndpoint {

    private static final Logger log = Logger.getLogger(ClientLoggingEndpoint.class.getName());

    
	  @ApiMethod(
		  name = "logs.log",
	      path = "logs/log/{message}",
          httpMethod = HttpMethod.POST
	   )
	  public void logFromClient(@Named("message") String message) {
	      log.info("client log: " + message);	      
	      return ;
	   }
	
}
