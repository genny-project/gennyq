package life.genny.lauchy.streams;

import java.util.Properties;

import javax.inject.Inject;

import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Testing of the Topology without a broker, using TopologyTestDriver
 */
@QuarkusTest
public class TopologyProducerTest {

	// @Inject
	// Topology topology;

	// TopologyTestDriver testDriver;

	// TestInputTopic<Integer, String> data;

	// TestOutputTopic<Integer, String> webData;

	// @BeforeEach
	// public void setUp() {

	// 	Properties config = new Properties();
	// 	config.put(StreamsConfig.APPLICATION_ID_CONFIG, "testApplicationId");
	// 	config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
	// 	testDriver = new TopologyTestDriver(topology, config);

	// 	data = testDriver.createInputTopic("data", new IntegerSerializer(), new
	// 		StringSerializer());
	// 	webData = testDriver.createOutputTopic("webdata", new IntegerDeserializer(),
	// 		new StringDeserializer());
	// }

	// @AfterEach
	// public void tearDown(){
	// 	testDriver.close();
	// }

	// @Test
	// public void test() {

	// 	// TODO: Make this a proper test

	// 	String json = "";
	// 	data.pipeInput(json);

	// 	TestRecord<Integer, String> result = webData.readRecord();

	// 	// Assertions.assertEquals(null, result);

	// }
}
