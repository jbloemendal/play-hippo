package controllers;

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryManagerImpl;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.*;

public class HippoController extends Controller {

    private static HippoRepository repo;
    private static Session session;

    static {
        try {
            repo = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
            session = repo.login("admin", new char[] {'a', 'd', 'm', 'i', 'n'});
        } catch (RepositoryException e) {
            Logger.error("Exception occurred, no repository session available.", e);
        }
    }

    protected ObjectConverter getObjectConverter() {
        return ObjectConverterUtils.createObjectConverter(getAnnotatedClasses(), true);
    }

    protected Collection<Class<? extends HippoBean>> getAnnotatedClasses() {
        List<Class<? extends HippoBean>> annotatedClasses = new ArrayList<Class<? extends HippoBean>>();
        annotatedClasses.add(PersistableTextPage.class);
        return annotatedClasses;
    }

    public Result published() {
        try {
            ObjectConverter objectConverter = getObjectConverter();

            ObjectBeanManager obm = new ObjectBeanManagerImpl(session, objectConverter);

            HippoFolder folder = (HippoFolder) obm.getObject("/content/documents");

            HstQueryManager queryManager = new HstQueryManagerImpl(session, objectConverter, null);

            HstQuery hstQuery = queryManager.createQuery(folder);
            Filter filter = hstQuery.createFilter();
            filter.addEqualTo("hippostd:state", "published");

            List<Object> jsonResponse = new LinkedList<Object>();
            final HstQueryResult result = hstQuery.execute();

            for (HippoBeanIterator it = result.getHippoBeans(); it.hasNext(); ) {
                HippoBean bean = it.nextHippoBean();
                if (bean != null) {
                    jsonResponse.add(new HashMap() {
                        {
                            put("uuid", bean.getCanonicalUUID());
                            put("title", bean.getProperty("gogreen:title"));
                            put("path", bean.getPath());
                        }
                    });
                }
            }

            return ok(Json.toJson(jsonResponse));
        } catch (ObjectBeanManagerException e) {
            Logger.error("Exception occurred, folder documents unavailable.", e);
            return internalServerError(e.getMessage());
        } catch (QueryException e) {
            Logger.error("Exception occurred, HstQuery unavailable.", e);
            return internalServerError(e.getMessage());
        }
    }

}
