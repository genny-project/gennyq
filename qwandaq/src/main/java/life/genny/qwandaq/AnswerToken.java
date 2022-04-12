package life.genny.qwandaq;

public class AnswerToken {
    private Answer answer;
    private String token;

    public AnswerToken() {
    }

    public AnswerToken(Answer answer, String token) {
        this.answer = answer;
        this.token = token;
    }

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "AnswerToken [answer=" + answer + "]";
    }

}
