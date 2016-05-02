package blaze.athena.services;

import blaze.athena.DatabaseQueries.InsertQuestionQuery;
import blaze.athena.DatabaseQueries.RatingQuery;
import blaze.athena.DatabaseQueries.SearchQuestionsQuery;
import blaze.athena.DatabaseQueries.SelectAllCategoriesQuery;
import blaze.athena.dto.CategoryTagsDTO;
import blaze.athena.dto.QuestionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author Chang Kon Han
 * @author John Law
 * @author Wesley Yep
 * @since 23 Feb 2016
 */
public class QuestionResource implements IQuestionResource {

    @Override
    public String storeQuestion(@RequestBody QuestionDTO input) {
        System.out.println("storing and rating question " + input.getRating());
        InsertQuestionQuery insertQuestionQuery = new InsertQuestionQuery();
        insertQuestionQuery.insert(input);
        RatingQuery ratingQuery = new RatingQuery();
        ratingQuery.insertRating(input);
        return "yay";
    }

    @Override
    public String rateQuestion(@RequestBody QuestionDTO input) {
        System.out.println("rating question " + input.getRating());
        RatingQuery ratingQuery = new RatingQuery();
        ratingQuery.insertRating(input);
        return "yay";
    }

    @Override
    public ResponseEntity getCategories() {
        SelectAllCategoriesQuery query = new SelectAllCategoriesQuery();
        return new ResponseEntity<>(query.select(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity getQuestions(CategoryTagsDTO tags) {
        SearchQuestionsQuery searchQuestionsQuery = new SearchQuestionsQuery();
        return new ResponseEntity<>(searchQuestionsQuery.search(tags.getCategoryTags()), HttpStatus.OK);
    }


}