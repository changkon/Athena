package blaze.athena.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wesley on 6/03/2016.
 */
public class QuestionDTO {
    private String question;
    private String topic;
    private List<String> answers = new ArrayList<>();
    private int answer;

    public QuestionDTO(String topic, String question) {
        this.topic = topic;
        this.setQuestion(question.replaceAll("(\\r|\\n|\\r\\n)+", "\\\\n"));
    }

    public String getQuestion() {
        return question;
    }

    public void addAnswer(String answer) {
        answers.add(answer);
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public void setAnswers(List<String> answers) {
        this.answers = answers;
    }

    public int getAnswer() {
        return answer;
    }

    public void setAnswer(int answer) {
        this.answer = answer;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String q) {
        this.question = q;
    }
}
