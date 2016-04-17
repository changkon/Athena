package blaze.athena.services;

import blaze.athena.DatabaseQueries.*;
import blaze.athena.dto.GroupDTO;
import blaze.athena.dto.QuestionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Set;

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
        SelectGroups query = new SelectGroups();
        Set<List<String>> groups = query.selectByOwner(ownerId);
        groups.addAll(query.selectByMember(ownerId));
        return new ResponseEntity<>(groups, HttpStatus.OK);
    }

}