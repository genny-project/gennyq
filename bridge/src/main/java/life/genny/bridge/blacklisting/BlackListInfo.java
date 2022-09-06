package life.genny.bridge.blacklisting;

import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BlackListInfo --- Handle rules and conditions sent from different sources
 * such as kafka or http endpoints. The conditions are:
 *      - minus to delete all 
 *      - minus appended to a UUID to delete it correspondingly 
 *      - Only UUID to save it and blacklisted 
 *
 * @author    hello@gada.io
 */
@ApplicationScoped
public class BlackListInfo {

    private static final Logger log = Logger.getLogger(BlackListInfo.class);

    Set<UUID> blackListedUUIDs = ConcurrentHashMap.newKeySet();

    public Set<UUID> getBlackListedUUIDs() {
        return blackListedUUIDs;
    }

    public void deleteAll() {
        this.blackListedUUIDs.clear();
    }

    public void deleteRecord(UUID uuid) {
        this.blackListedUUIDs.remove(uuid);
    }

    public void onReceived(UUID uuid){
        onReceived(uuid.toString());
    }

    /**
     * Handle the uuid sent from a trusted source to delete clear 
     * or saved into a concurrent set where all blacklisted uuids are
     * maintained
     *
     * @param uuidRuleProtocol
     */
    public void onReceived(String uuidRuleProtocol){

        String protocol = uuidRuleProtocol.trim();
        if (protocol.equals("-")) {
            deleteAll();
            return;
        } else if (protocol.length() < 2){
            return;
        }

        boolean hasDelete = protocol.startsWith("-") && !protocol.subSequence(1, 2).equals("-");

        try {
            if (hasDelete) {
                String withoutMinus = protocol.substring(1);
                deleteRecord(UUID.fromString(withoutMinus));
            } else {
                log.info("Added blacklist "+protocol);
                this.blackListedUUIDs.add(UUID.fromString(protocol));
            }
        } catch (Exception exception) {
            log.error("The string received might not be a uuid {"+uuidRuleProtocol+"} please check again");
        }

    }
}
