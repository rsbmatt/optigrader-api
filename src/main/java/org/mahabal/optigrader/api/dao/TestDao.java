package org.mahabal.optigrader.api.dao;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindFields;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;
import org.mahabal.optigrader.api.model.Test;

import java.util.List;
import java.util.Optional;

/**
 * @author Matthew
 */
@UseStringTemplateSqlLocator
public interface TestDao {

    @SqlUpdate
    void createTable();

    @SqlUpdate
    void dropTable();

    @SqlUpdate
    void addTest(@BindFields final Test test);

    @SqlQuery
    @RegisterBeanMapper(Test.class)
    List<Test> getTestsOwnedByNid(@Bind("nid") final String nid);

    @SqlQuery
    @RegisterBeanMapper(Test.class)
    Optional<Test> getTestByCode(@Bind("code") final String code);

    @SqlUpdate
    void deleteTestByCode(@Bind("code") final String code);

}
