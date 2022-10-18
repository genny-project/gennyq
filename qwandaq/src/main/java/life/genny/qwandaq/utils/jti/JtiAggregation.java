package life.genny.qwandaq.utils.jti;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RegisterForReflection
public class JtiAggregation {
    public String userCode;
    public String bridges;
    public String browserAgent;
    public double min = Double.MAX_VALUE;
    public double max = Double.MIN_VALUE;
    public int count;
    public double sum;
    public double avg;


    public JtiAggregation updateFrom(JtiMeasurement measurement) {
        userCode = measurement.userCode;

        count++;
        sum += measurement.value;
        avg = BigDecimal.valueOf(sum / count) .setScale(1, RoundingMode.HALF_UP).doubleValue();

        min = Math.min(min, measurement.value);
        max = Math.max(max, measurement.value);

        return this;
    }

}
