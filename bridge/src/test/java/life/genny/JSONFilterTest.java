package life.genny;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.javax.JsonUtils;

public class JSONFilterTest {
    
    private String readFileAsString(String fp) {
        File file = new File(new File("").getAbsolutePath() + fp);
        String json = "";
        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null) {
                json += line + '\n';
            }
        } catch(FileNotFoundException e) {
            System.err.println("unlucky");
        } catch(IOException e) {
            System.err.println("extra unlucky");
        }

        return json;
    }

    @Test
    public void wearehere() {
        
        Jsonb jsonb = JsonbBuilder.create();
        
        String json = readFileAsString("/src/test/java/life/genny/test2.json");
        JsonObject obj = jsonb.fromJson(json, JsonObject.class);
        Instant last = Instant.now();
        obj = JsonUtils.filter(obj, "capabilityRequirements");
        Instant now = Instant.now();

        System.out.println(obj.toString());
        System.out.println("Time taken: " + Duration.between(last, now).toMillis());
    }
}


   