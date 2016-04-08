package blaze.athena.services;

import blaze.athena.DatabaseQueries.InsertQuestionQuery;
import blaze.athena.DatabaseQueries.RatingQuery;
import blaze.athena.dto.QuestionDTO;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author Chang Kon Han
 * @author John Law
 * @author Wesley Yep
 * @since 23 Feb 2016
 */
public class QuestionResource implements IQuestionResource {

    @Override
    public String storeQuestion(@RequestBody QuestionDTO input) {
        System.out.println("rating question " + input.getRating());
        InsertQuestionQuery insertQuestionQuery = new InsertQuestionQuery();
        insertQuestionQuery.insert(input);
        RatingQuery ratingQuery = new RatingQuery();
        ratingQuery.insertRating(input);
        return "yay";
    }

}