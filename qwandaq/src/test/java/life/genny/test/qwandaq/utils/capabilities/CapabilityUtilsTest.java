package life.genny.test.qwandaq.utils.capabilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.CapabilityBuilder;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityMode;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.datatype.capability.core.node.PermissionMode;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;

import life.genny.qwandaq.utils.testsuite.BaseTestCase;
import life.genny.qwandaq.utils.testsuite.JUnitTester;

import static life.genny.qwandaq.datatype.capability.core.node.CapabilityMode.*;
import static life.genny.qwandaq.datatype.capability.core.node.PermissionMode.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@RunWith(MockitoJUnitRunner.class)
public class CapabilityUtilsTest extends BaseTestCase {

    @InjectMocks
    CapabilitiesManager capManager;

    @Test
    public void cleanCapabilityCodeTest() {
        
         new JUnitTester<String, String>()
        .setTest((input) -> {
            return Expected(CapabilitiesManager.cleanCapabilityCode(input.input));
        }).createTest("Clean Cap 1")
        .setInput("OWN_APPLE")
        .setExpected("CAP_OWN_APPLE")
        .build()

        .createTest("Clean Cap 2")
        .setInput("oWn_ApplE")
        .setExpected("CAP_OWN_APPLE")
        .build()

        .assertAll();

    }

    @Test
    public void serializeCapabilityTest() {
        List<CapabilityNode> caps = new ArrayList<>();
        for(CapabilityMode mode : CapabilityMode.values()) {
            for(PermissionMode permMode : PermissionMode.values()) {
                caps.add(CapabilityNode.get(mode, permMode));
            }
        }

        List<String> expected = caps.stream()
            .map((CapabilityNode cap) -> cap.capMode.getIdentifier() + CapabilityNode.DELIMITER + cap.permMode.getIdentifier())
            .collect(Collectors.toList());


        JUnitTester<CapabilityNode, String> unitTester = new JUnitTester<CapabilityNode, String>()
        .setTest((input) -> {
            return Expected(input.input.toString());
        });

        // Create Tests
        for(int i = 0; i < caps.size(); i++) {
            unitTester.createTest("Serialize test: " + expected.get(i))
            .setInput(caps.get(i))
            .setExpected(expected.get(i))
            .build();
        }

        unitTester.assertAll();
    }

    @Test
    public void deserializeCapabilityTest() {

        List<String> capString = new ArrayList<>();

        // Gen data
        for(CapabilityMode mode : CapabilityMode.values()) {
            for(PermissionMode permMode : PermissionMode.values()) {
                capString.add(mode.getIdentifier() + CapabilityNode.DELIMITER + permMode.getIdentifier());
            }
        }

        List<CapabilityNode> expected = capString.stream().map((String caps) -> {
            CapabilityMode mode = CapabilityMode.getByIdentifier(caps.charAt(0));
            PermissionMode permMode = PermissionMode.getByIdentifier(caps.charAt(2));
            return CapabilityNode.get(mode, permMode);
        }).collect(Collectors.toList());

        // Create tester
        JUnitTester<String, CapabilityNode> unitTester = new JUnitTester<String, CapabilityNode>()
        .setTest((input) -> {
            return Expected(CapabilityNode.parseCapability(input.input));
        });

        for(int i = 0; i < expected.size(); i++) {
            unitTester.createTest("Serialize test: " + expected.get(i))
            .setInput(capString.get(i))
            .setExpected(expected.get(i))
            .build();
        }

        unitTester.assertAll();
    }

    private String mergeTestName(Capability c1, Capability c2, boolean mostPermissive) {
        return (mostPermissive ? "Most" : "Least")
             + " Permissive Test "
             + c1.nodeString() + " & " + c2.nodeString();
    }

    @Test
    public void mergeTest() {
        JUnitTester<Capability, Capability> tester = new JUnitTester<Capability, Capability>();

        Capability capability1 = new CapabilityBuilder("CAP_ADMIN").add(SELF).buildCap();
        Capability capability2 = new CapabilityBuilder("CAP_ADMIN").edit(SELF).buildCap();
        Capability capability3 = new CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap();

        tester.setTest((input) -> {
            return Expected(capability1.merge(input.input, true)); // most permissive test
        })

        .createTest(mergeTestName(capability1, capability2, true))
        .setInput(capability2)
        .setExpected(new CapabilityBuilder("CAP_ADMIN").add(SELF).edit(SELF).buildCap())
        .build()

        .createTest(mergeTestName(capability1, capability3, true))
        .setInput(capability3)
        .setExpected(new CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap())
        .build()

        .assertAll()

        .setTest((input) -> {
            return Expected(capability1.merge(input.input, false)); // least permissive test
        })

        .createTest(mergeTestName(capability1, capability2, false))
        .setInput(capability2)
        .setExpected(new CapabilityBuilder("CAP_ADMIN").add(SELF).edit(SELF).buildCap())
        .build()

        .createTest(mergeTestName(capability1, capability3, false))
        .setInput(capability3)
        .setExpected(new CapabilityBuilder("CAP_ADMIN").add(SELF).buildCap())
        .build()

        .assertAll();
    }

    @Test
    public void getLesserNodesTest() {
        new JUnitTester<CapabilityNode, CapabilityNode[]>()
        .setTest((input) -> {
            return Expected(input.input.getLesserNodes());
        })
        .setVerification((result, expected) -> {
            assertArrayEquals(expected, result);
        })
        .createTest("Lesser Nodes Test 1")
        .setInput(CapabilityNode.get(ADD, ALL))
        .setExpected(new CapabilityNode[] {
            CapabilityNode.get(ADD, SELF),
            CapabilityNode.get(ADD, NONE)
        }).build()
        .assertAll();

    }

    @Test
    public void testCheckCapability() {
        // testing checkCapability
        Set<CapabilityNode> capabilitySet = new HashSet<>(Arrays.asList(
            new CapabilityNode[] {
                CapabilityNode.get(ADD, ALL),
                CapabilityNode.get(EDIT, SELF)
            }
        ));

        new JUnitTester<CapabilityNode[], Boolean>()
        .setTest((input) -> {
            return Expected(CapabilitiesManager.checkCapability(capabilitySet, false, input.input));
        })
        .createTest("Has Any One Capability 1")
        .setInput(new CapabilityNode[] {
            CapabilityNode.get(ADD, ALL)
        }).setExpected(true)
        .build()

        .createTest("Has Any One Capability 2")
        .setInput(new CapabilityNode[] {
            CapabilityNode.get(ADD, SELF), CapabilityNode.get(DELETE, ALL)
        }).setExpected(true)
        .build()

        .assertAll()

        // Next test
        .setTest((input) -> {
            return Expected(CapabilitiesManager.checkCapability(capabilitySet, true, input.input));
        })

        .createTest("Has All Capabilities 1")
        .setInput(new CapabilityNode[] {
            CapabilityNode.get(ADD, SELF)
        }).setExpected(true)
        .build()

        .createTest("Has All Capabilities 2")
        .setInput(new CapabilityNode[] {
            CapabilityNode.get(ADD, SELF), CapabilityNode.get(DELETE, ALL)
        }).setExpected(false)
        .build()

        .assertAll();
    }
}
