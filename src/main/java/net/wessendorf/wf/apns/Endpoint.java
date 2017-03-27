package net.wessendorf.wf.apns;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
@Path("apns")
public class Endpoint {

    @GET
    public String get() throws Exception {


        final ApnsSender sender = new ApnsSender();

        sender.connectToAppleAndSendNotification();

        return "yep";
    }

}