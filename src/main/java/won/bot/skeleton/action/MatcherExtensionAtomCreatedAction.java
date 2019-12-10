package won.bot.skeleton.action;

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

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MatcherExtensionAtomCreatedAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

        Map<URI, Set<URI>> connectedSocketsMapSet = botContextWrapper.getConnectedSockets();

        URI uri =  atomCreatedEvent.getAtomURI();
        Map<String, Object> map = botContextWrapper.getBotContext().loadObjectMap(uri.toString());

        botContextWrapper.getBotContext().loadFromObjectMap("s:title", uri.toString());
       for(String s : map.keySet()){
            String val = (String) map.get(s);
            logger.info("Key: " + s + ", Val: " + val);
        }


        DefaultAtomModelWrapper wrapper = new DefaultAtomModelWrapper(((MatcherExtensionAtomCreatedEvent) event).getAtomData());
        for(String s:wrapper.getAllTags()){
            logger.info("Tag: " + s);
        }
        for(String s:wrapper.getAllTitles()){
            logger.info("Title: " + s);
        }
        for(String s:wrapper.getContentPropertyStringValues(RDFS.label, "Titel")){
            logger.info("Title: " + s);
        }




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
                        .text("We registered that an Atom was created, atomUri is: " + atomCreatedEvent.getAtomURI())
                        .build();
                ctx.getWonMessageSender().prepareAndSendMessage(wonMessage);
            }
        }
    }
}
