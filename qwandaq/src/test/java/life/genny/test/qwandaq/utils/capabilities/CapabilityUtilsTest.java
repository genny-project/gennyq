package life.genny.test.qwandaq.utils.capabilities;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import life.genny.qwandaq.datatype.Capability;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;

import life.genny.test.qwandaq.utils.BaseTestCase;
import life.genny.test.utils.callbacks.test.FITestCallback;
import life.genny.test.utils.suite.TestCase;

import static life.genny.test.utils.suite.TestCase.Builder;
import static life.genny.test.utils.suite.TestCase.Input;
import static life.genny.test.utils.suite.TestCase.Expected;

import static life.genny.qwandaq.datatype.Capability.CapabilityMode;
import static life.genny.qwandaq.datatype.Capability.PermissionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class CapabilityUtilsTest extends BaseTestCase {

	static final Logger log = Logger.getLogger(CapabilitiesManager.class);

    @InjectMocks
    CapabilitiesManager capManager;

    @Test
    public void cleanCapabilityCodeTest() {

        Builder<String, String> builder = new Builder<String, String>();

        FITestCallback<Input<String>, Expected<String>> testFunction = (input) -> {
            return new Expected<>(CapabilitiesManager.cleanCapabilityCode(input.input));
        };
        
        List<TestCase<String, String>> tests = new ArrayList<>();
        tests.add(
            builder.setName("Clean Cap 1")
                    .setInput("OWN_APPLE")
                    .setExpected("CAP_OWN_APPLE")
                    .setTest(testFunction)
                    .build()
        );
        
        tests.add(
            builder.setName("Clean Cap 2")
                    .setInput("oWn_ApplE")
                    .setExpected("CAP_OWN_APPLE")
                    .setTest(testFunction)
                    .build()
        );

        for(TestCase<String, String> test : tests) {
            assertEquals(test.getExpected(), test.test());
        }

    }

    @Test
    public void serializeCapabilityTest() {
        Builder<Capability, String> builder = new Builder<>();

        FITestCallback<Input<Capability>, Expected<String>> testFunc = (input) -> {
            return new Expected<>(input.input.toString());
        };
        
        List<TestCase<Capability, String>> tests = new ArrayList<>();
        List<Capability> caps = new ArrayList<>();
        for(CapabilityMode mode : CapabilityMode.values()) {
            for(PermissionMode permMode : PermissionMode.values()) {
                caps.add(new Capability(mode, permMode));
            }
        }

        List<String> expected = caps.stream().map((Capability cap) -> cap.capMode.name() + Capability.DELIMITER + cap.permMode.name()).collect(Collectors.toList());

        for(int i = 0; i < caps.size(); i++) {
            tests.add(
                builder.setName("Serialize test: " + expected.get(i))
                    .setInput(caps.get(i))
                    .setExpected(expected.get(i))
                    .setTest(testFunc)
                    .build()
            );
        }

        for(TestCase<Capability, String> test : tests) {
            assertEquals(test.getExpected(), test.test());
        }
    }

    @Test
    public void deserializeCapabilityTest() {
        Builder<String, Capability> builder = new Builder<>();

        FITestCallback<Input<String>, Expected<Capability>> testFunc = (input) -> {
            return new Expected<>(Capability.parseCapability(input.input));
        };
        
        List<TestCase<String, Capability>> tests = new ArrayList<>();
        List<String> capString = new ArrayList<>();

        for(CapabilityMode mode : CapabilityMode.values()) {
            for(PermissionMode permMode : PermissionMode.values()) {
                capString.add(mode.name() + Capability.DELIMITER + permMode.name());
            }
        }

        List<Capability> expected = capString.stream().map((String caps) -> {
            String[] components = caps.split(":");
            CapabilityMode mode = CapabilityMode.valueOf(components[0]);
            PermissionMode permMode = PermissionMode.valueOf(components[1]);

            return new Capability(mode, permMode);
        }).collect(Collectors.toList());

        for(int i = 0; i < expected.size(); i++) {
            tests.add(
                builder.setName("Serialize test: " + expected.get(i))
                    .setInput(capString.get(i))
                    .setExpected(expected.get(i))
                    .setTest(testFunc)
                    .build()
            );
        }

        for(TestCase<String, Capability> test : tests) {
            assertEquals(test.getExpected(), test.test());
        }
    }

    @Test
    public void getCapModeArrayTest() {
        Builder<String, Capability[]> builder = new Builder<String, Capability[]>();
    }

    @Test
    public void getCapModeStringTest() {
        Builder<Capability[], String> builder = new Builder<Capability[], String>();
    }
}
