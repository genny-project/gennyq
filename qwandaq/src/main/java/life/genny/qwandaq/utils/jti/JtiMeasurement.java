package life.genny.qwandaq.utils.jti;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;

@RegisterForReflection
public class JtiMeasurement {
    public String  userCode;
    public Instant timestamp;
    public double value;

    public JtiMeasurement(String userCode) {
        this.userCode = userCode;
    }

    public JtiMeasurement(String userCode, Instant timestamp, double value) {
        this.userCode = userCode;
        this.timestamp = timestamp;
        this.value = value;
    }

}
