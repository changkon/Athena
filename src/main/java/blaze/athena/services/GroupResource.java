package blaze.athena.services;

import blaze.athena.DatabaseQueries.*;
import blaze.athena.dto.GroupDTO;
import blaze.athena.dto.QuestionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author Chang Kon Han
 * @author John Law
 * @author Wesley Yep
 * @since 23 Feb 2016
 */
public class GroupResource implements IGroupResource {


    @Override
    public String createGroup(@RequestBody GroupDTO input) {
        InsertGroupQuery query = new InsertGroupQuery();
        query.insert(input);
        return "group created";
    }

    @Override
    public ResponseEntity getGroups(int ownerId) {
        SelectGroupsByOwnerQuery query = new SelectGroupsByOwnerQuery();
        return new ResponseEntity<>(query.select(ownerId), HttpStatus.OK);
    }

}