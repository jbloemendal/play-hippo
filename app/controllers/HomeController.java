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
import org.hippoecm.hst.content.beans.query.filter.*;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.util.DateTools;
import play.libs.Json;
import play.mvc.*;

import views.html.*;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
    public Result index() {
        return ok(index.render("Your new application is ready."));
    }

    protected ObjectConverter getObjectConverter() {
        return ObjectConverterUtils.createObjectConverter(getAnnotatedClasses(), true);
    }

    protected Collection<Class<? extends HippoBean>> getAnnotatedClasses() {
        List<Class<? extends HippoBean>> annotatedClasses = new ArrayList<Class<? extends HippoBean>>();
        annotatedClasses.add(PersistableTextPage.class);
        return annotatedClasses;
    }

    public Result hippo() {
        try {
            HippoRepository repo = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
            Session session = repo.login("admin", new char[] {'a', 'd', 'm', 'i', 'n'});

            ObjectConverter objectConverter = getObjectConverter();

            ObjectBeanManager obm = new ObjectBeanManagerImpl(session, objectConverter);

            HippoFolder folder = (HippoFolder) obm.getObject("/content/documents");

            HstQueryManager queryManager = new HstQueryManagerImpl(session, objectConverter, null);

            HstQuery hstQuery = queryManager.createQuery(folder);
            Filter filter = hstQuery.createFilter();
            filter.addEqualTo("hippotranslation:locale", "en");

            List<HippoBean> resultBeans = new LinkedList<HippoBean>();
            final HstQueryResult result = hstQuery.execute();

            // TODO return list which makes sense
            List<String> returnValue = new LinkedList<String>();

            for (HippoBeanIterator it = result.getHippoBeans(); it.hasNext(); ) {
                HippoBean bean = it.nextHippoBean();
                if (bean != null) {
                    resultBeans.add(bean);
                    returnValue.add(bean.getPath());
                }
            }

            return ok(Json.toJson(returnValue));
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        catch (ObjectBeanManagerException e) {
            e.printStackTrace();
        } catch (QueryException e) {
            e.printStackTrace();
        }
        return TODO;
    }

}
