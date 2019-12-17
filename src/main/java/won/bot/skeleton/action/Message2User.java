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
    private final String helpMsg =
            "Your commands: \n"+
                    "get (a-z) \n" +"get all available coutries which names start with a letter between 'a' and 'z' \n" +
                    "example: get (a-c) --> AT \n"+
                    "______________________________\n"+
                    "country/get (a-z) \n" + "get all available locations in the specified country which names start with a letter between 'a' and 'z' \n"+
                    "example: AT/get (a-b)  --> Amstetten \n"+
                    "______________________________\n"+
                    "sub country/location \n" +"get a message you everytime a new atom of the specified location is created. \n"+
                    "example: sub AT/Amstetten \n"+
                    "______________________________\n"+
                    "unsub country/location \nget no more messages everytime a new atom of the specified location is created. \n"+
                    "example: unsub AT/Amstetten \n";

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
            Pattern warning_regex = Pattern.compile("^warnings [0-4] \\([a-z]-[a-z]\\)");
            //System.out.println("regexGiveCities: " + regexGiveCities);
            //System.out.println("regexGiveCountries: " + regexGiveCountries);
            //System.out.println("sub_regex: " + sub_regex);
            //System.out.println("unsub_regex: " + unsub_regex);
            //System.out.println("msg: " + message);


            String returnMsg = "Sorry, I couln´t recognize your input parameters. Did you check for typos?";

            if (sub_regex.matcher(message).find()) {
                message = message.split("sub ")[1];
                List<String> subsc;
                subsc = subscribers.get(message);
                if (subsc == null) {
                    List<String> uri = new ArrayList<>();
                    uri.add(socketUri.toString());
                    subscribers.put(message, uri);
                    if (isValid(message)) returnMsg = "Subscribed to: " + message;
                } else {
                    subsc.add(socketUri.toString());
                    subscribers.put(message, subsc);
                    if (isValid(message)) returnMsg = "Already subscribed to: " + message;
                }
            } else if (unsub_regex.matcher(message).find()) {
                message = message.split("unsub ")[1];
                List<String> subsc;
                subsc = subscribers.get(message);
                if (subsc != null) {
                    subsc.remove(socketUri.toString());
                    subscribers.put(message, subsc);
                    if (isValid(message)) returnMsg = "Unsubscribed from: " + message;
                }

            } else if (regexGiveCountries.matcher(message).find()) {
                char[] bounds = getBounds(message);
                Set<String> keys = MatcherExtensionAtomCreatedAction.getValues().keySet().stream().filter(s -> charIsInRange(bounds[0], bounds[1], s.charAt(0))).collect(Collectors.toSet());
                List<String> countries = new ArrayList<>();
                if (isValid(message)) returnMsg = "Possible countries are: \n";
                for (String s : keys) {
                    String country = s.split("/")[0];
                    if (!countries.contains(country)) {
                        returnMsg += country.toUpperCase() + "\n";
                        countries.add(country);
                    }
                }

            } else if (regexGiveCities.matcher(message).find()) {
                char[] bounds = getBounds(message);
                String country = message.split("/")[0];
                Set<String> keys = MatcherExtensionAtomCreatedAction.getValues().keySet().stream().filter(s -> s.split("/")[0].compareTo(country) == 0 && charIsInRange(bounds[0], bounds[1], s.split("/")[1].charAt(0))).collect(Collectors.toSet());
                List<String> cities = new ArrayList<>();
                if (isValid(message)) returnMsg = "Possible cities are: \n";
                for (String s : keys) {
                    String city = s.split("/")[1];
                    if (!cities.contains(city)) {
                        returnMsg += city + "\n";
                        cities.add(city);
                    }
                }

            } else if (warning_regex.matcher(message).find()) {
                char[] bounds = getBounds(message);
                String cat = message.split(" ")[1];
                Set<String> keys = MatcherExtensionAtomCreatedAction.getValues().keySet().stream().filter(s -> charIsInRange(bounds[0], bounds[1], s.charAt(0))).collect(Collectors.toSet());
                returnMsg = "Cities with the measured param which were "+MatcherExtensionAtomCreatedAction.getCatByNr(Integer.parseInt(cat))+ " are: \n";

                List<String> data = new ArrayList<>();
                for (String s : keys) {
                    String param = s.split("/")[2];
                    double standard = MatcherExtensionAtomCreatedAction.getStandardByParam(param);
                    int lvl = MatcherExtensionAtomCreatedAction.getCatNr(MatcherExtensionAtomCreatedAction.getValues().get(s), standard);
                    if (lvl == Integer.parseInt(cat)) {
                        if(!data.contains(s))data.add(s);
                    }
                }
                Collections.sort(data);
                for (String s: data){
                    returnMsg += s + "\n";
                }
            }else if (message.compareTo("help")==0){
                returnMsg = helpMsg;
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

    private boolean isValid(String message) {
        Set<String> keys = MatcherExtensionAtomCreatedAction.getValues().keySet();
        for (String s : keys) {
            if (s.contains(message)) {
                return true;
            }
        }
        return false;
    }
}
