package com.mhxy.price;

import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.stereotype.Component;

@Component
class DataSourceCloser implements BeanPostProcessor, DisposableBean {
  private HikariDataSource pool;

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (!(bean instanceof HikariDataSource hikari)) {
      return bean;
    }
    hikari.setMinimumIdle(0);
    hikari.setIdleTimeout(10_000);
    hikari.setMaxLifetime(60_000);
    hikari.setKeepaliveTime(0);
    if (hikari.getMaximumPoolSize() > 2) {
      hikari.setMaximumPoolSize(2);
    }
    pool = hikari;
    return new EvictOnReturnDataSource(hikari);
  }

  @Override
  public void destroy() {
    if (pool != null && !pool.isClosed()) {
      pool.close();
    }
  }

  private static final class EvictOnReturnDataSource extends DelegatingDataSource implements AutoCloseable {
    private final HikariDataSource hikari;

    private EvictOnReturnDataSource(HikariDataSource hikari) {
      super(hikari);
      this.hikari = hikari;
    }

    @Override
    public Connection getConnection() throws SQLException {
      return wrap(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
      return wrap(super.getConnection(username, password));
    }

    @Override
    public void close() {
      hikari.close();
    }

    private Connection wrap(Connection connection) {
      return (Connection) Proxy.newProxyInstance(
          connection.getClass().getClassLoader(),
          new Class<?>[] {Connection.class},
          (proxy, method, args) -> {
            if ("close".equals(method.getName()) && (args == null || args.length == 0)) {
              try {
                return invoke(connection, method, args);
              } finally {
                var mx = hikari.getHikariPoolMXBean();
                if (mx != null) {
                  mx.softEvictConnections();
                }
              }
            }
            return invoke(connection, method, args);
          });
    }

    private static Object invoke(Connection connection, java.lang.reflect.Method method, Object[] args) throws Throwable {
      try {
        return method.invoke(connection, args);
      } catch (InvocationTargetException error) {
        throw error.getCause();
      }
    }
  }
}
