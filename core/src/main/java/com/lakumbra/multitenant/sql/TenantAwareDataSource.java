package com.lakumbra.multitenant.sql;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * Will return the connection based on which db instance the tenant is allocated to.
 * @author ravi.somepalli
 *
 */
public abstract class TenantAwareDataSource  implements DataSource{

	
	private Map<String, DataSource> dbInstances = new HashMap<>();
	private TenantContext tenantContext;
	protected TenantSessionCallback tenantSessionCallback;
	private TenantDBInstanceKeyProvider tenantDBInstanceProvider;
	

	
	public DataSource getDbInstance(String instanceId) {
		return dbInstances.get(instanceId);
	}

	public void setDbInstances(Map<String, DataSource> dbInstances) {
		this.dbInstances.putAll(dbInstances);
	}
	
	public void addDbInstance(String name , DataSource dataSource) {
		this.dbInstances.put(name, dataSource);
	}

	public void setTenantContext(TenantContext tenantContext) {
		this.tenantContext = tenantContext;
	}

	protected void setTenantSessionCallback(TenantSessionCallback tenantSessionCallback) {
		this.tenantSessionCallback = tenantSessionCallback;
	}

	public void setTenantDBInstanceProvider(TenantDBInstanceKeyProvider tenantDBInstanceProvider) {
		this.tenantDBInstanceProvider = tenantDBInstanceProvider;
	}

	/**
	 * Returns 0, indicating the default system timeout is to be used.
	 */
	@Override
	public int getLoginTimeout() throws SQLException {
		return 0;
	}

	/**
	 * Setting a login timeout is not supported.
	 */
	@Override
	public void setLoginTimeout(int timeout) throws SQLException {
		throw new UnsupportedOperationException("setLoginTimeout");
	}

	/**
	 * LogWriter methods are not supported.
	 */
	@Override
	public PrintWriter getLogWriter() {
		throw new UnsupportedOperationException("getLogWriter");
	}

	/**
	 * LogWriter methods are not supported.
	 */
	@Override
	public void setLogWriter(PrintWriter pw) throws SQLException {
		throw new UnsupportedOperationException("setLogWriter");
	}


	//---------------------------------------------------------------------
	// Implementation of JDBC 4.0's Wrapper interface
	//---------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isInstance(this)) {
			return (T) this;
		}
		throw new SQLException("DataSource of type [" + getClass().getName() +
				"] cannot be unwrapped as [" + iface.getName() + "]");
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isInstance(this);
	}


	//---------------------------------------------------------------------
	// Implementation of JDBC 4.1's getParentLogger method
	//---------------------------------------------------------------------

	@Override
	public Logger getParentLogger() {
		return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	}


	@Override
	public Connection getConnection() throws SQLException {
		int tenantId = this.tenantContext.getTenantId();
		String dbInstanceKey = this.tenantDBInstanceProvider.getDBInstanceKey(tenantId);
		DataSource ds = this.dbInstances.get(dbInstanceKey);
		System.out.println("returning connection for dbInstanceKey=" + dbInstanceKey + "  tenantId=" + tenantId);
		Connection connection = ds.getConnection();
		this.tenantSessionCallback.startTenantSession(tenantId, connection);
		return connection;
		
	}
	
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		int tenantId = this.tenantContext.getTenantId();
		String dbInstanceKey = this.tenantDBInstanceProvider.getDBInstanceKey(tenantId);
		DataSource ds = this.dbInstances.get(dbInstanceKey);
		Connection connection = ds.getConnection(username, password);
		this.tenantSessionCallback.startTenantSession(tenantId, connection);
		return connection;

	}

	
}
