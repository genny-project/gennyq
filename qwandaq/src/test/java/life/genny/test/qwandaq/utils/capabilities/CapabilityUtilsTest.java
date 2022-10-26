package life.genny.test.qwandaq.utils.capabilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import life.genny.qwandaq.datatype.capability.Capability;
import life.genny.qwandaq.datatype.capability.CapabilityMode;
import life.genny.qwandaq.datatype.capability.CapabilityNode;
import life.genny.qwandaq.datatype.capability.PermissionMode;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;

import life.genny.test.qwandaq.utils.BaseTestCase;
import life.genny.test.utils.callbacks.test.FITestCallback;
import life.genny.test.utils.suite.TestCase;

import static life.genny.qwandaq.datatype.capability.CapabilityMode.*;
import static life.genny.qwandaq.datatype.capability.PermissionMode.*;

import static life.genny.test.utils.suite.TestCase.Builder;
import static life.genny.test.utils.suite.TestCase.Input;
import static life.genny.test.utils.suite.TestCase.Expected;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class CapabilityUtilsTest extends BaseTestCase {

	static final Logger log = Logger.getLogger(CapabilitiesManager.class);

    @InjectMocks
    CapabilitiesManager capManager;

    @Test
    public void cleanCapabilityCodeTest() {

        Builder<String, String> builder = new Builder<>();

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
        Builder<CapabilityNode, String> builder = new Builder<>();

        FITestCallback<Input<CapabilityNode>, Expected<String>> testFunc = (input) -> {
            return new Expected<>(input.input.toString());
        };
        
        List<TestCase<CapabilityNode, String>> tests = new ArrayList<>();
        List<CapabilityNode> caps = new ArrayList<>();
        for(CapabilityMode mode : CapabilityMode.values()) {
            for(PermissionMode permMode : PermissionMode.values()) {
                caps.add(new CapabilityNode(mode, permMode));
            }
        }

        List<String> expected = caps.stream()
            .map((CapabilityNode cap) -> cap.capMode.getIdentifier() + CapabilityNode.DELIMITER + cap.permMode.getIdentifier())
            .collect(Collectors.toList());

        for(int i = 0; i < caps.size(); i++) {
            tests.add(
                builder.setName("Serialize test: " + expected.get(i))
                    .setInput(caps.get(i))
                    .setExpected(expected.get(i))
                    .setTest(testFunc)
                    .build()
            );
        }

        for(TestCase<CapabilityNode, String> test : tests) {
            assertEquals(test.getExpected(), test.test());
        }
    }

    @Test
    public void deserializeCapabilityTest() {
        Builder<String, CapabilityNode> builder = new Builder<>();

        FITestCallback<Input<String>, Expected<CapabilityNode>> testFunc = (input) -> {
            return new Expected<>(CapabilityNode.parseCapability(input.input));
        };
        
        List<TestCase<String, CapabilityNode>> tests = new ArrayList<>();
        List<String> capString = new ArrayList<>();

        for(CapabilityMode mode : CapabilityMode.values()) {
            for(PermissionMode permMode : PermissionMode.values()) {
                capString.add(mode.getIdentifier() + CapabilityNode.DELIMITER + permMode.getIdentifier());
            }
        }

        List<CapabilityNode> expected = capString.stream().map((String caps) -> {
            CapabilityMode mode = CapabilityMode.getByIdentifier(caps.charAt(0));
            PermissionMode permMode = PermissionMode.getByIdentifier(caps.charAt(2));

            return new CapabilityNode(mode, permMode);
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

        for(TestCase<String, CapabilityNode> test : tests) {
            assertEquals(test.getExpected(), test.test());
        }
    }

    @Test
    public void mostPermissiveTest() {
        Capability capability1 = new Capability("CAP_ADMIN", new CapabilityNode(ADD, NONE), new CapabilityNode(EDIT, SELF));
        Capability capability2 = new Capability("CAP_ADMIN", new CapabilityNode(ADD, SELF));
        Capability capability3 = new Capability("CAP_ADMIN", new CapabilityNode(ADD, ALL));
    }

    @Test
    public void getLesserNodesTest() {
        Builder<CapabilityNode, CapabilityNode[]> builder = new Builder<>();
        List<TestCase<CapabilityNode, CapabilityNode[]>> tests = new ArrayList<>();

        FITestCallback<Input<CapabilityNode>, Expected<CapabilityNode[]>> testFunc = (input) -> {
            return new Expected<>(input.input.getLesserNodes());
        };

        tests.add(
            builder.setName("Lesser Nodes Test 1")
                    .setInput(new CapabilityNode(ADD, ALL))
                    .setExpected(new CapabilityNode[] {
                        new CapabilityNode(ADD, SELF),
                        new CapabilityNode(ADD, NONE)
                    })
                    .setTest(testFunc)
                    .build()
        );

        for(TestCase<CapabilityNode, CapabilityNode[]> test : tests) {
            assertArrayEquals(test.getExpected(), test.test());
        }

    }

    @Test
    public void testCheckCapability() {
        // testing checkCapability
        Set<CapabilityNode> capabilitySet = new HashSet<>(Arrays.asList(
            new CapabilityNode[] {
                new CapabilityNode(ADD, ALL),
                new CapabilityNode(EDIT, SELF),
                new CapabilityNode(VIEW, NONE)
            }
        ));

        boolean result = CapabilitiesManager.checkCapability(capabilitySet, false, new CapabilityNode(ADD, ALL));
        assertEquals(true, result);
        
        result = CapabilitiesManager.checkCapability(capabilitySet, false, new CapabilityNode(ADD, SELF));
        assertEquals(true, result);

        result = CapabilitiesManager.checkCapability(capabilitySet, true, new CapabilityNode(ADD, SELF));
        assertEquals(true, result);

        result = CapabilitiesManager.checkCapability(capabilitySet, true, new CapabilityNode(ADD, SELF), new CapabilityNode(DELETE, ALL));
        assertEquals(false, result);

        result = CapabilitiesManager.checkCapability(capabilitySet, false, new CapabilityNode(ADD, SELF), new CapabilityNode(DELETE, ALL));
        assertEquals(true, result);
    }

    @Test
    public void getCapModeArrayTest() {
        Builder<String, CapabilityNode[]> builder = new Builder<String, CapabilityNode[]>();
    }

    @Test
    public void getCapModeStringTest() {
        Builder<CapabilityNode[], String> builder = new Builder<CapabilityNode[], String>();
    }
}
