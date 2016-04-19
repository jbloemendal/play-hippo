package controllers;

import model.HippoGoGreenNewsDocument;
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
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.PlayHippo;

import java.util.*;


public class HippoController extends Controller {

    public Result published() {
        try {
            HstQuery hstQuery = PlayHippo.createQuery("/content/documents");

            Filter filter = hstQuery.createFilter();
            filter.addEqualTo("jcr:primaryType", "gogreen:newsdocument");
            filter.addEqualTo("hippostd:state", "published");

            hstQuery.setFilter(filter);

            List<Object> jsonResponse = new LinkedList<Object>();

            final HstQueryResult result = hstQuery.execute();
            for (HippoBeanIterator it = result.getHippoBeans(); it.hasNext(); ) {
                HippoBean bean = it.nextHippoBean();
                if (bean != null && bean instanceof HippoGoGreenNewsDocument) {
                    jsonResponse.add(new HashMap() {
                        {
                            put("uuid", bean.getCanonicalUUID());
                            put("title", ((HippoGoGreenNewsDocument) bean).getTitle());
                            put("path", bean.getPath());
                            put("content", ((HippoGoGreenNewsDocument) bean).getBodyContent());
                        }
                    });
                }
            }

            return ok(Json.toJson(jsonResponse));
        } catch (ObjectBeanManagerException e) {
            Logger.error("Exception occurred, folder /content/documents unavailable.", e);
            return internalServerError(e.getMessage());
        } catch (QueryException e) {
            Logger.error("Exception occurred, HstQuery unavailable.", e);
            return internalServerError(e.getMessage());
        }
    }


    public Result uuid(String uuid) {
        try {
            HstQuery hstQuery = PlayHippo.createQuery("/content/documents");

            Filter filter = hstQuery.createFilter();
            filter.addEqualTo("jcr:uuid", uuid);
            hstQuery.setFilter(filter);

            final HstQueryResult result = hstQuery.execute();

            Map<String, Object> response = null;

            HippoBeanIterator it = result.getHippoBeans();
            final HippoBean bean;

            if (it.hasNext() && (bean = it.nextHippoBean()) != null) {
                response = new HashMap() {
                    {
                        put("uuid", bean.getCanonicalUUID());
                        put("title", ((HippoGoGreenNewsDocument) bean).getTitle());
                        put("path", bean.getPath());
                        put("content", ((HippoGoGreenNewsDocument) bean).getBodyContent());
                    }
                };
            }

            if (response == null) {
                return notFound();
            }
            return ok(Json.toJson(response));
        } catch (ObjectBeanManagerException e) {
            Logger.error("Exception occurred, folder /content/documents unavailable.", e);
            return internalServerError(e.getMessage());
        } catch (QueryException e) {
            Logger.error("Exception occurred, HstQuery unavailable.", e);
            return internalServerError(e.getMessage());
        }
    }

}
