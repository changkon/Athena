package blaze.athena.QuestionGeneration;

/**
 * Created by Wesley on 4/04/2016.
 */
public class Answer {
    private String answerText;
    private double value;

    public Answer(String answerText, double value) {
        this.answerText = answerText;
        this.value = value;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object other) {
        Answer otherAnswer =(Answer) other;
        return (answerText.equals(otherAnswer.answerText));
    }
}
