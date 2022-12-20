package life.genny.bootq.bootxport.bootx;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jboss.logging.Logger;

import com.google.api.services.sheets.v4.model.Sheet;
import com.google.common.collect.Table.Cell;

public class XSSFService {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().getClass());

    private static List<Object> apply(List<Object> acc, List<Object> first) {
        return first.stream()
                .filter(a -> !a.toString().isEmpty()).flatMap(s -> {
                    acc.set(first.indexOf(s), s);
                    return acc.stream();
                }).collect(Collectors.toList());
    }

    public List<List<Object>> offlineService(String sheetId,
                                             String sheetName) {
        Workbook workbook = null;
        List<List<Object>> values = null;
        try (FileInputStream excelFile = new FileInputStream(Paths.get(System.getProperty("user.home"), sheetId).toFile())) {
            workbook = new XSSFWorkbook(excelFile);
        } catch (IOException ex) {
            log.error(ex.getMessage());
            return Collections.emptyList();
        }

        Sheet datatypeSheet = workbook.getSheet(sheetName);

        Stream<Row> targetStream =
                fromIteratorToStream(datatypeSheet.iterator());


        int count = (int) fromIteratorToStream(datatypeSheet.iterator()).limit(1).map(r -> r.getLastCellNum()).findFirst().get();

        values = targetStream.filter(a -> a.cellIterator().hasNext()).map(currentRow -> {

            Stream<Cell> targetStream2 =
                    fromIteratorToStream(currentRow.iterator());


            return targetStream2.map(currentCell -> {
                List<Object> arrayList1 = new ArrayList<>(
                        Collections.nCopies(count, ""));

                CellType cellType = currentCell.getCellType();
                int columnIndex = currentCell.getColumnIndex();
                String value = " ";

                switch (cellType) {
                    case NUMERIC:
                        value = Double.toString(currentCell.getNumericCellValue());
                        break;
                    case BOOLEAN:
                    case FORMULA:
                        value = Boolean.toString(currentCell.getBooleanCellValue());
                        break;
                    case BLANK:
                    case _NONE:
                    case ERROR:
                        value = " ";
                        break;
                    default:
                        value = currentCell.getStringCellValue().equals("") ? " " : currentCell.getStringCellValue();
                }

                arrayList1.set(columnIndex, value);
                return arrayList1;
            }).reduce(XSSFService::apply).get();

        }).collect(Collectors.toList());
        return values;
    }

    public <T> Stream<T> fromIteratorToStream(Iterator<T> iterator) {
        Iterable<T> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
