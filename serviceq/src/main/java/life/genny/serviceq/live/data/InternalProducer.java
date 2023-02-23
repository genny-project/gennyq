package life.genny.serviceq.live.data;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

/**
 * InternalProducer --- Kafka smallrye producer objects to send to other
 * internal consumers within GENNY.
 */

@ApplicationScoped
public class InternalProducer {

  @Inject @Channel("eventsout") Emitter<String> events;
  public Emitter<String> getToEvents() {
    return events;
  }

  @Inject @Channel("search_eventsout") Emitter<String> searchEvents;
  public Emitter<String> getToSearchEvents() {
	  return searchEvents;
  }

  @Inject @Channel("valid_eventsout") Emitter<String> validEvents;
  public Emitter<String> getToValidEvents() {
	  return validEvents;
  }

  @Inject @Channel("genny_eventsout") Emitter<String> gennyEvents;
  public Emitter<String> getToGennyEvents() {
	  return gennyEvents;
  }

  @Inject @Channel("genny_dataout") Emitter<String> gennyData;
  public Emitter<String> getToGennyData() {
	  return gennyData;
  }

  @Inject @Channel("dataout") Emitter<String> data;
  public Emitter<String> getToData() {
    return data;
  }

  @Inject @Channel("valid_dataout") Emitter<String> valid_data;
  public Emitter<String> getToValidData() {
    return valid_data;
  }

  @Inject @Channel("search_dataout") Emitter<String> searchData;
  public Emitter<String> getToSearchData() {
	  return searchData;
  }

  @Inject @Channel("webcmdsout") Emitter<String> webCmds;
  public Emitter<String> getToWebCmds() {
	  return webCmds;
  }

  @Inject @Channel("webdataout") Emitter<String> webData;
  public Emitter<String> getToWebData() {
	  return webData;
  }

  @Inject @Channel("messagesout") Emitter<String> messages;
  public Emitter<String> getToMessages() {
    return messages;
  }

  @Inject @Channel("scheduleout") Emitter<String> schedule;
  public Emitter<String> getToSchedule() {
    return schedule;
  }

  @Inject @Channel("blacklistout") Emitter<String> blacklist;
  public Emitter<String> getToBlacklist() {
    return blacklist;
  }

  @Inject @Channel("deadletterqueue") Emitter<String> deadletterqueue;
  public Emitter<String> getToDeadLetterQueue() {
    return deadletterqueue;
  }

}
