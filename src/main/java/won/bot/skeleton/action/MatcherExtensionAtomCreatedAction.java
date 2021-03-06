package won.bot.skeleton.action;

import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.extensions.matcher.MatcherExtensionAtomCreatedEvent;
import won.bot.skeleton.context.PollutionWarningBotContextWrapper;
import won.bot.skeleton.impl.AirQualityDataSchema;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.util.DefaultAtomModelWrapper;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.*;

public class MatcherExtensionAtomCreatedAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static double µG_O3 = 120f;
    private static double µG_NO2 = 400f;
    private static double µG_SO2 = 500f;
    private static double µG_CO = 300f;
    private static double µG_PM10 = 50f;
    private static double µG_PM25 = 25f;
    private static double µG_TSP = 80f;

    private static final String o3 = "o3";
    private static final String no2 = "no2";
    private static final String pm10 = "pm10";
    private static final String tsp = "tsp";
    private static final String pm25 = "pm25";
    private static final String co = "co";
    private static final String so2 = "so2";


    private static Map<String, Double> values = new HashMap<>();

    public MatcherExtensionAtomCreatedAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();

        if (!(event instanceof MatcherExtensionAtomCreatedEvent) || !(getEventListenerContext().getBotContextWrapper() instanceof PollutionWarningBotContextWrapper)) {
            logger.error("MatcherExtensionAtomCreatedAction can only handle MatcherExtensionAtomCreatedEvent and only works with PollutionWarningBotContextWrapper");
            return;
        }

        PollutionWarningBotContextWrapper botContextWrapper = (PollutionWarningBotContextWrapper) ctx.getBotContextWrapper();
        MatcherExtensionAtomCreatedEvent atomCreatedEvent = (MatcherExtensionAtomCreatedEvent) event;

        DefaultAtomModelWrapper defaultWrapper = new DefaultAtomModelWrapper(atomCreatedEvent.getAtomData());
        Collection<String> tags = defaultWrapper.getAllTags();
        if (!tags.contains("AirQualityData")) {
            return;
        }

        Map<URI, Set<URI>> connectedSocketsMapSet = botContextWrapper.getConnectedSockets();

        String[] output = getAirData(atomCreatedEvent, defaultWrapper, event);

        List<String> subs = Message2User.getSubscribers().get(output[1]);

        if (subs != null) {
            for (Map.Entry<URI, Set<URI>> entry : connectedSocketsMapSet.entrySet()) {
                URI senderSocket = entry.getKey();
                Set<URI> targetSocketsSet = entry.getValue();
                for (URI targetSocket : targetSocketsSet) {
                    if (subs.contains(senderSocket.toString())) {
                        logger.info("TODO: Send MSG(" + senderSocket + "->" + targetSocket + ") that we registered that an Atom was created, atomUri is: " + atomCreatedEvent.getAtomURI());
                        WonMessage wonMessage = WonMessageBuilder
                                .connectionMessage()
                                .sockets()
                                .sender(senderSocket)
                                .recipient(targetSocket)
                                .content()
                                .text(output[0])
                                .build();
                        ctx.getWonMessageSender().prepareAndSendMessage(wonMessage);
                    }
                }
            }
        }
    }

    private String[] getAirData(MatcherExtensionAtomCreatedEvent atomCreatedEvent, DefaultAtomModelWrapper defaultWrapper, Event event) {
        //todo: substitute random values w/ actual values read from defaultWrapper
        String[] outData = new String[2];
        Model m = defaultWrapper.getAtomModel();
        Resource atom = defaultWrapper.getAtomModel().getResource(((MatcherExtensionAtomCreatedEvent) event).getAtomURI().toString());
        String output = "";
        String data = "";

        Resource addr = atom.getPropertyResourceValue(AirQualityDataSchema.LOCATION).getPropertyResourceValue(AirQualityDataSchema.PLACE_ADDRESS);

        String country_val = addr.getProperty(AirQualityDataSchema.ADDR_COUNTRY).getString();
        String region_val = addr.getProperty(AirQualityDataSchema.ADDR_CITY).getString();

        String country = "Country: " + country_val + "\n";
        String location = "Locality: " + addr.getProperty(AirQualityDataSchema.ADDR_LOCALITY).getString() + "\n";
        String region = "Region: " + region_val + "\n \n";

        output += country + location + region;

        StmtIterator it = atom.getPropertyResourceValue(AirQualityDataSchema.LOCATION).listProperties(AirQualityDataSchema.PLACE_MEASUREMENT);

        while (it.hasNext()) {
            data = "";
            Statement st = it.next();
            String param = st.getProperty(AirQualityDataSchema.MEASURE_PARAM).getString();
            String unit = st.getProperty(AirQualityDataSchema.MEASURE_UNIT).getString();
            String date = st.getProperty(AirQualityDataSchema.MEASURE_DATETIME).getString();
            String param_name = st.getProperty(AirQualityDataSchema.MEASURE_PARAM_NAME).getString();

            double value = st.getProperty(AirQualityDataSchema.MEASURE_VALUE).getDouble();
            double standard = getStandardByParam(param);

            if (standard > 0) {
                data += (param_name + ": " + value + " " + unit + "\nAirIndex: " + getCat(value, standard) + "\nDate: " + date + "\n");
                output += (data) + " \n";
                values.put(country_val.toLowerCase() + '/' + region_val.toLowerCase() + '/' + param.toLowerCase(), value);
            }
        }

        System.out.println(output);
        outData[0] = output;
        outData[1] = country_val.toLowerCase() + '/' + region_val.toLowerCase();
        return outData;
    }

    public static double getStandardByParam(String param) {
        switch (param) {
            case o3:
                return µG_O3;
            case no2:
                return µG_NO2;
            case co:
                return µG_CO;
            case so2:
                return µG_SO2;
            case pm10:
                return µG_PM10;
            case pm25:
                return µG_PM25;
            case tsp:
                return µG_TSP;
        }
        return -1;
    }

    public static String getFullName(String param) {
        switch (param) {
            case o3:
                return "Ozone";
            case no2:
                return "Nitrogendioxide";
            case co:
                return "Carbonmonoxide";
            case so2:
                return "Sulfurdioxide";
            case pm10:
                return "Atmospheric aerosol particles(size 10µm)";
            case pm25:
                return "Atmospheric aerosol particles(size 2.5µm)";
            case tsp:
                return "Total suspended particles";
        }
        return "";
    }

    public static String getCat(double value, double opt) {
        double val = value / opt * 100;
        if (val < 33) {
            return "Very good";
        } else if (val < 66) {
            return "Good";
        } else if (val < 99) {
            return "Fair";
        } else if (val < 149) {
            return "Poor";
        } else {
            return "Very poor";
        }
    }

    public static String getCatByNr(int nr) {
        switch (nr) {
            case 0:
                return "Very good";
            case 1:
                return "Good";
            case 2:
                return "Fair";
            case 3:
                return "Poor";
            default:
                return "Very poor";
        }
    }

    public static int getCatNr(double value, double opt) {
        double val = value / opt * 100;
        if (val < 33) {
            return 0;
        } else if (val < 66) {
            return 1;
        } else if (val < 99) {
            return 2;
        } else if (val < 149) {
            return 3;
        } else {
            return 4;
        }
    }

    public static Map<String, Double> getValues() {
        return values;
    }
}
