package life.genny.test.qwandaq.constants;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.utils.testsuite.JUnitTester;
import life.genny.test.qwandaq.utils.BaseTestCase;

public class StringConstantsTest extends BaseTestCase {

    @Test
    public void testAttributeCodes() {

        new JUnitTester<Class<?>, Boolean>()
            .setTest((input) -> {
                return Expected(checkClassStringConstants(input.input));
            })
            
            .createTest("Attribute Code Constants")
            .setInput(Attribute.class)
            .setExpected(true)
            .build()

            .assertAll();
	}

	public static boolean checkClassStringConstants(Class<?> clazz) {

		Field[] fields = clazz.getDeclaredFields();

		for (Field field : fields) {
			if (isStatic(field) && isString(field)) {
				if (!fieldNameEqualsValue(clazz, field)) {
					System.out.println(field.getName() + " does not have matching value");
					return false;
				}
			}
		}

		return true;
	}

	public static boolean fieldNameEqualsValue(Class<?> clazz, Field field) {
		try {
			return field.getName().equals(field.get(clazz));
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		return false;
	}

	public static boolean isStatic(Field field) {
		return java.lang.reflect.Modifier.isStatic(field.getModifiers());
	}

	public static boolean isString(Field field) {
		return String.class.equals(field.getType());
	}

}
