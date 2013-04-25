package com.geekfinder;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import java.util.logging.Logger;

import javax.inject.Named;

@Api(name = "contact")
public class ContactEndpoint {

    private static final Logger log = Logger.getLogger(ContactEndpoint.class.getName());

	  @ApiMethod(
		  name = "Contact.sendEmail",
	      path = "contact/sendemail/{emailAddress}",
          httpMethod = HttpMethod.POST
	   )
	  public void sendEmail(@Named("emailAddress") String emailAddress) {
	      log.info("An informational message.");
	      return ;
	   }
	
}
