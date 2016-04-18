package blaze.athena.services;

import blaze.athena.DatabaseQueries.SelectAccountNameQuery;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.ws.rs.PathParam;

/**
 * @author Chang Kon Han
 * @author John Law
 * @author Wesley Yep
 * @since 23 Feb 2016
 */
public class AccountResource implements IAccountResource {

    @Override
    public ResponseEntity getAccountName(@PathParam("id") int accountId) {
        SelectAccountNameQuery query = new SelectAccountNameQuery();
        return new ResponseEntity<String[]>(query.select(accountId), HttpStatus.OK);
    }
}