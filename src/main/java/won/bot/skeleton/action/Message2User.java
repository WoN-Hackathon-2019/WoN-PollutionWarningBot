package won.bot.skeleton.action;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Regexp;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.MessageEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.skeleton.context.SkeletonBotContextWrapper;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by MS on 24.09.2019.
 */
public class Message2User extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static Map<String, List<String>> getSubscribers() {
        return subscribers;
    }

    private static Map<String, List<String>> subscribers = new HashedMap();

    public Message2User(EventListenerContext ctx) {
        super(ctx);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        logger.info("MessageEvent received");
        EventListenerContext ctx = getEventListenerContext();
        if (event instanceof MessageFromOtherAtomEvent
                && ctx.getBotContextWrapper() instanceof SkeletonBotContextWrapper) {
            Connection con = ((MessageFromOtherAtomEvent) event).getCon();
            URI msgAtomUri = con.getAtomURI();
            URI targetUri = con.getTargetAtomURI();
            URI socketUri = con.getSocketURI();
            String message = "";
            try {
                WonMessage msg = ((MessageEvent) event).getWonMessage();
                message = extractTextMessageFromWonMessage(msg);
            } catch (Exception te) {
                logger.error(te.getMessage());
            }
            message = message.toLowerCase();
            String responseMessge = "You want specific information about Austria? Just type the name of the 'AT'!";
            Pattern regexGiveCities = Pattern.compile("^[a-z]+/get \\([a-z]-[a-z]\\)");
            Pattern regexGiveCountries = Pattern.compile("^get \\([a-z]-[a-z]\\)");
            Pattern sub_regex = Pattern.compile("^sub [a-z]+/[a-z]+");
            Pattern unsub_regex = Pattern.compile("^unsub [a-z]+/[a-z]+");
            System.out.println("regexGiveCities: " + regexGiveCities);
            System.out.println("regexGiveCountries: " + regexGiveCountries);
            System.out.println("sub_regex: " + sub_regex);
            System.out.println("unsub_regex: " + unsub_regex);
            System.out.println("msg: " + message);


            String returnMsg = "";

            if (sub_regex.matcher(message).find()) {
                System.out.println("sub_regex");
                message = message.split("sub ")[1];
                List<String> subsc;
                subsc = subscribers.get(message);
                if (subsc == null) {
                    List<String> uri = new ArrayList<>();
                    uri.add(socketUri.toString());
                    subscribers.put(message, uri);
                    returnMsg = "Subscribed to: " + message;
                } else {
                    subsc.add(socketUri.toString());
                    subscribers.put(message, subsc);
                    returnMsg = "Already subscribed to: " + message;
                }
            } else if (unsub_regex.matcher(message).find()) {
                System.out.println("unsub_regex");
                message = message.split("unsub ")[1];
                List<String> subsc;
                subsc = subscribers.get(message);
                if (subsc != null) {
                    subsc.remove(socketUri.toString());
                    subscribers.put(message, subsc);
                    returnMsg = "Unsubscribed from: " + message;
                }

                //ChuckNorrisJoke chuckNorrisJoke = JokeBotsApi.fetchJokeData(this.jsonURL);
                //String newJokeText = chuckNorrisJoke.getValue();
                //responseMessge = "Okay, how about this one: \n" + newJokeText;
            } else if (regexGiveCountries.matcher(message).find()) {
                char[] bounds = getBounds(message);
                Set<String> keys = MatcherExtensionAtomCreatedAction.getValues().keySet().stream().filter(s -> charIsInRange(bounds[0], bounds[1], s.charAt(0))).collect(Collectors.toSet());
                List<String> countries = new ArrayList<>();
                returnMsg = "Possible countries are: \n";
                for (String s : keys) {
                    String country = s.split("/")[0];
                    if(!countries.contains(country)){
                        returnMsg += country.toUpperCase() + "\n";
                        countries.add(country);
                    }
                }

            }else if (regexGiveCities.matcher(message).find()) {
                char[] bounds = getBounds(message);
                String country = message.split("/")[0];
                Set<String> keys = MatcherExtensionAtomCreatedAction.getValues().keySet().stream().filter(s -> s.split("/")[0].compareTo(country)==0 && charIsInRange(bounds[0], bounds[1], s.split("/")[1].charAt(0))).collect(Collectors.toSet());
                List<String> cities = new ArrayList<>();
                returnMsg = "Possible cities are: \n";
                for (String s : keys) {
                    String city = s.split("/")[1];
                    if(!cities.contains(city)){
                        returnMsg += city + "\n";
                        cities.add(city);
                    }
                }

            }

            try {
                Model messageModel = WonRdfUtils.MessageUtils.textMessage(returnMsg);
                getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, messageModel));
            } catch (Exception te) {
                logger.error(te.getMessage());
            }
        }
    }

    private boolean charIsInRange(char lower, char upper, char target) {
        return lower <= target && target <= upper;
    }

    private char[] getBounds(String message) {
        String[] boundsString = new String[2];
        try {
            boundsString = message.split("\\(")[1].split("\\)")[0].split("-");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        if (boundsString.length == 2 && boundsString[0] != null && boundsString[1] != null) {
            char[] bounds = new char[]{boundsString[0].charAt(0), boundsString[1].charAt(0)};
            return bounds;
        }
        return null;
    }


    private String extractTextMessageFromWonMessage(WonMessage wonMessage) {
        if (wonMessage == null)
            return null;
        String message = WonRdfUtils.MessageUtils.getTextMessage(wonMessage);
        return StringUtils.trim(message);
    }
}
