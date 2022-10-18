package life.genny.qwandaq.utils.jti;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.ArrayList;
import java.util.List;

@RegisterForReflection
public class JtiEntity {
    public String userCode;
    public String bridges;
    public String browserAgent;
    public int numLogins;
    public String active;

    public JtiEntity(String userCode,String bridges,String browserAgent,int numLogins,String active){
        this.userCode = userCode;
        this.bridges = bridges;
        this.browserAgent = browserAgent;
        this.numLogins = numLogins;
        this.active = active;
    }

    public static JtiEntity from(JtiAggregation aggregation) {
        String active = "inactive";
        if(aggregation.count < 5) {
            active = "active";
        }

        return new JtiEntity(
                aggregation.userCode,
                aggregation.bridges,
                aggregation.browserAgent,
                aggregation.count,
                active);
    }
}

