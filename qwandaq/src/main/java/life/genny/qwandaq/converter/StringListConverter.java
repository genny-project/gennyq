package life.genny.qwandaq.converter;


import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

  
  /** 
   * Convert a List of Strings to a stringified list for storing in the database
   *
   * @param list the List of Strings to convert
   * @return String
   */
  @Override
  public String convertToDatabaseColumn(final List<String> list) {
    String ret = "";
    if (list!=null) {
    for (final String str : list) {
      ret += str + ",";
    }
    }
    return ret;

  }

  
  /** 
   * Convert a stringified list to a List of Strings for an EntityAttribute
   *
   * @param joined the string to convert
   * @return List&lt;String&gt;
   */
  @Override
  public List<String> convertToEntityAttribute(final String joined) {
      List<String> strings = new CopyOnWriteArrayList<>();
    if (joined != null) {
      strings = new CopyOnWriteArrayList<>(Arrays.asList(joined.split(",")));
    }
    return strings;
  }

}
