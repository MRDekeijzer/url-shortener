package dev.minurl.db;

import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;

import dev.minurl.AppConfig;

public final class DataSourceFactory {
    private DataSourceFactory() {
    }

    public static DataSource create(AppConfig config) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(config.dbUrl());
        dataSource.setUser(config.dbUser());
        dataSource.setPassword(config.dbPassword());
        return dataSource;
    }
}
