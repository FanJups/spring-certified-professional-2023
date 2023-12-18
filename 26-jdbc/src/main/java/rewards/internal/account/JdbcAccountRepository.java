package rewards.internal.account;

import common.money.MonetaryAmount;
import common.money.Percentage;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Loads accounts from a data source using the JDBC API.
 */

// TODO-10 (Optional) : Inject JdbcTemplate directly to this repository class
// - Refactor the constructor to get the JdbcTemplate injected directly
//   (instead of DataSource getting injected)
// - Refactor RewardsConfig accordingly
// - Refactor JdbcAccountRepositoryTests accordingly
// - Run JdbcAccountRepositoryTests and verity it passes

// TODO-05: Refactor this repository to use JdbcTemplate.
// - Add a field of type JdbcTemplate.
// - Refactor the code in the constructor to instantiate the JdbcTemplate
//   object using the given DataSource object.
public class JdbcAccountRepository implements AccountRepository {

	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;

	public JdbcAccountRepository(JdbcTemplate jdbcTemplate) {

		this.dataSource = dataSource;
		this.jdbcTemplate = jdbcTemplate;
	}

	// TODO-07 (Optional): Refactor this method using JdbcTemplate and ResultSetExtractor
	// - Create a ResultSetExtractor object and pass it as an argument
	//   to jdbcTemplate.query(..) method
	// - Let the extractData() method of the ResultSetExtractor to call
	//   mapAccount() method, which is provided in this class, to do all the work.
    // - Run the JdbcAccountRepositoryTests class. It should pass.
	public Account findByCreditCard(String creditCardNumber) {
		String sql = "select a.ID as ID, a.NUMBER as ACCOUNT_NUMBER, a.NAME as ACCOUNT_NAME, c.NUMBER as CREDIT_CARD_NUMBER, " +
			"	b.NAME as BENEFICIARY_NAME, b.ALLOCATION_PERCENTAGE as BENEFICIARY_ALLOCATION_PERCENTAGE, b.SAVINGS as BENEFICIARY_SAVINGS " +
			"from T_ACCOUNT a, T_ACCOUNT_CREDIT_CARD c " +
			"left outer join T_ACCOUNT_BENEFICIARY b " +
			"on a.ID = b.ACCOUNT_ID " +
			"where c.ACCOUNT_ID = a.ID and c.NUMBER = ?";

		return jdbcTemplate.query(sql,this::mapAccount,creditCardNumber);
	}

	// TODO-06: Refactor this method to use JdbcTemplate.
	// - Note that an account has multiple beneficiaries
	//   and you are going to perform UPDATE operation using
	//   JdbcTemplate for each of those beneficiaries
	// - Rerun the JdbcAccountRepositoryTests and verify it passes
	public void updateBeneficiaries(Account account) {
		String sql = "update T_ACCOUNT_BENEFICIARY SET SAVINGS = ? where ACCOUNT_ID = ? and NAME = ?";
		for (Beneficiary beneficiary : account.getBeneficiaries()) {
			jdbcTemplate.update(sql,beneficiary.getSavings().asBigDecimal(),account.getEntityId(),beneficiary.getName());
		}

	}

	/**
	 * Map the rows returned from the join of T_ACCOUNT and T_ACCOUNT_BENEFICIARY to an fully-reconstituted Account
	 * aggregate.
	 * 
	 * @param rs the set of rows returned from the query
	 * @return the mapped Account aggregate
	 * @throws SQLException an exception occurred extracting data from the result set
	 */
	private Account mapAccount(ResultSet rs) throws SQLException {
		Account account = null;
		while (rs.next()) {
			if (account == null) {
				String number = rs.getString("ACCOUNT_NUMBER");
				String name = rs.getString("ACCOUNT_NAME");
				account = new Account(number, name);
				// set internal entity identifier (primary key)
				account.setEntityId(rs.getLong("ID"));
			}
			account.restoreBeneficiary(mapBeneficiary(rs));
		}
		if (account == null) {
			// no rows returned - throw an empty result exception
			throw new EmptyResultDataAccessException(1);
		}
		return account;
	}

	/**
	 * Maps the beneficiary columns in a single row to an AllocatedBeneficiary object.
	 * 
	 * @param rs the result set with its cursor positioned at the current row
	 * @return an allocated beneficiary
	 * @throws SQLException an exception occurred extracting data from the result set
	 */
	private Beneficiary mapBeneficiary(ResultSet rs) throws SQLException {
		String name = rs.getString("BENEFICIARY_NAME");
		MonetaryAmount savings = MonetaryAmount.valueOf(rs.getString("BENEFICIARY_SAVINGS"));
		Percentage allocationPercentage = Percentage.valueOf(rs.getString("BENEFICIARY_ALLOCATION_PERCENTAGE"));
		return new Beneficiary(name, allocationPercentage, savings);
	}
}
