package life.genny.test.qwandaq.utils.capabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.capabilities.CapabilitiesController;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.CapabilityBuilder;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityMode;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.datatype.capability.core.node.PermissionMode;
import life.genny.test.qwandaq.utils.BaseTestCase;
import life.genny.qwandaq.utils.testsuite.JUnitTester;

import static life.genny.qwandaq.datatype.capability.core.node.CapabilityMode.*;
import static life.genny.qwandaq.datatype.capability.core.node.PermissionMode.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

// @RunWith(MockitoJUnitRunner.class)
public class CapabilityUtilsTest extends BaseTestCase {

    @Test
    public void cleanCapabilityCodeTest() {
        
         new JUnitTester<String, String>()
        .setTest((input) -> {
            return Expected(CapabilitiesController.cleanCapabilityCode(input.input));
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
    public void serializeNodeTest() {
        List<CapabilityNode> caps = new ArrayList<>();
        for(CapabilityMode mode : CapabilityMode.values()) {
            for(PermissionMode permMode : PermissionMode.values()) {
                caps.add(new CapabilityNode(mode, permMode));
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
        for(int i = 0; i < 2; i++) {
            for(CapabilityMode mode : CapabilityMode.values()) {
                for(PermissionMode permMode : PermissionMode.values()) {
                    capString.add((i != 0 ? "!" : "") + mode.getIdentifier() + CapabilityNode.DELIMITER + permMode.getIdentifier());
                }
            }
        }

        List<CapabilityNode> expected = capString.stream().map((String caps) -> {
            int startInd = 0;
            boolean negate = false;
            if(caps.startsWith("!")) {
                negate = true;
                startInd = 1;
            }
            CapabilityMode mode = CapabilityMode.getByIdentifier(caps.charAt(startInd));
            PermissionMode permMode = PermissionMode.getByIdentifier(caps.charAt(startInd + 2));
            CapabilityNode node = new CapabilityNode(mode, permMode);
            node.negate = negate;
            return node;
        }).collect(Collectors.toList());

        // Create tester
        JUnitTester<String, CapabilityNode> unitTester = new JUnitTester<String, CapabilityNode>()
        .setTest((input) -> {
            return Expected(CapabilityNode.parseNode(input.input));
        });

        for(int i = 0; i < expected.size(); i++) {
            unitTester.createTest("Serialize test: " + expected.get(i) + ", negate: " + expected.get(i).negate)
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
        .setInput(new CapabilityNode(ADD, ALL))
        .setExpected(new CapabilityNode[] {
            new CapabilityNode(ADD, SELF),
            new CapabilityNode(ADD, NONE)
        }).build()
        .assertAll();

    }
}
