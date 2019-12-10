package won.bot.skeleton.action;

import javafx.beans.property.Property;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.extensions.matcher.MatcherExtensionAtomCreatedEvent;
import won.bot.skeleton.context.SkeletonBotContextWrapper;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.SCHEMA;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MatcherExtensionAtomCreatedAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static float PPM_OZONE = 0.1f;
    private static float PPM_NO = 0.12f;
    private static float PPM_SDIO = 0.2f;
    private static float PPM_CO = 9f;
    private static float µG_PM10 = 50f;
    private static float µG_PM2_5 = 25f;
    private static float µG_TSP = 80f;

    public MatcherExtensionAtomCreatedAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();

        if (!(event instanceof MatcherExtensionAtomCreatedEvent) || !(getEventListenerContext().getBotContextWrapper() instanceof SkeletonBotContextWrapper)) {
            logger.error("MatcherExtensionAtomCreatedAction can only handle MatcherExtensionAtomCreatedEvent and only works with SkeletonBotContextWrapper");
            return;
        }

        SkeletonBotContextWrapper botContextWrapper = (SkeletonBotContextWrapper) ctx.getBotContextWrapper();
        MatcherExtensionAtomCreatedEvent atomCreatedEvent = (MatcherExtensionAtomCreatedEvent) event;

        DefaultAtomModelWrapper defaultWrapper = new DefaultAtomModelWrapper(atomCreatedEvent.getAtomData());
        Collection<String> tags = defaultWrapper.getAllTags();
        if(!tags.contains("AirData")) {//todo: filter tags set in atoms of AirQualityBot
            return;
        }

        Map<URI, Set<URI>> connectedSocketsMapSet = botContextWrapper.getConnectedSockets();

/*
        URI uri =  atomCreatedEvent.getAtomURI();
        Map<String, Object> map = botContextWrapper.getBotContext().loadObjectMap(uri.toString());


        botContextWrapper.getBotContext().loadFromObjectMap("s:title", uri.toString());
       for(String s : map.keySet()){
            String val = (String) map.get(s);
            logger.info("Key: " + s + ", Val: " + val);
        }


       //DefaultAtomModelWrapper wrapper = new DefaultAtomModelWrapper(atomCreatedEvent.getAtomData());

        for(String s:defaultWrapper.getAllTags()){
            logger.info("Tag: " + s);
        }
        for(String s:defaultWrapper.getAllTitles()){
            logger.info("Title: " + s);
        }
        for(String s:defaultWrapper.getContentPropertyStringValues(RDFS.label, "Titel")){
            logger.info("Title: " + s);
        }
*/



        for (Map.Entry<URI, Set<URI>> entry : connectedSocketsMapSet.entrySet()) {
            URI senderSocket = entry.getKey();
            Set<URI> targetSocketsSet = entry.getValue();
            for (URI targetSocket : targetSocketsSet) {
                logger.info("TODO: Send MSG(" + senderSocket + "->" + targetSocket + ") that we registered that an Atom was created, atomUri is: " + atomCreatedEvent.getAtomURI());
                WonMessage wonMessage = WonMessageBuilder
                        .connectionMessage()
                        .sockets()
                        .sender(senderSocket)
                        .recipient(targetSocket)
                        .content()
                        .text(getAirData(atomCreatedEvent, defaultWrapper, event))
                        .build();
                ctx.getWonMessageSender().prepareAndSendMessage(wonMessage);
            }
        }
    }

    private String getAirData(MatcherExtensionAtomCreatedEvent atomCreatedEvent, DefaultAtomModelWrapper defaultWrapper, Event event){
        //todo: substitute random values w/ actual values read from defaultWrapper
        Model m = defaultWrapper.getAtomModel();
        Resource atom = defaultWrapper.getAtomModel().getResource(((MatcherExtensionAtomCreatedEvent) event).getAtomURI().toString());
        String def_string = "We registered that an Atom w/ tag 'AirData' was created, atomUri is: " + atomCreatedEvent.getAtomURI()+ "\n";
        String city = "City: Wien\n";

        String airQ1 = "Air quality index (Ozone): " + getCat(atom.getProperty(SCHEMA.DESCRIPTION).getInt(), PPM_OZONE) + "\n";
        String airQ2 = "Air quality index (Nitrogen dioxide): " + getCat(atom.getProperty(SCHEMA.DESCRIPTION).getInt(), PPM_NO) + "\n";
        String airQ3 = "Air quality index (Sulfur dioxide): " + getCat(atom.getProperty(SCHEMA.DESCRIPTION).getInt(), PPM_SDIO) + "\n";
        String airQ4 = "Air quality index (Carbon monoxide): " + getCat(atom.getProperty(SCHEMA.DESCRIPTION).getInt(), PPM_CO) + "\n";
        String airQ5 = "Air quality index (PM10): " + getCat(atom.getProperty(SCHEMA.DESCRIPTION).getInt(), µG_PM10) + "\n";
        String airQ6 = "Air quality index (PM2.5): " + getCat(atom.getProperty(SCHEMA.DESCRIPTION).getInt(), µG_PM2_5) + "\n";
        String airQ7 = "Air quality index (TSP): " + getCat(atom.getProperty(SCHEMA.DESCRIPTION).getInt(), µG_TSP) + "\n";

        //String description1 = "description: " + atom.getProperty(SCHEMA.DESCRIPTION).getResource() + "\n";
        //String description2 = "description: " + atom.getProperty(SCHEMA.DESCRIPTION).getLanguage() + "\n";

        return def_string.concat(city).concat(airQ1).concat(airQ2).concat(airQ3).concat(airQ4).concat(airQ5).concat(airQ6).concat(airQ7);
    }

    private String getCat(float value, float opt){
        float val = value / opt * 100;
        if(val < 33){
            return "Very good";
        }else if(val < 66){
            return "Good";
        }else if(val < 99){
            return "Fair";
        }else if(val < 149){
            return "Poor";
        }else{
            return "Very poor";
        }
    }
}
