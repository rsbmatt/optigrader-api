package dev.mahabal.optigrader.api.dao;

import dev.mahabal.optigrader.api.model.Submission;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindFields;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.List;

/**
 * @author Matthew
 */
@UseStringTemplateSqlLocator
public interface SubmissionDao {

    @SqlUpdate
    void createTable();

    @SqlUpdate
    void dropTable();

    @SqlUpdate
    void addSubmission(@BindFields final Submission test);


    @SqlQuery
    @RegisterBeanMapper(Submission.class)
    List<Submission> getSubmissionsForTestCode(@Bind("testCode") final String testCode);


    @SqlQuery
    @RegisterBeanMapper(Submission.class)
    List<Submission> getSubmissionsForNid(@Bind("nid") final String nid);

}
