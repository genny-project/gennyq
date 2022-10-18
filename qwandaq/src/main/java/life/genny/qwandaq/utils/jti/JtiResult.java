package life.genny.qwandaq.utils.jti;

import java.util.Optional;

public class JtiResult {
    private static JtiResult NOT_FOUND = new JtiResult(null);

    private final JtiEntity result;

    private JtiResult(JtiEntity result) {
        this.result = result;
    }

    public static JtiResult found(JtiEntity data) {
        return new JtiResult(data);
    }

    public static JtiResult notFound() {
        return NOT_FOUND;
    }

    public Optional<JtiEntity> getResult() {
        return Optional.ofNullable(result);
    }
}
