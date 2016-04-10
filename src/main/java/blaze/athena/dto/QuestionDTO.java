package blaze.athena.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wesley on 6/03/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionDTO {
    private String question;
    private String topic;
    private String category;
    private List<String> answers = new ArrayList<>();
    private int answer;
    private double rating;

    public QuestionDTO(String topic, String question) {
        this.topic = topic;
        this.setQuestion(question.replaceAll("(\\r|\\n|\\r\\n)+", "\\\\n"));
    }

    public QuestionDTO() {

    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public void setTopic(String t) {
        this.topic = t;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
}
