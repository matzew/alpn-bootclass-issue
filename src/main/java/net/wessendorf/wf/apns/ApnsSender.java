package net.wessendorf.wf.apns;

import com.relayrides.pushy.apns.ApnsClient;
import com.relayrides.pushy.apns.ApnsClientBuilder;
import com.relayrides.pushy.apns.ClientNotConnectedException;
import com.relayrides.pushy.apns.PushNotificationResponse;
import com.relayrides.pushy.apns.util.ApnsPayloadBuilder;
import com.relayrides.pushy.apns.util.SimpleApnsPushNotification;
import io.netty.util.concurrent.Future;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * Created by matzew on 3/27/17.
 */
public class ApnsSender {

    final Logger LOGGER = Logger.getLogger(ApnsSender.class.getName());
    final List<String> tokens = Arrays.asList("1234");

    ApnsClient apnsClient = null;

    public ApnsSender() {


        try {

            File file = new File(System.getProperty("java.io.tmpdir") + "/foo.p12");
            FileUtils.copyInputStreamToFile(Thread.currentThread().getContextClassLoader().getResourceAsStream("/AeroDoc.p12"), file);


            apnsClient = new ApnsClientBuilder()
                    .setClientCredentials(file, "AeroDoc")
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void connectToAppleAndSendNotification() throws InterruptedException {
        final Future<Void> connectFuture = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
        connectFuture.await();

        for (final String token: tokens) {

            final SimpleApnsPushNotification pushNotification = createNotification(token, "Here is iteration: " + token);

            final Future<PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture = apnsClient.sendNotification(pushNotification);

            try {
                final PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse =
                        sendNotificationFuture.get();

                if (pushNotificationResponse.isAccepted()) {
                    LOGGER.info("Push notification accepted by APNs gateway." + pushNotificationResponse);
                } else {

                    LOGGER.info("DA-----------------> " + pushNotificationResponse.getTokenInvalidationTimestamp());

                    LOGGER.info("Notification rejected by the APNs gateway: " +
                            pushNotificationResponse.getRejectionReason() + pushNotificationResponse);

                    if (pushNotificationResponse.getTokenInvalidationTimestamp() != null) {
                        LOGGER.info("\t…and the token is invalid as of " +
                                pushNotificationResponse.getTokenInvalidationTimestamp());
                    }
                }
            } catch (final ExecutionException e) {
                LOGGER.severe("Failed to send push notification.");
                e.printStackTrace();

                if (e.getCause() instanceof ClientNotConnectedException) {
                    LOGGER.info("Waiting for client to reconnect…");
                    apnsClient.getReconnectionFuture().await();
                    LOGGER.info("Reconnected.");
                }
            }

        }

        final Future<Void> disconnectFuture = apnsClient.disconnect();
        disconnectFuture.await();


    }







    private SimpleApnsPushNotification createNotification(final String token, final String value) {

        final SimpleApnsPushNotification pushNotification;

        {
            final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
            payloadBuilder.setAlertBody(value);
            payloadBuilder.setSoundFileName("default");

            final String payload = payloadBuilder.buildWithDefaultMaximumLength();

            pushNotification = new SimpleApnsPushNotification(token, null, payload);
        }

        return pushNotification;
    }



}
